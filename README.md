# Splatball

Paintball arena shooter for Minecraft **1.20.1 (Forge 47+)**. Five paint markers, an arena you build
with one command, and red vs blue matches where every shot paints the map your colour.

## Markers

Hold **right-click** to fire, **R** to reload (rebindable). Each has a magazine and reloads itself when empty.

| Marker | Role | Damage | Magazine |
| --- | --- | --- | --- |
| Splat Pistol | Accurate sidearm | 5 | 12 |
| Splatter SMG | Fast, sprays | 3 | 30 |
| Scatter Blaster | 7-pellet shotgun | 3 x7 | 5 |
| Long Shot | Sniper, sneak to zoom | 16 | 4 |
| Paint Launcher | Lobbed splash of paint | 10 + splash | 3 |

Players have 20 health. Markers are in the Splatball creative tab and have crafting recipes
(iron, a slime ball, and one special part each).

## Playing

**New world: nothing to do.** Create a world with the mod installed and you are dropped straight into
a match: the arena is built in the sky above your spawn, you get the full kit, and bots fill both teams.
Everyone who logs in afterwards is dropped in too, and when a match ends the next one starts by itself.
A Paintball Station is set down next to your spawn for getting back in after you leave. Set `autoPlay = false`
in the config to turn this off.

**Existing world:** auto-play never touches a world that is more than a minute old. There, place a **Paintball Station** (Splatball creative tab, or wool + red dye + blue dye + a slime ball) and
right-click it. The first click builds the arena in the sky 40 blocks above the station; every click
after that drops the player straight into the match with a full kit, starting one if none is running.
Inside the arena there is a station at the back of each base: click it to leave and return to where you joined.

Every paintball hit takes health (see the table above); at zero you are "splatted", the other team
scores, and you respawn at your base. Bots fill each team up to four members (`botsFillTeamTo` in the
config, 0 turns them off) and step aside as real players join, so a match works solo. No commands are needed, but they exist for server owners.

All commands start with `/paintball`. The ones marked *op* need cheats / operator.

| Command | What it does |
| --- | --- |
| `arena build` *(op)* | Shows the footprint, then `arena build confirm` builds the 81 x 145 arena on the ground, centred on you. **It replaces the blocks in that box.** |
| `arena reset` *(op)* | Rebuilds the arena and washes off the paint. |
| `arena tp` | Teleports you into the arena. |
| `join` / `join red` / `join blue` | Pick a side before or during a match. |
| `start [splats]` *(op)* | Starts red vs blue with everyone online. First to the splat count (default 25) or the best score after 8 minutes wins. |
| `stop` *(op)* | Ends the match. |
| `leave` | Leave the match. |
| `kit` *(op)* | Gives you all five markers. |

During a match players are in adventure mode with a team-coloured helmet and jacket (only if those
slots are empty), nobody actually dies (you respawn at your base with 3 seconds of protection), and
your old game mode and position come back when the match ends. Building the arena removes animals and dropped items inside it. Your inventory is never cleared.

White concrete, wool, terracotta, stained glass and carpet take paint. By default that only happens
inside the arena; see `config/splatball-common.toml` to change that, the win target, match length or damage.

On a dedicated server `pvp=true` must be set in `server.properties` or paintballs cannot hurt players.

## Building

Needs JDK 17. `build.bat build` (uses the portable JDK in `..\.tools`) or `gradlew build` writes
`build/libs/splatball-<version>.jar`, which is the file to upload to CurseForge.

- `gradlew runClient` launches a dev client with the mod.
- `gradlew runGameTestServer` runs the end-to-end test in `src/main/java/com/splatball/test`.
- `python tools/gen_assets.py` regenerates the inventory sprites, the station block assets and recipes.
- `python tools/gun_models.py` regenerates the 3D marker models (edit the box lists in that file) and writes `tools/guns_preview.png`.
- Setting the environment variable `SPLATBALL_SHOTS=1` before `gradlew runClient` makes the dev client screenshot every marker in first person and from the side into `run/screenshots`, then quit.
