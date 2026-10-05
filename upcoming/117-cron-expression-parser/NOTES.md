# Cron expression parser (next run times)

**Problem:** Parse standard 5-field cron expressions (`minute hour day-of-month month day-of-week`) with lists, ranges, steps, names, and `@macros`, validate them, and compute the next N times a schedule fires.

## Approach
- **Parse** each field into a sorted array of allowed values. Split on `,` into parts. Each part is `range[/step]`, where range is `*`, `a-b`, or a single value (`5/15` means "from 5 to max, every 15"). Names (`JAN`, `mon`) map to numbers. Day-of-week `7` folds into `0` (Sunday).
- Validation is strict: field count, bounds per field, zero steps, reversed ranges, and junk tokens all throw `CronParseError`.
- **Day rule (Vixie cron):** if *both* day-of-month and day-of-week are restricted, a day matches when *either* does. Otherwise both must match. This is the most commonly missed cron subtlety.
- **Next run:** start at the next whole minute and check fields from the most significant down. If the month doesn't match, jump to the 1st of next month. If the day fails, jump to the next midnight. If the hour fails, jump to the next hour. Otherwise take the first allowed minute ≥ now, or roll to the next hour. This skips impossible ranges in large steps instead of scanning minutes.
- A 5-year search horizon returns `null` for impossible schedules like `0 0 30 2 *`.
- A randomized test cross-checks `nextRun` against a minute-by-minute brute force.

## Complexity
| Aspect | Cost |
|--------|------|
| Parse | O(field ranges), at most 60+24+31+12+7 values |
| nextRun | O(days scanned · log) worst case, typically a few iterations |
| Space | O(1) (bounded value sets) |

## Interview talking points
- Time zones and DST are the hard part in production. A 02:30 job may run zero or two times on transition days. This version uses UTC to sidestep that, and real schedulers convert per zone with explicit DST policies.
- Bitmasks (a 64-bit minute mask, etc.) make matching O(1) and are how many schedulers store fields.
- Extensions: seconds field (Quartz), `L` (last day), `W` (nearest weekday), `#` (nth weekday), `?` placeholder.
- Missed runs (machine asleep): skip, run once, or catch up all? That's the anacron vs cron trade-off.
- Distributed cron needs leader election or locking so a job runs once across replicas.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
