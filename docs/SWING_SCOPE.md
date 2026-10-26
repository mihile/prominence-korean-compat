# Vanilla arm animations and combat powers

The installed Simply Swords 1.70.2 build calls `SimplySwordsAPI.onWeaponSwing` from both its LivingEntity arm-animation callback and its Better Combat server attack handler. Arm animations are also sent for block interaction and mining. Livyatan's wave, Molten Edge, Wraithmaw, Gloampiercer, Soulstalker, Riftmane and runic/gem swing powers all share that API. Previously the client use-key fence only guarded item-use activation; it did not cover this animation route.

The common module marks the original LivingEntity swing method with a nested, finally-cleared thread-local scope. The API ignores server-player animation dispatch when the exact supported Better Combat attack hook is present. Authoritative attack dispatch outside that scope, ordinary animations, mob powers and right-click abilities are retained. Missing native versions/hashes disable the mixins. A missing transformed attack hook falls back to original behavior rather than disabling combat powers. The client use-key fence additionally consumes a press while block breaking is active.

No packets, cooldowns, damage coefficients, mining rules or container handling are changed. Multiplayer needs the server build too; the client common module covers integrated single player. No protocol dependency or new network channel is added.

Verification applies the complete installed Simply Swords LivingEntity and Better Combat mixins, followed by this module, to real Minecraft LivingEntity, Better Combat ServerNetwork and SimplySwordsAPI bytecode. It checks scope cleanup, the unchanged real-attack hook and the API guard before powers. ASM verifies every transformed method. This is CPU-only validation, not a live game test.

The existing exact-line quest review overlay includes the revised missive, prologue and Obsidilith description, including official English and current Korean variants. Starting with build2026102608 the related language-key corrections also ship inside the JAR, above the old Paxi language data; see TRANSLATION_UPDATES.md. The JAR updater still does not install external resource packs or quest source files.
