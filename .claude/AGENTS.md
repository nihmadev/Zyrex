# AGENTS.md

> Локальная проверка Duo: файл изменён тестовой строкой.

## Project Index (Zyrex)

### 1) High-level architecture
- Monorepo with **two products**:
  - Root: Java/Fabric Minecraft mod client (`src/main/java`, `src/main/resources`)
  - `launcher/`: Tauri launcher stack (Rust backend + React/TS frontend)
- Build systems:
  - Root: Gradle + Fabric Loom + Kotlin plugin (`build.gradle`)
  - Launcher: Cargo + Tauri 2 (`launcher/src-tauri/Cargo.toml`)

### 2) Core entrypoints and startup flow
- Mod entrypoint: `lol.zyrex.Zyrex` (`src/main/java/lol/zyrex/Zyrex.java`)
- Fabric metadata: `src/main/resources/fabric.mod.json`
- Mixins registry: `src/main/resources/mixins.json`
- Startup in `Zyrex.onInitialize()` initializes:
  - mod autoload
  - module system and switcher
  - draggable/HUD subsystem
  - file repositories and persisted configs
  - command subsystem
  - listeners/events
  - sound manager
  - account restore/session swap
  - ClickGUI main screen init

### 3) Critical subsystems (Java client)
- **Event bus / events**:
  - Internal event manager under `utils/client/managers/event`
  - Many custom events under `events/*` (block/chat/container/item/player/render/etc.)
- **Module framework**:
  - Base APIs: `features/module/*`
  - Implementations grouped by categories:
    - `features/impl/combat/*`
    - `features/impl/movement/*`
    - `features/impl/player/*`
    - `features/impl/render/*`
    - `features/impl/misc/*`
- **Command framework**:
  - `commands/*` + parser/argument/datatype APIs in `utils/client/managers/api/command/*`
- **Persistence/files**:
  - file manager APIs under `utils/client/managers/file/*`
  - concrete files: modules, macros, friends, staff, proxy, accounts, autobuy configs, etc.
- **Mixin layer**:
  - heavy runtime patching across client, render, input, world, network, entities, inventory
  - canonical list in `mixins.json`

### 4) UI and rendering areas
- HUD components: `display/hud/*`
- ClickGUI: `display/screens/clickgui/**/*`
- Main menu and alt/account screens: `display/screens/mainmenu/**/*`
- Rendering utilities and shader abstractions: `utils/display/**/*`
- GLSL shaders in: `src/main/resources/assets/minecraft/shaders/*`

### 5) Networking and external integration
- WebSocket/IRC/cloud related code exists under:
  - `utils/connection/cloud/*`
  - `utils/connection/irc/*`
  - `utils/connection/auracheckft/*`
- In current `Zyrex.onInitialize()`, external socket initializers are commented as disabled.
- Local libs included from `libs/*.jar` (also shaded into jar by Gradle `jar` task).

### 6) Account/session handling (important)
- Account repository and file:
  - `display/screens/mainmenu/altscreen/impl/AccountRepository.java`
  - `utils/client/managers/file/impl/AccountFile.java`
- Session mutation is done directly via mixin bridge `IMinecraftClient` in `Zyrex.setSession(...)`.
- Uses offline `UserApiService` path after swap.

### 7) Launcher stack (separate app)
- Frontend entry: `launcher/src/App.tsx` (TitleBar + HomePage)
- Tauri backend modules include:
  - auth (`src-tauri/src/auth/*`)
  - client installer/orchestration (`src-tauri/src/client_installer/*`)
  - minecraft launch/runtime (`src-tauri/src/minecraft/*`)
  - media service (`src-tauri/src/media_service/*`)
- This launcher appears to manage installation, dependencies, launch, and auth flows.

### 8) Build/runtime constraints
- Java target: **21** (`build.gradle`, mixin compatibility also JAVA_21)
- Minecraft dependency: `~1.21.4`
- Fabric loader constraint: `>=0.16.8`
- Uses Fabric API + Loom + multiple third-party libs (Caffeine, Gson, WebSocket, OSHI, Kotlin stdlib, etc.)

### 9) Important directories map
- `src/main/java/lol/zyrex/` - main client codebase
- `src/main/resources/` - Fabric metadata, mixins, assets, shaders, translations
- `libs/` - bundled local jars
- `launcher/` - standalone launcher product (React + Tauri/Rust)

### 10) Critical maintenance notes
- Project is large and patch-heavy (mixins + custom events), regressions often emerge from:
  - event order changes
  - packet/entity hooks
  - render pipeline hooks
  - inventory/screen mixins
- Keep module/config serialization compatibility when refactoring file models.
- Verify Java 21 + Fabric/Minecraft version alignment before dependency upgrades.
- Treat session/account and network integration paths as high-risk areas for runtime breakage.

### 11) Initial indexing status
- Root tree scanned and key architecture indexed.
- AGENTS.md created as a working operational map for contributors/agents.
- Extended class-level mapping and design rules added below.

### 12) Extended class map (critical classes and responsibilities)

#### Bootstrap and runtime composition
- `lol.zyrex.Zyrex`
  - Central composition root for the mod runtime.
  - Wires repositories, module provider/switcher, command dispatcher, file manager, listeners, sound, GUI.
  - Owns startup order; changes here can break persistence, module registration, and UI initialization.

#### Module system
- `features/module/Module`
  - Base abstraction for every feature.
  - Owns `state`, bind info, category, visual animation state.
  - State transitions call `activate()/deactivate()` and register/unregister from `EventManager`.
- `features/module/ModuleRepository`
  - Static-like registration list of concrete modules grouped by category.
  - Source of truth for available modules.
- `features/module/ModuleProvider`
  - Query layer to retrieve modules by name/class.
- `features/module/ModuleSwitcher`
  - Global keybind handler that toggles modules on key events.
  - Depends on `EventManager` registration and only toggles when no GUI screen is open.
- `features/module/ModuleCategory`
  - Enum taxonomy used by both runtime and ClickGUI.

#### Settings system
- `features/module/setting/Setting`
  - Base setting metadata (`name`, `description`, conditional visibility predicate).
- `features/module/setting/SettingComponentAdder`
  - Factory bridge from setting model to UI setting components.
- `display/screens/clickgui/components/implement/settings/*`
  - Concrete visual editors for each setting type (bool, slider, select, multiselect, text, color, bind, etc.).

#### ClickGUI system
- `display/screens/clickgui/MenuScreen`
  - Root GUI screen and interaction router.
  - Maintains open/close animation, forwards input to base components + `windowManager` overlays.
- `CategoryContainerComponent`
  - Lays out category panels horizontally, centered, fixed card sizes.
  - Initializes one `CategoryComponent` per category (except hidden/special cases).
- `CategoryComponent`
  - Renders one category card with header, icon, scissored module list, smooth scroll.
  - Builds `ModuleComponent` list by filtering `ModuleRepository`.
- `ModuleComponent`
  - Renders module row, handles left-click toggle and right-click expand.
  - Hosts dynamic list of per-module setting components.
- `SettingComponent`
  - Minimal action icon component (gear button), used to trigger custom callbacks.

### 13) Strict design rules (must follow)
- Keep **single source of truth**:
  - Module availability only via `ModuleRepository.setup()`.
  - Categories only via `ModuleCategory`.
- Preserve state lifecycle:
  - Always toggle through `Module.setState()/switchState()`.
  - Do not bypass `activate/deactivate` or direct event manager operations in feature code.
- Respect visibility contracts:
  - Setting UI must honor `Setting.isVisible()` / `visible` supplier before render/input.
- Maintain UI routing order:
  - In `MenuScreen`, first try `windowManager`, then component list.
  - Keep this precedence to avoid focus/input conflicts.
- Keep layout constants coherent:
  - Current card geometry relies on `panelWidth=130`, `panelHeight=210`, `gap=8`.
  - If changed, validate clipping/scroll behavior in `CategoryComponent`.
- Do not introduce side effects in render paths:
  - Rendering methods should only draw and compute transient visuals.
  - Mutations should happen in events, ticks, or explicit actions.
- Preserve serialization compatibility:
  - When changing setting/file models, keep backward-compatible load behavior in file managers.

### 14) Example: adding a new tab/category to ClickGUI (safe path)
1. Add enum value in `ModuleCategory`:
   - Example: `UTILITY("Utility")`.
2. Register at least one module in that category in `ModuleRepository.setup()`.
3. Add optional icon/display alias in `CategoryComponent` static maps:
   - `ICONS.put(ModuleCategory.UTILITY, "<fa-icon>")`
   - `DISPLAY_NAMES.put(...)` only if alias differs from readable name.
4. Verify `CategoryContainerComponent.initializeCategoryComponents()` includes it (it auto-iterates enum).
5. Run UI check:
   - panel appears
   - scroll works
   - module row toggles on LMB
   - settings expand on RMB

Minimal code sketch:
```java
// src/main/java/lol/zyrex/features/module/ModuleCategory.java
UTILITY("Utility")
```
```java
// src/main/java/lol/zyrex/features/module/ModuleRepository.java
new SomeUtilityModule(),
```
```java
// src/main/java/lol/zyrex/display/screens/clickgui/components/implement/category/CategoryComponent.java
ICONS.put(ModuleCategory.UTILITY, "\uF0AD");
```

### 15) Quality checklist for new modules/tabs
- Module extends `Module` and uses correct `ModuleCategory`.
- All settings added through existing setting classes and appear via `SettingComponentAdder`.
- No direct packet/render invasive hooks without event/mixin boundary consistency.
- Toggle behavior tested for:
  - keybind toggle
  - GUI click toggle
  - persistence across restart (if module settings are persisted).
