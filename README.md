<p align="center">
  <img src="https://i.imgur.com/25SXvgQ.png" alt="Enhanced Movement banner" />
</p>

# Enhanced Movement

A small Minecraft mod that adds **double jump**, **directional dash**, **ledge grab**, and **dash afterimages** — built for **Minecraft 26.1.x** on both **Fabric** and **NeoForge**.

## Downloads

- **[Modrinth](https://modrinth.com/mod/enhanced-movement)** — recommended
- **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/enhanced-movement-fabric)**
- **[GitHub Releases](https://github.com/kestalkayden/enhanced-movement-mod/releases)** — backup direct downloads

A single jar per loader covers Minecraft **26.1**, **26.1.1**, and **26.1.2**.

> ⚠️ Always back up your world before installing new mods. Enhanced Movement is small and doesn't touch world data, but this is good practice in general.

## Features

- **Double jump** with smart fall-damage protection (no damage on small drops from your double-jump peak; real damage on big falls).
- **Directional dash** — double-tap WASD (400 ms window) or bind a dedicated dash key (mouse side buttons, anything).
- **In-air dash** with a slightly reduced speed so jump-dash chains feel intentional, not infinite.
- **Ledge grab** — assists you onto blocks you almost made.
- **Dash afterimages** — visible Matrix-style trail, with an optional rainbow "prism" mode.
- **Fully configurable** via Mod Menu (Fabric) or the NeoForge Mod List config button.

## Requirements

- **Minecraft 26.1, 26.1.1, or 26.1.2**
- **Java 25**
- **Fabric build:** [Fabric Loader 0.18.4+](https://fabricmc.net/use/), [Fabric API 0.149.0+26.1.2+](https://modrinth.com/mod/fabric-api), [Cloth Config 26.1.154](https://modrinth.com/mod/cloth-config), optional [Mod Menu](https://modrinth.com/mod/modmenu)
- **NeoForge build:** [NeoForge 26.1+](https://neoforged.net/), [Cloth Config 26.1.154](https://modrinth.com/mod/cloth-config)

## Quick start

1. Install Fabric Loader or NeoForge for Minecraft 26.1.x.
2. Drop the Enhanced Movement jar plus the dependencies above into your `mods/` folder.
3. Launch the game. Double-tap a movement key to dash; double-press jump for a mid-air boost.
4. To use the dedicated dash keybind: open **Mod Menu → Enhanced Movement → Config**, toggle **`useKeybinds`** to **ON**, then bind a key in **Options → Controls**.

## Why this?

I like movement. I like double jumping. I like dashing. I wanted to challenge myself with focusing on movement.

## Contributing

Issues and pull requests welcome. The code is small, the build is `./gradlew buildAll`, and CI builds both loaders on every push.

## License

CC0-1.0 — public domain. Do whatever you want with this code.
