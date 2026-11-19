/**
 * Thirds time model — GNOME reimplementation.
 * Pure local wall-clock arithmetic; must match core/spec.md and pass
 * core/fixtures/vectors.json (see gnome/test/core.test.mjs).
 * This file must stay free of any GJS/shell imports so node can test it.
 */

export function thirdState(hour, minute) {
  let period;
  if (hour >= 9 && hour < 17) period = "day";
  else if (hour >= 17 || hour < 1) period = "evening";
  else period = "night";

  const starts = { day: 9, evening: 17, night: 1 };
  const elapsed = ((hour - starts[period] + 24) % 24) * 60 + minute; // 0..479

  const quarters = Math.floor(elapsed / 15); // 0..31
  const fullSections = Math.floor(quarters / 4);
  const section = Math.min(fullSections, 7);
  const fill = (quarters % 4) * 0.25;

  return { period, quarters, fullSections, section, fill };
}
