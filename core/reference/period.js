/**
 * Reference implementation of the Thirds time model.
 * See ../spec.md. Pure arithmetic over local wall-clock hour/minute.
 */

const PERIODS = [
  { id: "day", start: 9 },
  { id: "evening", start: 17 }, // wraps midnight: hours 17..23 and 0
  { id: "night", start: 1 },
];

/**
 * @param {number} hour 0-23 local wall clock
 * @param {number} minute 0-59 local wall clock
 * @returns {{period: string, quarters: number, fullSections: number, section: number, fill: number}}
 */
export function thirdState(hour, minute) {
  let period;
  if (hour >= 9 && hour < 17) period = "day";
  else if (hour >= 17 || hour < 1) period = "evening";
  else period = "night";

  const start = PERIODS.find((p) => p.id === period).start;
  const elapsed = ((hour - start + 24) % 24) * 60 + minute; // 0..479

  const quarters = Math.floor(elapsed / 15); // 0..31
  const fullSections = Math.floor(quarters / 4);
  const section = Math.min(fullSections, 7);
  const fill = (quarters % 4) * 0.25;

  return { period, quarters, fullSections, section, fill };
}
