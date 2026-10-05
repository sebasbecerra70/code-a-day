import { test } from "node:test";
import assert from "node:assert/strict";
import { parseCron, nextRun, nextRuns, matches, CronParseError } from "./solution.ts";

const iso = (d: Date | null) => d?.toISOString().slice(0, 16);
const at = (s: string) => new Date(s + ":00Z");

test("parses lists, ranges, steps, and names", () => {
  const s = parseCron("*/15 9-17/4 1,15 JAN-MAR mon-fri");
  assert.deepEqual(s.minutes, [0, 15, 30, 45]);
  assert.deepEqual(s.hours, [9, 13, 17]);
  assert.deepEqual(s.daysOfMonth, [1, 15]);
  assert.deepEqual(s.months, [1, 2, 3]);
  assert.deepEqual(s.daysOfWeek, [1, 2, 3, 4, 5]);
  assert.deepEqual(parseCron("5/20 * * * *").minutes, [5, 25, 45]);
});

test("7 is Sunday, duplicates collapse, macros expand", () => {
  assert.deepEqual(parseCron("0 0 * * 0,7").daysOfWeek, [0]);
  assert.deepEqual(parseCron("1,1,2 * * * *").minutes, [1, 2]);
  assert.deepEqual(parseCron("@daily"), parseCron("0 0 * * *"));
  assert.deepEqual(parseCron("@hourly").minutes, [0]);
});

test("rejects malformed expressions", () => {
  for (const bad of ["* * * *", "60 * * * *", "* 24 * * *", "* * 0 * *", "* * * 13 *", "*/0 * * * *", "5-1 * * * *", "a * * * *", "1-2-3 * * * *", ", * * * *", "* * * * 8"]) {
    assert.throws(() => parseCron(bad), CronParseError, bad);
  }
});

test("next run: simple cases and strictly-after semantics", () => {
  assert.equal(iso(nextRun("*/15 * * * *", at("2026-03-01T10:07"))), "2026-03-01T10:15");
  assert.equal(iso(nextRun("*/15 * * * *", at("2026-03-01T10:15"))), "2026-03-01T10:30");
  assert.equal(iso(nextRun("0 9 * * *", at("2026-03-01T09:00"))), "2026-03-02T09:00");
  assert.equal(iso(nextRun("30 * * * *", new Date("2026-03-01T10:29:59.999Z"))), "2026-03-01T10:30");
});

test("next run rolls over hours, days, months, and years", () => {
  assert.equal(iso(nextRun("@yearly", at("2026-06-15T12:00"))), "2027-01-01T00:00");
  assert.equal(iso(nextRun("0 0 31 * *", at("2026-04-01T00:00"))), "2026-05-31T00:00"); // April has 30 days
  assert.equal(iso(nextRun("0 12 29 2 *", at("2026-01-01T00:00"))), "2028-02-29T12:00"); // next leap day
});

test("day-of-month OR day-of-week when both restricted", () => {
  // The 13th of the month OR any Friday. 2026-03-06 is a Friday.
  assert.deepEqual(nextRuns("0 0 13 * 5", at("2026-03-05T12:00"), 3).map(iso), [
    "2026-03-06T00:00",
    "2026-03-13T00:00",
    "2026-03-20T00:00",
  ]);
  // Only DOW restricted: weekdays at 09:00. 2026-03-07 is Saturday.
  assert.deepEqual(nextRuns("0 9 * * MON-FRI", at("2026-03-06T10:00"), 2).map(iso), ["2026-03-09T09:00", "2026-03-10T09:00"]);
});

test("impossible schedule returns null", () => {
  assert.equal(nextRun("0 0 30 2 *", at("2026-01-01T00:00")), null);
});

test("matches()", () => {
  assert.equal(matches("*/5 9-17 * * 1-5", at("2026-03-09T09:35")), true);
  assert.equal(matches("*/5 9-17 * * 1-5", at("2026-03-08T09:35")), false); // Sunday
});

test("randomized: nextRun agrees with minute-by-minute brute force", () => {
  let seed = 3;
  const rand = (n: number) => ((seed = (seed * 1103515245 + 12345) % 2 ** 31) % n);
  const pick = (opts: string[]) => opts[rand(opts.length)];
  for (let t = 0; t < 150; t++) {
    const expr = [
      pick(["*", "0", "*/7", "15,45", "10-20/5"]),
      pick(["*", "3", "*/6", "22-23"]),
      pick(["*", "1", "15,28", "*/10"]),
      pick(["*", "2", "*/3", "6-8"]),
      pick(["*", "0", "1-5", "SAT"]),
    ].join(" ");
    const start = new Date(Date.UTC(2026, rand(12), 1 + rand(28), rand(24), rand(60)));
    const got = nextRun(expr, start);
    let brute = new Date(start.getTime() + 60_000);
    while (!matches(expr, brute)) brute = new Date(brute.getTime() + 60_000);
    assert.equal(iso(got), iso(brute), `${expr} after ${start.toISOString()}`);
  }
});
