# Thirds core — time model

The time model is ~40 lines of pure arithmetic over **local wall-clock
hour and minute**. Every platform reimplements it in its own language;
all reimplementations must agree with the golden vectors in
[`fixtures/vectors.json`](fixtures/vectors.json). The JS implementation
in [`reference/period.js`](reference/period.js) is the reference.

## Model

Period ranges (local wall clock, inclusive start):

| period    | range          | start |
|-----------|----------------|-------|
| `day`     | 09:00 – 16:59  | 09:00 |
| `evening` | 17:00 – 00:59  | 17:00 |
| `night`   | 01:00 – 08:59  | 01:00 |

Given a local time `(h, m)`:

1. Choose the period whose range contains `h` (note `evening` wraps
   midnight: hours 17–23 and 0).
2. `elapsed` = minutes since period start (0–479), with wrap-around for
   the `evening` period.
3. `quarters`      = floor(elapsed / 15)          (0–31)
4. `fullSections`  = floor(quarters / 4)          (0–7; sections to draw fully filled)
5. `section`       = floor(quarters / 4)          (0–7; the pill that may be partial)
6. `fill`          = (quarters mod 4) × 0.25      (0, 0.25, 0.5, 0.75)

Rendering rule: draw sections `0 .. fullSections-1` fully filled,
section `section` filled from its left edge by fraction `fill`, all
remaining sections as empty glass.

Off-quarter inputs (e.g. 12:07) are legal — quantization (step 3) makes
them identical to the preceding quarter hour, so a platform that renders
at an arbitrary moment still agrees with the vectors.

## Fixture format

```json
{
  "format": "thirds-vectors/1",
  "vectors": [
    { "t": "HH:MM", "period": "day|evening|night",
      "quarters": 0-31, "fullSections": 0-7,
      "section": 0-7, "fill": 0 | 0.25 | 0.5 | 0.75 }
  ]
}
```

Vectors were written by hand from the rules above, covering: period
start/end, both ends of every period, the midnight wrap, quarter-step
transitions, and off-grid minutes.

## Testing contract

Each platform's unit tests load `fixtures/vectors.json`, run their
implementation on every vector, and assert all four derived fields
(`quarters`, `fullSections`, `section`, `fill`) plus the period id match.
