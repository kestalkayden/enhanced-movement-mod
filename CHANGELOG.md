# Changelog

All notable changes to this project are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows [SemVer](https://semver.org/) with a `+mcXX.Y.Z` suffix for the target Minecraft version.

## [2.0.2+mc26.1.2] - 2026-05-19

### Fixed
- **Afterimage trails now render under shader packs (Iris/Oculus).** The previous renderer used `RenderTypes.debugQuads()`, whose `pipeline/debug_quads` shader program is intentionally not in shader-pack override lists (debug pipelines aren't meant for the final image). Trails were silently failing under any shader pack. Switched to `RenderTypes.entityTranslucentEmissive()`, which uses the standard entity rendering pipeline that every shader pack handles. Blend, depth, and cull semantics are unchanged. Visual side effect: emissive surfaces stay self-bright regardless of ambient light — actually a feature for ghost trails.
- **Keybind options now shows "Enhanced Movement" as the category header** instead of the literal translation key. The lang file used the vanilla legacy format `key.categories.enhancedmovement`, but mod-registered categories use `key.category.<namespace>.<path>`. Corrected to `key.category.enhancedmovement.main`.

### Added
- 1x1 white pixel texture at `assets/enhancedmovement/textures/effect/white.png` (70 bytes) for the emissive render type to sample. Uniform white means no color tinting from the texture; the per-vertex color drives the actual hue.

## [2.0.1+mc26.1.2] - 2026-05-16

### Changed
- Refreshed README for Minecraft 26.1.x.

## [2.0.0+mc26.1.2] - 2026-05-16

### Changed
- Rewritten for Minecraft 26.1.2 on Fabric 0.18.4 + NeoForge 26.1.2.55-beta with Mojang official mappings.
- Multiloader project layout: per-loader subprojects (`fabric/`, `neoforge/`) sharing `shared-resources/` for assets and lang.
- CI builds both loaders on every push; auto-publishes to Modrinth and CurseForge on `v*` tags.
