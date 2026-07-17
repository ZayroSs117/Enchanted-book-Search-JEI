# Enchanted Book Search for JEI

A client-side Forge mod for **Minecraft 1.20.1** that makes enchanted books easier to find and browse in **Just Enough Items (JEI)**.

JEI normally exposes only part of the enchanted-book list in some modpacks. Lower levels may be missing, configured levels added by Apotheosis may not appear, searching an enchantment name may return nothing, and the visible order can look like `VIII, I, II, III...`.

This mod rebuilds that part of JEI so every supported level is searchable and displayed in ascending order.

## Features

- Adds one enchanted book for every level between the enchantment's real minimum and maximum.
- Reads Apotheosis-configured maximum levels when Apotheosis is installed.
- Supports vanilla and modded enchantments registered through Forge.
- Adds JEI search aliases based on the translated enchantment name and registry ID.
- Sorts levels from lowest to highest, for example `Lure I` through `Lure VIII`.
- Preserves unusual enchanted books contributed by other mods, including books with custom NBT or multiple enchantments.
- Changes only the JEI ingredient list. It does **not** change enchanting rules, recipes, loot, balance, or server gameplay.

## Example

Searching for `sharpness` can display:

```text
Sharpness I
Sharpness II
Sharpness III
...
Sharpness VIII
Sharpness IX
```

The final level depends on the enchantment configuration used by the current modpack.

## Requirements

| Dependency | Supported version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.2.0 or newer for Minecraft 1.20.1 |
| JEI | 15.20.0.130 or newer |
| Java | 17 |
| Apotheosis | Optional |

## Installation

1. Install the required Forge and JEI versions.
2. Download the mod JAR.
3. Place it in the client instance's `mods` folder.
4. Restart Minecraft completely.
5. Search an enchantment name in JEI, such as `protection`, `sharpness`, or `lure`.

This is a **client-side-only** mod. A dedicated server does not need it. Forge's version check is disabled for the server side, so players can use it as a personal JEI improvement.

## JEI configuration

JEI search aliases must remain enabled. In JEI's client configuration, verify that ingredient alias search is enabled if enchantment names do not return results.

## Compatibility notes

- Without Apotheosis, the mod uses the minimum and maximum levels reported by each registered enchantment.
- With Apotheosis, it attempts to use Apotheosis's effective maximum-level hook so the displayed books match the configured limits.
- A defensive limit of 255 prevents malformed configurations from creating an excessive number of JEI entries.
- The mod cannot make an impossible level obtainable. It only displays levels reported as supported by the installed mods and configuration.

## Building from source

The repository includes lightweight API stubs and a regression test used to validate the JEI integration logic without distributing Minecraft, Forge, JEI, or Apotheosis code.

Requirements:

- JDK 17
- Bash, PowerShell, or equivalent command-line tools

Run on Linux/macOS:

```bash
./scripts/build.sh
./scripts/test.sh
```

Run on Windows PowerShell:

```powershell
./scripts/build.ps1
./scripts/test.ps1
```

The built JAR is written to `build/libs/`.

## Project structure

```text
src/main/java/        Mod and JEI plugin source
src/main/resources/   Forge metadata and resource-pack metadata
stubs/src/             Minimal compile-time API stubs
test/                  Standalone regression test
scripts/               Reproducible local build and test scripts
```

## Troubleshooting

### The mod loads, but searching an enchantment still returns nothing

Confirm that:

- JEI is at least version `15.20.0.130`;
- JEI ingredient aliases are enabled;
- only one version of this mod is present in the `mods` folder;
- Minecraft was fully restarted after changing the JAR.

### A log mentions a missing `fabric.mod.json`

Some compatibility mods inspect every JAR for Fabric metadata, even when the JAR is a valid Forge mod. This message is unrelated to this mod's Forge metadata as long as Forge reports `enchantedbooksearch` as loaded.

### A displayed level cannot be obtained in game

This mod does not create enchanting mechanics. The level may be registered or configured for internal use while remaining unavailable through normal gameplay in that particular modpack.

## License

Licensed under the [MIT License](LICENSE).

## Disclaimer

This project is not affiliated with Mojang Studios, Microsoft, Forge, JEI, or Apotheosis.

---

## Résumé en français

Ce mod client Forge pour Minecraft 1.20.1 complète la liste des livres enchantés dans JEI. Il ajoute tous les niveaux réellement disponibles, utilise les limites configurées par Apotheosis lorsqu'il est présent, rend les enchantements recherchables par leur nom et classe les niveaux dans l'ordre croissant. Il ne modifie pas le gameplay et n'est pas nécessaire sur le serveur.
