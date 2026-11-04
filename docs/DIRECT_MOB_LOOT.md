# Direct mob rewards in EMI Loot

Some installed mods generate items in entity code instead of loot tables. Version and SHA-256 guarded display entries cover the Wither shard and void lord soul, Ender Dragon purple lord soul, Faceless X'al, AdventureZ The Eye prime eye, Cornelia's seasonal weapons, and conditional charged-Creeper head drops for the Shattered, skeleton creeper and Enderman. The source drops are not modified. Direct reward conditions and reviewed probabilities are shown; each source mod has an independent guard.

The installed jars and nested jars were scanned for direct item-giving and entity spawning patterns, and the found death-related cases were reviewed. This static scan is not proof of full coverage of arbitrary mod callbacks or datapack/config changes. In particular the BOMD soul-star milestone uses a server-configured list and player kill counter; it is not modelled as a random per-mob chance. Equipped container/saddle contents and interaction-generated items are not presented as guaranteed death drops.

The static chance builder rejects zero-output counts, explosion decay and unreviewed output-transform functions. Existing reviewed rows are preserved; 219 previously unsafe or ambiguous observations are now left unknown. Runtime server metadata remains authoritative when available. Unsupported conditions or changed source values must not be stated as verified probabilities.

Enhanced Boss Bars translates the vanilla Wither and Ender Dragon names into oversized private-use glyphs. EMI-only names use ordinary language strings when such glyphs are detected; the original names and resource pack remain unchanged elsewhere.

AAAParticles logged immediate loading and unloading of the Faceless shadowsurge during the reported fight. When its memory-pressure collector is running, the compatibility code keeps the recently loaded surge effect for eight seconds; explicit resource reload remains unaffected. The server additionally binds the effect before networking. OpenGL errors occurred at the same times, so rendering is not considered verified without a real fight.

Native Mixin transforms, guarded source hashes, direct-drop fixtures, zero-count/decay rejection, existing loot UI cases, effect-lifetime scope and language preservation were checked offline. Actual UI and particle rendering in game remain pending.
