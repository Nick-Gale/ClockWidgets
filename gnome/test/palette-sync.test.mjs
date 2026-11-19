import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const canonical = JSON.parse(
  await readFile(new URL("../../palette/tokens.json", import.meta.url), "utf8")
);
const bundled = JSON.parse(
  await readFile(new URL("../src/tokens.json", import.meta.url), "utf8")
);

test("gnome/src/tokens.json is in sync with palette/tokens.json", () => {
  assert.deepStrictEqual(bundled, canonical);
});
