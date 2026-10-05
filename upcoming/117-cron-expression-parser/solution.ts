// Parses standard 5-field cron expressions ("minute hour day-of-month month
// day-of-week") into sets of allowed values, and computes upcoming run times.
// Supports *, lists (1,5), ranges (1-5), steps (*/15, 10-50/10), names
// (JAN, MON), 7 as Sunday, and macros like @daily. Times are in UTC.

export interface CronSchedule {
  minutes: number[];
  hours: number[];
  daysOfMonth: number[];
  months: number[];
  daysOfWeek: number[];
  /** True when the day-of-month field was "*" (affects the DOM/DOW OR rule). */
  domStar: boolean;
  dowStar: boolean;
}

interface FieldSpec {
  name: string;
  min: number;
  max: number;
  names?: string[]; // index + min = value
}

const FIELDS: FieldSpec[] = [
  { name: "minute", min: 0, max: 59 },
  { name: "hour", min: 0, max: 23 },
  { name: "day-of-month", min: 1, max: 31 },
  { name: "month", min: 1, max: 12, names: ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"] },
  { name: "day-of-week", min: 0, max: 7, names: ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"] },
];

const MACROS: Record<string, string> = {
  "@yearly": "0 0 1 1 *",
  "@annually": "0 0 1 1 *",
  "@monthly": "0 0 1 * *",
  "@weekly": "0 0 * * 0",
  "@daily": "0 0 * * *",
  "@midnight": "0 0 * * *",
  "@hourly": "0 * * * *",
};

export class CronParseError extends Error {}

function parseValue(token: string, f: FieldSpec): number {
  const idx = f.names?.indexOf(token.toUpperCase()) ?? -1;
  if (idx >= 0) return idx + f.min;
  if (!/^\d+$/.test(token)) throw new CronParseError(`invalid ${f.name} value "${token}"`);
  const n = Number(token);
  if (n < f.min || n > f.max) throw new CronParseError(`${f.name} value ${n} out of range ${f.min}-${f.max}`);
  return n;
}

function parseField(field: string, f: FieldSpec): number[] {
  const values = new Set<number>();
  for (const part of field.split(",")) {
    const [rangePart, stepPart, ...extra] = part.split("/");
    if (extra.length || rangePart === "") throw new CronParseError(`invalid ${f.name} "${part}"`);
    let step = 1;
    if (stepPart !== undefined) {
      if (!/^\d+$/.test(stepPart) || Number(stepPart) === 0) throw new CronParseError(`invalid step "${stepPart}"`);
      step = Number(stepPart);
    }
    let lo: number, hi: number;
    if (rangePart === "*") [lo, hi] = [f.min, f.max];
    else if (rangePart.includes("-")) {
      const [a, b, ...more] = rangePart.split("-");
      if (more.length) throw new CronParseError(`invalid range "${rangePart}"`);
      [lo, hi] = [parseValue(a, f), parseValue(b, f)];
      if (lo > hi) throw new CronParseError(`reversed range "${rangePart}"`);
    } else {
      lo = parseValue(rangePart, f);
      hi = stepPart !== undefined ? f.max : lo; // "5/15" means 5,20,35,50
    }
    for (let v = lo; v <= hi; v += step) values.add(v);
  }
  return [...values].sort((a, b) => a - b);
}

export function parseCron(expr: string): CronSchedule {
  const src = MACROS[expr.trim().toLowerCase()] ?? expr;
  const parts = src.trim().split(/\s+/);
  if (parts.length !== 5) throw new CronParseError(`expected 5 fields, got ${parts.length}`);
  const [minutes, hours, daysOfMonth, months, dowRaw] = parts.map((p, i) => parseField(p, FIELDS[i]));
  const daysOfWeek = [...new Set(dowRaw.map((d) => d % 7))].sort((a, b) => a - b); // 7 == Sunday
  return { minutes, hours, daysOfMonth, months, daysOfWeek, domStar: parts[2] === "*", dowStar: parts[4] === "*" };
}

/**
 * Vixie-cron day rule: if both day fields are restricted, a day matches when
 * EITHER matches; otherwise both must (the "*" one trivially does).
 */
function dayMatches(s: CronSchedule, d: Date): boolean {
  const dom = s.daysOfMonth.includes(d.getUTCDate());
  const dow = s.daysOfWeek.includes(d.getUTCDay());
  return !s.domStar && !s.dowStar ? dom || dow : dom && dow;
}

/** First matching time strictly after `after` (to the minute), or null if none within ~5 years. */
export function nextRun(schedule: CronSchedule | string, after: Date): Date | null {
  const s = typeof schedule === "string" ? parseCron(schedule) : schedule;
  const t = new Date(after.getTime());
  t.setUTCSeconds(0, 0);
  t.setUTCMinutes(t.getUTCMinutes() + 1);
  const limit = after.getTime() + 5 * 366 * 24 * 3600 * 1000;

  // Jump field by field instead of minute by minute: skip whole months/days/hours that can't match.
  while (t.getTime() <= limit) {
    if (!s.months.includes(t.getUTCMonth() + 1)) {
      t.setUTCMonth(t.getUTCMonth() + 1, 1);
      t.setUTCHours(0, 0);
      continue;
    }
    if (!dayMatches(s, t)) {
      t.setUTCDate(t.getUTCDate() + 1);
      t.setUTCHours(0, 0);
      continue;
    }
    if (!s.hours.includes(t.getUTCHours())) {
      t.setUTCHours(t.getUTCHours() + 1, 0);
      continue;
    }
    const m = s.minutes.find((x) => x >= t.getUTCMinutes());
    if (m === undefined) {
      t.setUTCHours(t.getUTCHours() + 1, 0);
      continue;
    }
    t.setUTCMinutes(m);
    return t;
  }
  return null; // e.g. "0 0 30 2 *" never fires
}

export function nextRuns(expr: string, after: Date, count: number): Date[] {
  const s = parseCron(expr);
  const out: Date[] = [];
  let cur: Date | null = after;
  while (out.length < count && (cur = nextRun(s, cur))) out.push(cur);
  return out;
}

/** Does the schedule fire at this exact minute? */
export function matches(expr: string, at: Date): boolean {
  const s = parseCron(expr);
  return (
    s.minutes.includes(at.getUTCMinutes()) &&
    s.hours.includes(at.getUTCHours()) &&
    s.months.includes(at.getUTCMonth() + 1) &&
    dayMatches(s, at)
  );
}
