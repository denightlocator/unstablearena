# UnstableCharacters Plugin

A Minecraft **Purpur 1.21.1** plugin for the **Unstable Arena** server featuring character claiming from the actual Unstable SMP cast, plus a full in-game kit management system.

## Requirements

- Java 21+
- Purpur 1.21.1 (or Paper 1.21.1 / any Paper fork)
- Maven or Gradle (only for building from source)

> The jar is compiled against the Paper 1.21.1 API — Purpur is a Paper fork, so it runs on Purpur out of the box.

## Features

### Character System

- **Claim Characters**: `/claimcharacter <name>` — you *become* that character:
  - Name changes in the **tab list**
  - Name changes **above your head** (nametag)
  - Name changes in **chat** with clan prefix/suffix
- **70+ real Unstable SMP characters** organized by faction:
  - **Central Protagonists**: Wemmbu, ParrotX2, SpokeIsHere, FlameFrags
  - **Allies**: Wifies, Eggchan, Mapicc, TheobaldTheBird, Lomedy, Spongs, Rejoicin, ReinaDrop
  - **Zam Empire**: PrinceZam, SargeLAW, Horace Altman
  - **Cindercrest**: Saparata, ShoeBilly
  - **Mafia**: TheGodOfWar, Deputy_Ace
  - **Antagonists**: ClownPierce, JamatoP, Ashswagg, Jaden_MAN, The NULL
  - **Kingdom of the Caves**: Arachn1d
  - **Soul Keepers**, **Law Enforcement**, and 40+ neutral players!
- One character = one owner (no duplicate claims)
- `/characters` — browse everything grouped by clan (click to claim)
- `/unclaim` — reset your name

### Character Kits (Based on Actual Unstable SMP Gear)

Each major character has a kit matching their wiki gear, **with armor trims**:

| Character | Kit Highlights |
|---|---|
| Wemmbu | Crucible & Gambit (maces*), Orbital Strike triggers, 128 TNT, TNT minecarts |
| FlameFrags | The Flame, Incinerator (mace*), Carbonizer, Wolf Rod, fire charges |
| SpokeIsHere | **Redstone-trim Godset** (vex/snout/coast/eye), full trap kit |
| ParrotX2 | **Silence trim** diamond, 64 TNT minecarts, end crystals |
| PrinceZam | **Gold trim** (sentry/eye/coast), army supplies |
| ClownPierce | **Silence trim**, Thorns armor, Nightmare (mace*) |
| Saparata | **Silence trim** Cindercrest king set |
| MinuteTech | Full redstone lab kit |

\* Maces only exist in 1.21.2+. On 1.21.1 they automatically fall back to netherite swords.

**Faction army presets:**

| Faction | Kits | Trim |
|---|---|---|
| Null Army | `nullarmy`, `nullhunter` | Flow (raiser chest for squad leader) |
| Mafia | `mafiasoldier` (no trim), `mafiadiamond`, `mafiagold` | Sentry/Eye on elite sets |
| Cindercrest | `saparata`, `shoebilly` | Silence + Host boots |
| Kingdom of the Caves | `kingdomcaves` | Silence/Host |
| Mist Civilization | `mistcivilization` | Bolt (dune fallback) |
| Zam Empire | `princezam` | Gold eye/coast/sentry |

**About the trims:** "Silence" and "Bolt" are Unstable SMP lore trims that don't exist in vanilla 1.21.1, so the plugin maps them to the closest vanilla patterns (`co` and `dune`) via `KitManager.TRIM_FALLBACKS`. If your server adds custom patterns via datapack, edit that map (or the material map `TRIM_MATERIALS`) to the registered keys.

## Commands

### Player Commands

| Command | Permission | Description |
|---|---|---|
| `/claimcharacter [name]` | `unstable.characters.claim` | Claim a character (no arg = list) |
| `/characters [clan]` | `unstable.characters.list` | View all characters |
| `/unclaim` | `unstable.characters.unclaim` | Remove your character |
| `/kit [name]` | `unstable.kits.use` | Receive a kit (no arg = list) |

### Admin Commands (`/kits`)

| Command | Description |
|---|---|
| `/kits create <id> <name>` | Create a new kit |
| `/kits delete <id>` | Delete a kit |
| `/kits edit <id>` | Open edit menu |
| `/kits give <id> [player] [clear]` | Give kit to player |
| `/kits list` | List all kits |
| `/kits cooldown <player> [clear [kitid]]` | Manage cooldowns |
| `/kits category [name]` | View kits by category |
| `/kits reload` | Reload config |

**Kit edit subcommands** (`/kits edit <id> ...`): `name`, `desc`, `cooldown`, `category`, `icon` (hold item), `add` (hold item), `remove <slot>`, `armor <type>` (hold item), `perm <permission|clear>`, `enable`, `disable`, `clear`, `save`

All of it works **in-game** — ops can create and edit unlimited kits while the server is running, no restarts.

## Permissions

```yaml
unstable.characters.*         # All character permissions (op)
unstable.characters.claim     # Claim/unclaim characters (default: true)
unstable.characters.list      # View character list (default: true)
unstable.characters.unclaim   # Unclaim character (default: true)
unstable.characters.admin     # Claim unavailable characters (op)

unstable.kits.*               # All kit permissions (op)
unstable.kits.manage          # Create and edit kits (op)
unstable.kits.use             # Use /kit command (default: true)
unstable.kits.admin           # Give kits to others (op)
```

## Installation

1. Drop `UnstableCharacters-1.0.0.jar` into your `plugins/` folder
2. Restart the server
3. All Unstable SMP characters and character kits are auto-created
4. Tweak everything in `plugins/UnstableCharacters/`

## Building from Source

```bash
cd UnstableCharacters
./build.sh        # or: mvn clean package
# or with Gradle: ./gradlew build
```

Output: `target/UnstableCharacters-1.0.0.jar`

## Configuration Files

| File | Purpose |
|---|---|
| `plugins/UnstableCharacters/config.yml` | Main config (broadcasts, cooldowns) |
| `plugins/UnstableCharacters/characters.yml` | Character definitions (edit names, clans, prefixes, glow…) |
| `plugins/UnstableCharacters/kits.yml` | Kit metadata |
| `plugins/UnstableCharacters/kit_data/*.dat` | Kit inventory data (NBT) |
| `plugins/UnstableCharacters/playerdata.yml` | Player claims and kit cooldowns |

## Character List

**Protagonists:** Wemmbu, ParrotX2, SpokeIsHere, FlameFrags
**Allies:** Wifies, Eggchan, Mapicc, TheobaldTheBird, Lomedy, Spongs, Rejoicin, ReinaDrop, MarLowww
**Zam Empire:** PrinceZam, SargeLAW, Horace Altman
**Cindercrest:** Saparata, ShoeBilly
**Mafia:** TheGodOfWar, Deputy_Ace
**Antagonists:** ClownPierce, JamatoP, Ashswagg, Jaden_MAN, The NULL
**Kingdom of the Caves:** Arachn1d
**Soul Keepers:** The Director, Soul Keeper
**Law:** The Law
**Neutral:** MinuteTech, ManePear, ItzRealMe, Swight, FerreMC, Mugm, JumperWho, Wyll, Purpled, Leow0ok, LettuceK, Reddoons, Boomie, Boosfer, Conexion, FalconU, Nufuli, TeamKalal, Jepexx, Fantst, Baablu, MrCube6, Only_A_Squid, Sharkilz, Luke4472, Spepticle, TruOriginal, Roshambogames, Lopezzz, Arcn, Willsion, Fymada, LuigiToan, deanthebean9, Peentar, Woogie, TheRealSquiddo, Rekrap2, Pangi, BranzyCraft, SirPig, Salvationism, Tai, Wallibear, Hannahxxrose, Sarpn, Yungwill

Made for Unstable Arena | Characters & gear from Unstable SMP
**Target: Purpur 1.21.1 | Java 21+**
