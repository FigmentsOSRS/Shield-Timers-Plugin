# Shield Timers

A RuneLite plugin that tracks independent 2-minute activation cooldowns for the three dragonfire/wyvern shields used in consecutive spec workflows.

## Tracked Shields

| Shield | Detection |
|---|---|
| Dragonfire Shield (charged) | Varbit 6539 |
| Dragonfire Ward (charged) | Varbit 6540 |
| Ancient Wyvern Shield | Animation 7700 |

## Features

- Independent countdown InfoBox timers for each shield
- Color coding: white → yellow (≤30s) → red (≤10s)
- Tooltips showing shield name and time remaining
- Per-shield toggles in config
- Timers freeze on logout and resume on login with remaining time intact

## Notes

- Disable the Dragonfire Shield timer in the built-in **Timers and Buffs** plugin to avoid a duplicate DFS timer
- The Ancient Wyvern Shield has no cooldown varbit exposed to the client — its timer is triggered by the player's attack animation (ID 7700) after clicking Operate
