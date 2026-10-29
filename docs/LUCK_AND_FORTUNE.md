# Luck, Fortune and Looting terminology

Reviewed against the installed Minecraft1.20.1 Fabric classes, AdventureZ1.4.20, Puffish Attributes0.8.2, Tierify1.2.0 and the current language/quest sources. Names change; mechanics do not.

| Source | Korean | Actual role |
| --- | --- | --- |
| minecraft:generic.luck | 운 | Context Luck, used by participating loot distributions, reforging and other luck-aware mechanics |
| minecraft:fortune enchantment | 행운 | Block-drop bonus enchantment |
| puffish_attributes:fortune | 채굴 행운 | Modifies the effective Fortune enchantment level; player.fortune is its registry alias |
| minecraft:looting enchantment | 약탈 | Mob-drop count/chance bonuses only where the loot implementation consumes Looting |

AdventureZ's LootInit injects one Binomial(1,0.1) draw for the gilded blackstone shard in piglin_brute, and Binomial(1,0.01) in bartering/bastion tables. The Binomial provider uses only n, p and the context RNG. The injected entry has no looting function/condition, default quality0 and bonus_rolls0. Thus this draw receives neither Looting nor Luck scaling. Current client and server AdventureZ JARs are byte-identical. This is the base draw, not a claim about separately triggered additional loot mechanisms.

Vanilla weighted entries use max(floor(weight + quality*Luck),0), and pools add floor(bonus_rolls*Luck) to their base draw count. Therefore Luck does not universally increase all drop percentages. Tierify ModifierUtils independently uses player.getLuck() with the configured luckReforgeModifier (currently0.02) to reduce common-quality weights. Puffish's ApplyBonusLootFunctionMixin instead adjusts only Fortune's effective level, then probabilistically rounds a fractional level; it does not consume generic Luck. These are different calculations.

Vinery LuckEffect and Herbal Brews FortuneEffect both modify the vanilla Luck attribute, despite the latter's English name. Simply Skills luck+0.25 rewards generic.luck. Those descriptions, related status/potion labels, Souls Weapons' Luck tooltip and the corresponding glossary use 운. The Emerald backpack and reforging FTB passages are corrected through existing English and Korean exact-string display rules, without editing server quests or progress. Zenith's Luck/recipe book entries use the same terminology. Bumblezone's mining-crystal description now explicitly says 행운 마법부여 where its loot table uses minecraft:fortune. Narrative good-luck wishes and proper ingredient/item names remain intact.

Validation:29 language-key corrections, two FTB passages, two book entries,15 source-pack files changed;10,984 other entries remain byte-identical. All580 production classes and the updater helper are identical to nightly-20261029.10. Native language creation/lookup confirms the final overrides win over older pack wording. Cumulative checks cover Luck versus Fortune, old Korean/English quest aliases, normal enchantment names, narrative wishes and the lucky-foot ingredient name. Actual in-game screen review remains pending. Client-only nightly publication; server files, settings and loot math are unchanged.
