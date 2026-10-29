# Rare reward scaling (Minecraft 1.20.1 Fabric)

A cumulative compatibility module makes previously unscaled rare progression materials and equipment respond to Looting and positive Luck. Rates are read from the server's current loot objects, not a hand-written item-to-percentage map. New rolls use the existing originating-weapon attribution fix.

## Balance

For eligible one-roll probability gates:

`p = min(1, base × (1 + 0.4 × Looting + 0.05 × max(0, Luck)))`

Looting applies to player-attributed entity loot. Chest loot uses the native loot context's Luck and never the held weapon's Looting. Negative Luck receives no new penalty.

| Base | Looting III | Luck 10 | Both |
| --- | --- | --- | --- |
| 10% | 22% | 15% | 27% |
| 1% | 2.2% | 1.5% | 2.7% |

The relative Looting coefficient follows the familiar rare-drop pattern of 2.5% base plus 1 percentage point per level. Actual installed rare drops vary: AdventureZ blackstone heart is 5% + 3 percentage points per level; Graveyard reaper/ghoul use a relative 0.4; Piglin Beast uses 0.5; Orc drops use 0.1–0.125. These native bonuses are preserved, not replaced by the new coefficient. Luck 0.05 is conservative compared with Zenith's rare rarity quality/weight ratio 10/150. The coefficients are policy choices, not claims that vanilla applies this formula universally.

For weighted equipment/material pools, only qualifying rare candidates receive the factor. Pools consisting entirely of equally boosted candidates stay native because scaling every weight cannot improve any drop probability. All candidate weights are normalized together. Example weights 9 common : 1 rare become 9 : 2.2 with Looting III, so one-draw rare probability is 19.64%, not 22%. EMI evaluates the actual constant/uniform draw distribution using `sum(P(N=n) × (1-(1-share)^n))`. It never treats expected quantity or mean draws as a probability. Fixed-point weights are deterministic across both native selection passes; oversized distributions retain original weights. Zero bonus leaves native weights and thresholds exactly unchanged. No extra RNG call or duplicate loot roll is introduced.

## Selection from live data

- Conventional `entities/` and `chests/` tables, fully literal item/empty entries, zero native quality and zero bonus rolls.
- A simple probability gate (random chance or one-trial binomial) at most 20%, or rare candidates with at most 20% of a weighted pool's baseline weight.
- Durable/unstackable equipment, or non-food modded materials referenced by an actual recipe ingredient or an item/id field in the current FTB quest chapters. `reward_quest_items.json` is a reference catalog, not an item-specific rate/bonus table. Recipe references refresh on server data reload; metadata is cached once per reload.
- Original quantities, mandatory predicates and native functions are retained. Existing Looting functions/chance-with-Looting, quality, bonus rolls, guaranteed drops, ordinary unreferenced materials, ambiguous duplicate item/NBT entries, zero-output count functions, nested/unknown conditions and custom functions retain native behavior. Chest predicates which EMI cannot display safely are excluded.
- This is not an override for every custom Java drop event. Piglin barter is not a player loot context and is excluded. Fortune/mining, merchant offers, existing chest inventories and previously generated drops are unchanged.
- Eldritch aberration heart already has a native Looting quantity function behind its 1% gate; it stays native rather than receiving a second bonus. Scavenger's existing extra whole-table roll remains separate and can also roll the new eligible distributions; it is not counted as a flat probability increase.

## EMI and compatibility

The server sends a bounded, schema-validated description: base gate, weights, normalized draw distribution, coefficients and output identifiers. It contains no executable expressions. The client resolves each hovered slot against this current server description and current equipment; default text shows probability and the current Looting/Luck. Shift adds the calculation. Normal slot conditions remain visible. Items grouped into one cycling EMI slot must have the same rate and bonus eligibility. Session clear, data reload, stale-value ambiguity and old-server fallback are handled without forcing an EMI reload. The channel registration callback also covers clients which advertise their channel after initial data sync.

EMI shows the chance for the displayed pool evaluation, conditional on its listed requirements. Scavenger can cause a separate roll, noted under Shift. Original already-scaled drops retain the native display. No client may invent a changed server rate.

## Verification and delivery

Exact Minecraft 1.20.1 / EMI Loot 0.7.9 / EMI 1.1.24 class fingerprints guard the common module; the display is client-only. Setting JVM property `prominence.rewardScaling=false` preserves native behavior. No gameplay damage, attribute values, mining Fortune, configs or world files are changed. The common source is identical in the client and server artifacts.

Build 2026102912 (client nightly) and 2026102902 (server) add this module to their respective latest cumulative artifacts. Native client/server Mixin transformation + ASM verification, native RNG call preservation, production-policy/plan/scope API-double fixtures, math/wire/session/display tests and cumulative updater tests are offline checks. Actual game kills and chest openings remain gameplay verification after both updates. No running installed JAR was replaced and no game/server/world was launched.

Maintainer tools: `verify_rewards_native.py`, `verify_rewards_behavior.py`, and `generate_reward_references.py --quests PATH_TO_quests/chapters --output PATH_TO_reward_quest_items.json`. Private classpath files and raw native audits remain outside Git. Generated native verifier output stays under the build directory.
