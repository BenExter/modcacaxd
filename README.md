# Tuercasuelta (Minecraft 26.2, Fabric)

Minijuego cooperativo con traicion para 3-4 amigos: construyan un robot gigante
mientras un saboteador secreto lo desarma. TODOS los jugadores deben instalar el mod
(y Fabric API para 26.2).

## 1. Obtener el .jar (sin instalar nada)

1. Cuenta gratis en https://github.com y repositorio nuevo llamado `tuercasuelta`.
2. "uploading an existing file": arrastra TODO el contenido de esta carpeta
   (incluida la carpeta `.github` y `src`). "Commit changes".
3. Pestana "Actions": corre "Compilar mod" (unos minutos).
4. Si termina en verde, baja `tuercasuelta-jar` desde "Artifacts"; dentro esta el .jar.
   (Usa el que NO termina en "-sources".)
5. Pon el .jar en la carpeta `mods`, junto con Fabric API 26.2.
6. Si sale en rojo, copia el error y pasaselo a Claude.

Si la carpeta `.github` no se subio: "Add file" > "Create new file", nombre
`.github/workflows/build.yml` y pega el contenido del archivo.

## 2. Como se juega

Comando principal: `/tuerca menu` (abre el menu bonito para elegir jugadores e iniciar).
Elige una zona plana y abierta: el mod limpia y construye una plataforma ~8 bloques
delante de quien inicia la partida (borra lo que haya en ese espacio).

Flujo:
1. **Roles**: un jugador al azar es el saboteador (pantalla de revelacion solo para el).
2. **Draft (9 rondas)**: cada ronda aparecen 2 objetos; el equipo vota cual va a la cartilla.
   El saboteador puede forzar la otra opcion, pero solo una vez cada 4 opciones.
3. **Conseguir piezas**: consigan los 9 objetos de la cartilla y entreguenlos con
   `/tuerca entregar` (o el boton del panel). Cada entrega construye una parte del robot.
4. **Poderes del saboteador** (cargas limitadas, 30 s entre poderes, activos desde los 45 s):
   calavera + meteoritos/creepers electricos, esconder una pieza, cambiar objetos de la cartilla,
   apagon de 20 s (el saboteador ve), intercambiar a todos de lugar, invocar mobs.
5. **Votacion**: `/tuerca votar` (o el panel). Mayoria expulsa. El expulsado queda de fantasma
   y puede robar objetos del inventario o dar sustos.
6. **Finales**: si el equipo termina el robot, al azar: baila / persigue al impostor / confeti / viaje a la luna.
   Si gana el saboteador (se acaba el tiempo de 25 min o quedan 2 en pie): el robot explota / se lo roba /
   se va volando / mata a todos.

Otros comandos: `/tuerca panel`, `/tuerca cartilla`, `/tuerca parar`, `/tuerca ayuda`.

## 3. Probar solo (modo de prueba)

- `/tuerca probar 1` a `/tuerca probar 8` lanza directamente un final (1-4 equipo, 5-8 saboteador).
- `/tuerca menu` con un solo jugador marcado tambien funciona (tu seras el saboteador).

## 4. Datos tecnicos

- Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.2, Java 25.
- El servidor ejecuta comandos de Minecraft para los efectos (summon, effect, tp, title...).
- El robot final es un golem de hierro escalado x6.
