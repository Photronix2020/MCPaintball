# Splatball: CurseForge listing

Everything to paste into the CurseForge "Create Project" and "Upload File" forms.

## Project fields

| Field | Value |
| --- | --- |
| Project type | Minecraft > Mods |
| Name | Splatball |
| Main category | Adventure and RPG |
| Additional categories | Armor, Tools, and Weapons; Mobs; Server Utility |
| License | All Rights Reserved (matches `gradle.properties`; change both if you want something else) |
| Source URL | https://github.com/Photronix2020/MCPaintball |
| Issues URL | https://github.com/Photronix2020/MCPaintball/issues |

**Summary** (the one-line blurb, under 250 characters):

> Paintball arena shooter. Make a new world and you drop straight into a red vs blue match with bots, five paint markers, and an arena that gets painted as you play. No setup, no commands.

## Description

Paste this into the description editor (it accepts Markdown).

---

# Splatball

**A paintball arena shooter inside Minecraft.** Make a new world and you are dropped straight into a red vs blue match. No setup, no commands, nothing to craft first.

## Join and play

- **New world:** the arena is built in the sky above your spawn, you get the full kit, and bots fill both teams. Everyone who logs in is dropped into the match, and when a match ends the next one starts by itself.
- **Existing world:** your builds are never touched. Place a **Paintball Station** and right-click it to build the arena in the sky above it and drop in.
- A station at the back of each base takes you out again, back to where you joined.

## Five paint markers

Hold **right-click** to fire and press **R** to reload. Every marker has its own 3D model, magazine and feel.

| Marker | Role | Damage | Magazine |
| --- | --- | --- | --- |
| Splat Pistol | Accurate sidearm | 5 | 12 |
| Splatter SMG | Fast spray | 3 | 30 |
| Scatter Blaster | 7-pellet shotgun | 3 x7 | 5 |
| Long Shot | Sniper, sneak to zoom | 16 | 4 |
| Paint Launcher | Lobbed splash of paint | 10 + splash | 3 |

Players have 20 health. Run out and you are **splatted**: the other team scores and you respawn at your base with a few seconds of protection. Nobody actually dies and your inventory is never cleared.

## The arena paints itself

The 81 x 145 arena is white when the match starts. Every paintball recolours what it hits in the shooter's team colour, so the map fills with red and blue as you play. It has towers, ramps, bunkers, a centre platform with a tunnel underneath, and it is mirrored so both teams get the same map. Each new match washes it clean.

## Bots

Bots fill each team up to four and step aside as real players join, so it works solo, with a friend, or on a full server. They push toward the enemy base, shoot what they can see, and score and respawn like players.

## For server owners

- First team to 25 splats wins, or the best score after 8 minutes.
- `config/splatball-common.toml` controls auto-play, bot count, win target, match length, damage and whether paint works outside the arena.
- `/paintball start`, `stop`, `join red|blue`, `leave`, `arena build`, `arena reset` are there if you want manual control.
- On a dedicated server set `pvp=true` in `server.properties`, or paintballs cannot hurt players.

## Requirements

- Minecraft **1.20.1**
- **Forge 47** or newer
- Needed on both the client and the server

---

## File upload fields

| Field | Value |
| --- | --- |
| File | `build\libs\splatball-1.2.0.jar` |
| Display name | Splatball 1.2.0 |
| Release type | Beta (it has only been played solo so far; switch to Release once you have tried it with other people) |
| Game version | 1.20.1 |
| Mod loader | Forge |
| Java version | Java 17 |
| Environment | Client and Server |
| Dependencies | none |

**Changelog:**

```
1.2.0
- Auto-play: a new world builds the arena and drops every player into the match on login; matches repeat.
- Bots fill both teams to four and step aside as players join.
- Original 3D models for all five markers, held in an aiming pose by players and bots.
- Paintball Station block for joining without commands in existing worlds.
- Arena doubled to 81 x 145 with more towers and cover.
- Building the arena clears animals and dropped items from it.
- Fixed the arena appearing dark when logging in while it was being built.
```

## Before you submit

- **Author name:** `gradle.properties` still says `mod_authors=Splatball Team`. Change it and rebuild if you want your name in the in-game mod list.
- **Logo:** CurseForge requires a square project image (400 x 400 or larger). There isn't one yet.
- **Screenshots:** the listing does much better with a few. `run\screenshots` has shots from testing; in-match shots with paint on the walls would sell it best.
- **Name check:** search CurseForge for "Splatball" first. If it is taken the project name and URL slug need a variant, though the mod id inside the jar can stay.
