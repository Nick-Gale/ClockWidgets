import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { thirdState } from "../reference/period.js";

const { format, vectors } = JSON.parse(
  await readFile(new URL("../fixtures/vectors.json", import.meta.url), "utf8")
);

test("fixture format is supported", () => {
  assert.equal(format, "thirds-vectors/1");
  assert.ok(vectors.length >= 20, "expected a meaningful number of vectors");
});

for (const v of vectors) {
  test(`t=${v.t} -> ${v.period} q=${v.quarters} full=${v.fullSections} sec=${v.section} fill=${v.fill}`, () => {
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
