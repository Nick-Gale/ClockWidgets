import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { thirdState } from "../src/core.js";

const { vectors } = JSON.parse(
  await readFile(new URL("../../core/fixtures/vectors.json", import.meta.url), "utf8")
);

for (const v of vectors) {
  test(`gnome model: t=${v.t}`, () => {
    const [h, m] = v.t.split(":").map(Number);
    assert.deepEqual(thirdState(h, m), {
      period: v.period,
      quarters: v.quarters,
      fullSections: v.fullSections,
      section: v.section,
      fill: v.fill,
    });
  });
}
