# AbySm modules -> Henshin port

## What was added

### Compatibility layer (`com/abysm.client.game`) - Kotlin
Recreates the AbySm API surface inside Henshin so ported modules keep AbySm's look:
- `Module.kt`        - base class, keeps AbySm's `by floatValue(...)` delegate API, backed by Henshin values/bridge.
- `ModuleCategory.kt`- Combat/Motion/Visual/Misc/World mapped to Henshin GUI categories.
- `InterceptablePacket.kt` - stub; Henshin has no packet relay, so packet hooks never fire (see "Limits").
- `LocalPlayer.kt`   - local player state + actions via the native bridge.
- `Player.kt`        - nearby entity wrapper (parsed from the bridge's flat float array).
- `Level.kt`         - `Level.players(range)` / `mobs` / `items`.

### Ported modules (`com.abysm.client.game.module.*`) - Kotlin
Motion: Sprint, Speed, Bhop, HighJump, AirJump, MotionFly, NoClip
Misc:  AutoDisconnect

All are registered in `ModuleManager` and appear in the Henshin GUI like any other module.

## Important limits (read this)
AbySm is a packet-proxy client: its modules intercept real Bedrock protocol packets
(CloudburstMC) between game and server, and use a full world model (kiwi/Level/registries).
Henshin is a native-bridge launcher with NO relay, so:
- Anything copied that references `org.cloudburstmc.*` packets cannot compile or run here.
- `InterceptablePacket` exists so code compiles, but nothing feeds it: packet-only modules
  (CrystalAura, AutoTotem, ChestStealer, Timer, FakeDeath, FakeXP, NoChat, Spammer, Replay,
  PopCounter, EnemyHunter, CommandHandler, Miner...) are NOT ported because they cannot work.
- The native bridge only exposes: player pos/rotation, nearby entities (id/type/pos/dist/health),
  motion, attack, hotbar, use item, swing, rotation, hitbox, knockback, block place, ESP(items),
  desync, hunger. Modules above use only those calls.

## Already existed in Henshin (AbySm equivalents)
Fly=Fly, JetPack=Jetpack, Spider=Spider, AntiKnockback=Velocity, Hitbox=Hitbox,
Surround=Surround, AutoWalk=AutoWalk, AntiAFK=AntiAFK, WaterMark=Watermark, Aura/KillAura.
