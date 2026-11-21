# RPGClasses

Minecraft RPG plugin for **Purpur 1.21.11** (Java 21).

## Build
```bash
mvn clean package
```
Artifact: `target/RPGClasses-1.0.0.jar`

## Current foundation
- Personal bound RPG book, left/right click opens the menu; protected from dropping.
- 20 selectable class definitions.
- Attribute point progression (Agility, Strength, Intelligence).
- Mob kill XP and persistent character data.
- Ability upgrade progression to level 100.
- Character information menu.
- Diamond armor on join.
- Party invites via chat, accept/ignore, party membership and friendly-fire prevention.
- Central party checks for future ally-only heals and buffs.

## Development status
This is the initial foundation. Class-specific combat abilities are represented as class data and ability upgrade slots until their detailed mechanics, cooldowns, ranges and balance are specified.

Player data is stored in `plugins/RPGClasses/players.yml`.
