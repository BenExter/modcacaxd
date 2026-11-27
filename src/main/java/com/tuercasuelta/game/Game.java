package com.tuercasuelta.game;

import com.tuercasuelta.net.UiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.LongConsumer;

/** Toda la logica de la partida de Tuercasuelta. Todo ocurre en el hilo del servidor. */
public final class Game {
	public static final Game I = new Game();
	public static MinecraftServer server;

	// ---- Ajustes ----
	static final int SLOTS = 9;
	static final int DRAFT_VOTE_TICKS = 25 * 20;
	static final int RESOLVE_TICKS = 6 * 20;
	static final int GATHER_TICKS = 25 * 60 * 20;
	static final int POWER_COOLDOWN = 30 * 20;
	static final int FIRST_POWER_DELAY = 45 * 20;
	static final int VOTE_TICKS = 30 * 20;
	static final int VOTE_COOLDOWN = 90 * 20;
	static final int GHOST_COOLDOWN = 25 * 20;
	static final int FORCE_EVERY = 4; // el saboteur puede forzar una vez cada 4 opciones

	enum State { IDLE, DRAFT, GATHER, VOTING, ENDING }

	enum Phase { WAIT, VOTE, RESOLVE }

	record Task(long at, Runnable run) {}

	State state = State.IDLE;
	Phase phase = Phase.WAIT;
	long tick;
	final Random rnd = new Random();

	final List<String> players = new ArrayList<>();
	final Set<String> ghosts = new HashSet<>();
	String saboteur;
	int rx, ry, rz; // posicion base del robot

	final Items.Item[] card = new Items.Item[SLOTS];
	final boolean[] delivered = new boolean[SLOTS];
	final Set<String> usedItems = new HashSet<>();

	// draft
	int round;
	Items.Item optA, optB;
	final Map<String, Integer> draftVotes = new HashMap<>();
	long phaseAt;
	int teamPick;
	int forceReadyRound;

	// poderes
	final Map<String, Integer> charges = new LinkedHashMap<>();
	long powerReadyAt;

	// votacion
	final Map<String, String> votes = new HashMap<>();
	long voteDeadline, nextVoteAt;
	long gatherEnd;
	final Map<String, Long> ghostReadyAt = new HashMap<>();

	// tareas y finales
	final List<Task> tasks = new ArrayList<>();
	LongConsumer endingLoop;
	long endingStart;

	private Game() {}

	// =====================================================================
	// Utilidades
	// =====================================================================

	static String name(ServerPlayer p) {
		return p.getName().getString();
	}

	ServerPlayer sp(String n) {
		return server == null ? null : server.getPlayerList().getPlayerByName(n);
	}

	List<String> alive() {
		List<String> r = new ArrayList<>();
		for (String n : players) if (!ghosts.contains(n)) r.add(n);
		return r;
	}

	List<String> aliveOnline() {
		List<String> r = new ArrayList<>();
		for (String n : players) if (!ghosts.contains(n) && sp(n) != null) r.add(n);
		return r;
	}

	boolean sabActive() {
		return saboteur != null && !ghosts.contains(saboteur);
	}

	boolean isSab(ServerPlayer p) {
		return saboteur != null && saboteur.equals(name(p));
	}

	void say(ServerPlayer p, String msg) {
		p.sendSystemMessage(Component.literal(msg));
	}

	void broadcast(String msg) {
		for (String n : players) {
			ServerPlayer p = sp(n);
			if (p != null) say(p, msg);
		}
	}

	void ui(ServerPlayer p, String kind, String data) {
		ServerPlayNetworking.send(p, new UiPayload(kind, data));
	}

	void schedule(long delayTicks, Runnable r) {
		tasks.add(new Task(tick + delayTicks, r));
	}

	static String[] e(String label, String action, String arg) {
		return new String[]{label, action, arg};
	}

	void panel(ServerPlayer p, String title, String sub, List<String[]> entries) {
		StringBuilder sb = new StringBuilder(title).append('\n').append(sub);
		for (String[] en : entries) {
			sb.append('\n').append(en[0]).append('|').append(en[1]).append('|').append(en[2]);
		}
		ui(p, "panel", sb.toString());
	}

	int deliveredCount() {
		int c = 0;
		for (boolean b : delivered) if (b) c++;
		return c;
	}

	static String clock(long ticks) {
		long s = Math.max(0, ticks) / 20;
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}

	void reset() {
		state = State.IDLE;
		phase = Phase.WAIT;
		players.clear();
		ghosts.clear();
		saboteur = null;
		java.util.Arrays.fill(card, null);
		java.util.Arrays.fill(delivered, false);
		usedItems.clear();
		draftVotes.clear();
		votes.clear();
		charges.clear();
		ghostReadyAt.clear();
		tasks.clear();
		endingLoop = null;
		round = 0;
		forceReadyRound = 0;
	}

	// =====================================================================
	// Entrada de acciones (comandos y botones)
	// =====================================================================

	public void handleAction(ServerPlayer p, String action, String arg) {
		if (server == null) return;
		switch (action) {
			case "menu" -> openLobby(p);
			case "start" -> start(p, arg);
			case "panel" -> openPanel(p);
			case "pick" -> pick(p, arg);
			case "force" -> force(p);
			case "deliver" -> deliver(p);
			case "cartilla" -> showCard(p);
			case "callvote" -> callVote(p);
			case "vote" -> vote(p, arg);
			case "power" -> Powers.use(this, p, arg);
			case "ghost" -> Powers.ghost(this, p, arg);
			case "stop" -> stop(p);
			case "help" -> help(p);
			case "test" -> testEnding(p, arg);
			default -> { }
		}
	}

	void help(ServerPlayer p) {
		say(p, "=== Tuercasuelta ===");
		say(p, "/tuerca menu : abre el menu para iniciar la partida");
		say(p, "/tuerca panel : abre tu panel (draft, poderes, votacion)");
		say(p, "/tuerca cartilla : muestra la cartilla de piezas");
		say(p, "/tuerca entregar : entrega una pieza que lleves en el inventario");
		say(p, "/tuerca votar : convoca una votacion para expulsar al saboteador");
		say(p, "/tuerca parar : termina la partida");
		say(p, "/tuerca probar 1-8 : prueba un final (modo de prueba)");
	}

	void openLobby(ServerPlayer p) {
		if (state != State.IDLE) {
			say(p, "Ya hay una partida en curso. Usa /tuerca panel o /tuerca parar.");
			return;
		}
		List<String> names = new ArrayList<>();
		for (ServerPlayer o : server.getPlayerList().getPlayers()) names.add(name(o));
		ui(p, "lobby", String.join(",", names));
	}

	void stop(ServerPlayer p) {
		if (state == State.IDLE) {
			say(p, "No hay ninguna partida.");
			return;
		}
		broadcast("La partida fue terminada por " + name(p) + ".");
		cleanup();
	}

	// =====================================================================
	// Inicio
	// =====================================================================

	void start(ServerPlayer starter, String names) {
		if (state != State.IDLE) {
			say(starter, "Ya hay una partida en curso. Usa /tuerca parar para terminarla.");
			return;
		}
		List<String> chosen = new ArrayList<>();
		if (names == null || names.isBlank()) {
			for (ServerPlayer o : server.getPlayerList().getPlayers()) chosen.add(name(o));
		} else {
			for (String s : names.trim().split("[ ,]+")) {
				if (!s.isEmpty() && sp(s) != null && !chosen.contains(s)) chosen.add(s);
			}
		}
		if (chosen.isEmpty()) {
			say(starter, "No encontre jugadores para la partida.");
			return;
		}
		reset();
		players.addAll(chosen);
		saboteur = players.get(rnd.nextInt(players.size()));
		rx = (int) Math.floor(starter.getX());
		ry = (int) Math.floor(starter.getY());
		rz = (int) Math.floor(starter.getZ()) + 8;
		Parts.arena(this);
		charges.put("calavera", 1);
		charges.put("esconder", 2);
		charges.put("cambiar", 2);
		charges.put("apagon", 2);
		charges.put("intercambio", 1);
		charges.put("mobs", 2);

		state = State.DRAFT;
		phase = Phase.WAIT;
		phaseAt = tick + 200;
		round = 0;
		forceReadyRound = 0;

		for (String n : players) {
			ServerPlayer p = sp(n);
			if (p == null) continue;
			Cmd.run("gamemode survival " + n);
			Cmd.run("effect clear " + n);
			boolean sab = n.equals(saboteur);
			ui(p, "role", sab ? "saboteur" : "team");
			if (sab) {
				Cmd.title(n, "ERES EL SABOTEADOR", "Que nadie lo sepa...", "dark_red");
				Cmd.sound(n, "minecraft:entity.wither.spawn", 0.8f, 0.7f);
			} else {
				Cmd.title(n, "ERES DEL EQUIPO", "Construyan el robot. Hay un traidor.", "green");
				Cmd.sound(n, "minecraft:entity.player.levelup", 1f, 1f);
			}
		}
		broadcast("=== TUERCASUELTA === Participan: " + String.join(", ", players));
		if (players.size() < 3) {
			broadcast("(Modo de prueba: con menos de 3 jugadores no hay partida equilibrada.)");
		}
		broadcast("Primero el DRAFT: van a elegir las piezas del bingo. Empieza en 10 segundos.");
	}

	// =====================================================================
	// Draft (bingo)
	// =====================================================================

	boolean forceAvailable() {
		return sabActive() && round >= forceReadyRound;
	}

	void beginRound() {
		List<Items.Item> pool = new ArrayList<>(Items.POOL);
		Collections.shuffle(pool, rnd);
		List<Items.Item> opts = new ArrayList<>();
		for (Items.Item it : pool) {
			if (!usedItems.contains(it.id())) opts.add(it);
			if (opts.size() == 2) break;
		}
		optA = opts.get(0);
		optB = opts.get(1);
		draftVotes.clear();
		phase = Phase.VOTE;
		phaseAt = tick + DRAFT_VOTE_TICKS;
		broadcast("--- Ronda " + (round + 1) + "/" + SLOTS + " --- 1) " + optA.name() + "   o   2) " + optB.name());
		for (String n : aliveOnline()) {
			ServerPlayer p = sp(n);
			if (p != null) draftPanel(p);
		}
	}

	void draftPanel(ServerPlayer p) {
		List<String[]> en = new ArrayList<>();
		en.add(e("1) " + optA.name(), "pick", "1"));
		en.add(e("2) " + optB.name(), "pick", "2"));
		panel(p, "RONDA " + (round + 1) + " DE " + SLOTS, "Elijan la pieza que van a tener que conseguir", en);
	}

	void forcePanel(ServerPlayer p) {
		Items.Item other = teamPick == 1 ? optB : optA;
		List<String[]> en = new ArrayList<>();
		en.add(e("FORZAR la otra: " + other.name(), "force", ""));
		en.add(e("Dejarlo asi", "close", ""));
		panel(p, "SABOTEADOR", "El equipo eligio otra cosa. ¿Torces el destino?", en);
	}

	void pick(ServerPlayer p, String arg) {
		String n = name(p);
		if (state != State.DRAFT || phase != Phase.VOTE) {
			say(p, "Ahora no se esta votando una pieza.");
			return;
		}
		if (!players.contains(n)) return;
		int choice = "2".equals(arg.trim()) ? 2 : 1;
		draftVotes.put(n, choice);
		say(p, "Votaste por: " + (choice == 1 ? optA.name() : optB.name()));
		if (draftVotes.size() >= aliveOnline().size()) resolveDraft();
	}

	void resolveDraft() {
		int c1 = 0, c2 = 0;
		for (int v : draftVotes.values()) {
			if (v == 1) c1++;
			else c2++;
		}
		if (c1 == c2) teamPick = rnd.nextBoolean() ? 1 : 2;
		else teamPick = c1 > c2 ? 1 : 2;
		phase = Phase.RESOLVE;
		Items.Item chosen = teamPick == 1 ? optA : optB;
		broadcast("El equipo eligio: " + chosen.name() + "  (" + c1 + " vs " + c2 + ")");
		if (forceAvailable()) {
			phaseAt = tick + RESOLVE_TICKS;
			ServerPlayer s = sp(saboteur);
			if (s != null) forcePanel(s);
		} else {
			phaseAt = tick + 50;
		}
	}

	void force(ServerPlayer p) {
		if (state != State.DRAFT || phase != Phase.RESOLVE || !isSab(p) || !forceAvailable()) {
			say(p, "No puedes forzar ahora.");
			return;
		}
		teamPick = teamPick == 1 ? 2 : 1;
		forceReadyRound = round + FORCE_EVERY;
		Items.Item chosen = teamPick == 1 ? optA : optB;
		broadcast("¡Alguien torcio el destino! La pieza ahora es: " + chosen.name());
		for (String n : players) Cmd.sound(n, "minecraft:entity.lightning_bolt.thunder", 0.6f, 1.3f);
		say(p, "Forzaste la eleccion. Podras volver a hacerlo en " + FORCE_EVERY + " opciones.");
		phaseAt = tick + 40;
	}

	void finishRound() {
		Items.Item chosen = teamPick == 1 ? optA : optB;
		card[round] = chosen;
		usedItems.add(chosen.id());
		broadcast("Pieza " + (round + 1) + "/" + SLOTS + " de la cartilla: " + chosen.name());
		round++;
		phase = Phase.WAIT;
		phaseAt = tick + 50;
		if (round >= SLOTS) startGather();
	}

	// =====================================================================
	// Fase de conseguir piezas
	// =====================================================================

	void startGather() {
		state = State.GATHER;
		phase = Phase.WAIT;
		gatherEnd = tick + GATHER_TICKS;
		nextVoteAt = tick + 600;
		powerReadyAt = tick + FIRST_POWER_DELAY;
		broadcast("¡Cartilla completa! Consigan los objetos y entreguenlos para construir el robot.");
		broadcast("Tienen " + clock(GATHER_TICKS) + " minutos. Usen /tuerca panel.");
		for (String n : players) {
			ServerPlayer p = sp(n);
			if (p == null) continue;
			showCard(p);
			if (n.equals(saboteur)) say(p, "Tus poderes se activan en 45 segundos. Usa /tuerca panel.");
			gatherPanel(p);
		}
	}

	void showCard(ServerPlayer p) {
		if (state == State.IDLE) {
			say(p, "No hay partida.");
			return;
		}
		say(p, "--- CARTILLA DEL ROBOT ---");
		for (int i = 0; i < SLOTS; i++) {
			if (card[i] == null) continue;
			say(p, (delivered[i] ? "[X] " : "[ ] ") + (i + 1) + ". " + card[i].name() + "  (" + Parts.NAMES[i] + ")");
		}
	}

	void gatherPanel(ServerPlayer p) {
		String n = name(p);
		boolean ghost = ghosts.contains(n);
		List<String[]> en = new ArrayList<>();
		String title;
		if (ghost) {
			title = "PANEL FANTASMA";
			for (String o : alive()) {
				if (!o.equals(n)) en.add(e("Robar objeto a " + o, "ghost", "robar:" + o));
			}
			en.add(e("Dar un susto", "ghost", "susto:"));
		} else {
			title = n.equals(saboteur) && sabActive() ? "PANEL DEL SABOTEADOR" : "PANEL DEL EQUIPO";
			en.add(e("Entregar pieza (la que lleves encima)", "deliver", ""));
			en.add(e("Ver cartilla en el chat", "cartilla", ""));
			en.add(e("Convocar votacion", "callvote", ""));
			if (n.equals(saboteur) && sabActive()) {
				for (Map.Entry<String, Integer> c : charges.entrySet()) {
					if (c.getValue() > 0) {
						en.add(e("Poder: " + Powers.label(c.getKey()) + " (" + c.getValue() + ")", "power", c.getKey()));
					}
				}
			}
		}
		en.add(e("Cerrar", "close", ""));
		String sub = "Robot " + deliveredCount() + "/" + SLOTS + "  -  Tiempo " + clock(gatherEnd - tick);
		panel(p, title, sub, en);
	}

	void openPanel(ServerPlayer p) {
		String n = name(p);
		if (!players.contains(n)) {
			say(p, state == State.IDLE ? "No hay partida. Usa /tuerca menu." : "No participas en esta partida.");
			return;
		}
		switch (state) {
			case DRAFT -> {
				if (phase == Phase.VOTE) draftPanel(p);
				else if (phase == Phase.RESOLVE && isSab(p) && forceAvailable()) forcePanel(p);
				else say(p, "Espera un momento...");
			}
			case GATHER -> gatherPanel(p);
			case VOTING -> {
				if (!ghosts.contains(n)) votePanel(p);
				else say(p, "Hay una votacion en curso.");
			}
			default -> say(p, "No hay panel disponible ahora.");
		}
	}

	void deliver(ServerPlayer p) {
		String n = name(p);
		if (state != State.GATHER && state != State.VOTING) {
			say(p, "Todavia no se puede entregar.");
			return;
		}
		if (!players.contains(n) || ghosts.contains(n)) {
			say(p, "Los fantasmas no pueden entregar piezas.");
			return;
		}
		for (int i = 0; i < SLOTS; i++) {
			if (delivered[i] || card[i] == null) continue;
			if (Cmd.run("clear " + n + " " + card[i].id() + " 1") > 0) {
				delivered[i] = true;
				Parts.place(this, i, true);
				broadcast(n + " entrego: " + card[i].name() + "  (" + deliveredCount() + "/" + SLOTS + ")");
				for (String o : players) Cmd.sound(o, "minecraft:block.anvil.use", 0.7f, 1.2f);
				if (deliveredCount() == SLOTS) teamWins();
				return;
			}
		}
		say(p, "No llevas ninguna pieza de la cartilla en el inventario.");
	}

	// =====================================================================
	// Votacion
	// =====================================================================

	void callVote(ServerPlayer p) {
		String n = name(p);
		if (state != State.GATHER) {
			say(p, "Ahora no se puede convocar una votacion.");
			return;
		}
		if (!players.contains(n) || ghosts.contains(n)) return;
		if (tick < nextVoteAt) {
			say(p, "Falta " + ((nextVoteAt - tick) / 20 + 1) + " s para poder votar de nuevo.");
			return;
		}
		state = State.VOTING;
		votes.clear();
		voteDeadline = tick + VOTE_TICKS;
		broadcast("¡" + n + " convoco una VOTACION! Tienen 30 segundos para decidir a quien expulsar.");
		for (String o : players) Cmd.sound(o, "minecraft:block.bell.use", 1f, 0.8f);
		for (String o : aliveOnline()) {
			ServerPlayer q = sp(o);
			if (q != null) votePanel(q);
		}
	}

	void votePanel(ServerPlayer p) {
		String me = name(p);
		List<String[]> en = new ArrayList<>();
		for (String o : alive()) {
			if (!o.equals(me)) en.add(e("Expulsar a " + o, "vote", o));
		}
		en.add(e("Abstenerme", "vote", "-"));
		panel(p, "VOTACION", "¿Quien es el saboteador?", en);
	}

	void vote(ServerPlayer p, String target) {
		String n = name(p);
		if (state != State.VOTING || !players.contains(n) || ghosts.contains(n)) {
			say(p, "Ahora no se puede votar.");
			return;
		}
		if (!"-".equals(target) && (!alive().contains(target) || target.equals(n))) {
			say(p, "Voto invalido.");
			return;
		}
		votes.put(n, target);
		say(p, "Voto registrado.");
		if (votes.size() >= aliveOnline().size()) resolveVote();
	}

	void resolveVote() {
		Map<String, Integer> tally = new HashMap<>();
		for (String t : votes.values()) {
			if (!"-".equals(t)) tally.merge(t, 1, Integer::sum);
		}
		String top = null;
		int best = 0;
		boolean tie = false;
		for (Map.Entry<String, Integer> en : tally.entrySet()) {
			if (en.getValue() > best) {
				best = en.getValue();
				top = en.getKey();
				tie = false;
			} else if (en.getValue() == best) {
				tie = true;
			}
		}
		state = State.GATHER;
		nextVoteAt = tick + VOTE_COOLDOWN;
		int needed = aliveOnline().size() / 2 + 1;
		if (top == null || tie || best < needed) {
			broadcast("La votacion termino sin mayoria. Nadie fue expulsado.");
			return;
		}
		expel(top);
	}

	void expel(String n) {
		ghosts.add(n);
		Cmd.run("gamemode spectator " + n);
		boolean wasSab = n.equals(saboteur);
		for (String o : players) Cmd.sound(o, "minecraft:entity.ender_dragon.growl", 0.7f, 1f);
		if (wasSab) {
			broadcast("¡" + n + " fue expulsado... y ERA EL SABOTEADOR! Ya no tiene poderes, pero ahora es un fantasma.");
		} else {
			broadcast(n + " fue expulsado... pero era INOCENTE. Ahora es un fantasma.");
		}
		ServerPlayer gp = sp(n);
		if (gp != null) {
			Cmd.title(n, "FUISTE EXPULSADO", "Ahora eres un fantasma: usa /tuerca panel", "gray");
			gatherPanel(gp);
		}
		if (players.size() >= 3 && sabActive() && alive().size() <= 2) {
			broadcast("Quedan muy pocos jugadores en pie...");
			saboteurWins();
		}
	}

	// =====================================================================
	// Finales
	// =====================================================================

	void teamWins() {
		Endings.start(this, true, -1);
	}

	void saboteurWins() {
		Endings.start(this, false, -1);
	}

	void testEnding(ServerPlayer p, String arg) {
		if (state != State.IDLE) {
			say(p, "Termina la partida actual primero (/tuerca parar).");
			return;
		}
		int n;
		try {
			n = Integer.parseInt(arg.trim());
		} catch (NumberFormatException ex) {
			say(p, "Usa /tuerca probar <1-8>. 1-4: finales del equipo. 5-8: finales del saboteador.");
			return;
		}
		if (n < 1 || n > 8) {
			say(p, "Usa un numero del 1 al 8.");
			return;
		}
		reset();
		players.add(name(p));
		saboteur = name(p);
		rx = (int) Math.floor(p.getX());
		ry = (int) Math.floor(p.getY());
		rz = (int) Math.floor(p.getZ()) + 8;
		Parts.arena(this);
		for (int i = 0; i < SLOTS; i++) Parts.place(this, i, true);
		Endings.start(this, n <= 4, (n - 1) % 4);
	}

	void finishEnding(boolean team) {
		endingLoop = null;
		for (String n : players) {
			if (team) {
				Cmd.title(n, "¡GANO EL EQUIPO!", "El saboteador era " + saboteur, "green");
				Cmd.sound(n, "minecraft:ui.toast.challenge_complete", 1f, 1f);
			} else {
				Cmd.title(n, "¡GANO EL SABOTEADOR!", "Era " + saboteur, "dark_red");
				Cmd.sound(n, "minecraft:entity.wither.death", 0.8f, 1f);
			}
		}
		schedule(140, this::cleanup);
	}

	void cleanup() {
		Cmd.run("kill @e[tag=tuerca_robot]");
		for (String n : players) {
			Cmd.run("effect clear " + n);
			Cmd.run("gamemode survival " + n);
		}
		broadcast("Partida terminada. Usa /tuerca menu para jugar otra vez.");
		reset();
	}

	// =====================================================================
	// Tick
	// =====================================================================

	public void tick(MinecraftServer s) {
		server = s;
		tick++;
		if (!tasks.isEmpty()) {
			List<Task> due = new ArrayList<>();
			for (Task t : tasks) if (t.at() <= tick) due.add(t);
			tasks.removeAll(due);
			for (Task t : due) t.run().run();
		}
		switch (state) {
			case DRAFT -> {
				if (phase == Phase.WAIT && tick >= phaseAt) beginRound();
				else if (phase == Phase.VOTE && tick >= phaseAt) resolveDraft();
				else if (phase == Phase.RESOLVE && tick >= phaseAt) finishRound();
			}
			case GATHER, VOTING -> {
				if (tick % 20 == 0) {
					for (String n : players) {
						if (sp(n) == null) continue;
						Cmd.actionbar(n, "Robot " + deliveredCount() + "/" + SLOTS + "   Tiempo " + clock(gatherEnd - tick), "gold");
					}
				}
				if (state == State.VOTING && tick >= voteDeadline) resolveVote();
				if ((state == State.GATHER || state == State.VOTING) && tick >= gatherEnd) {
					broadcast("¡Se acabo el tiempo!");
					saboteurWins();
				}
			}
			case ENDING -> {
				if (endingLoop != null) endingLoop.accept(tick - endingStart);
			}
			default -> { }
		}
	}
}
