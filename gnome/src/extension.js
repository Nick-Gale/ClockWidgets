// Thirds — GNOME Shell extension (GNOME 50, ESM).
// See ../../docs/spec.md. Renders the current third of the day as 8
// glass pills filling left to right, at the left edge of the panel's
// right box (closest to the centre clock, before the indicators).

import St from 'gi://St';
import Clutter from 'gi://Clutter';
import GLib from 'gi://GLib';
import Gio from 'gi://Gio';
import Shell from 'gi://Shell';

import { Extension } from 'resource:///org/gnome/shell/extensions/extension.js';
import * as Main from 'resource:///org/gnome/shell/ui/main.js';

import { thirdState } from './core.js';

// Ubuntu's top panel is dark even under a light theme; we draw for it.
const APPEARANCE = 'dark';

const SECTIONS = 8;
const PILL_H = 18; // base pill height in device px (panel scale); the rest
                   // of the geometry derives from tokens.json "shape".
const QUARTER_SECONDS = 15 * 60;

class ThirdsBar {
  constructor(tokens) {
    this._tokens = tokens;
    const sh = tokens.shape;
    const pillW = Math.round(PILL_H * sh.aspect);
    this._geo = {
      pillH: PILL_H,
      pillW,
      gap: Math.max(1, Math.round(pillW * sh.gapRatio)),
      radius: Math.round(PILL_H * sh.cornerRatio),
      inset: Math.round(PILL_H * sh.fillInsetRatio),
      fillRadius: Math.round(PILL_H * (1 - 2 * sh.fillInsetRatio) * sh.cornerRatio),
    };
    this.actor = new St.BoxLayout({
      name: 'thirds-bar',
      style: `spacing: ${this._geo.gap}px;`,
      y_align: Clutter.ActorAlign.CENTER,
      reactive: false,
      can_focus: false,
    });

    this._pills = [];
    for (let i = 0; i < SECTIONS; i++) {
      // BoxLayout pins the single fill child to the pill's left edge.
      const glass = new St.BoxLayout({
        style: this._glassStyle(),
      });
      this._setGlassSize(glass);
      const fill = new St.Widget({
        y_align: Clutter.ActorAlign.CENTER,
        style: 'width: 0px; min-width: 0px;',
      });
      glass.add_child(fill);
      this.actor.add_child(glass);
      this._pills.push({ glass, fill });
    }
  }

  _glassStyle() {
    const g = this._tokens.glass[APPEARANCE];
    const { radius } = this._geo;
    // A soft top-lit vertical gradient gives the empty glass depth.
    return `background-color: ${g.body}; border-color: ${g.hairline}; border-width: 1px; border-radius: ${radius}px;` +
      ' background-gradient-direction: vertical;' +
      ' background-gradient-start: rgba(255,255,255,0.10);' +
      ' background-gradient-end: rgba(255,255,255,0.02);';
  }

  _setGlassSize(w) {
    const { pillW, pillH } = this._geo;
    w.style += ` width: ${pillW}px; min-width: ${pillW}px; height: ${pillH}px; min-height: ${pillH}px;`;
  }

  render(state) {
    const pal = this._tokens.periods[state.period][APPEARANCE];
    for (let i = 0; i < SECTIONS; i++) {
      const { fill } = this._pills[i];
      const frac = Math.max(0, Math.min(1, (state.fullSections - i) + (i === state.section ? state.fill : 0)));
      if (frac <= 0) {
        fill.style = 'width: 0px; min-width: 0px;';
        continue;
      }
      // The fill is the box shape itself, growing left→right: it fills the
      // glass content area (inside the 1px hairline rim) with a soft
      // vertical sheen. A full section reads as a filled glass cell.
      const { pillW, pillH, radius, fillRadius } = this._geo;
      const innerMax = pillW - 2; // St content box: minus the 1px border
      const w = Math.max(radius, Math.round(innerMax * frac));
      fill.style =
        `background-gradient-direction: vertical;` +
        ` background-gradient-start: ${pal.sheen};` +
        ` background-gradient-end: ${pal.fill};` +
        ` border-radius: ${Math.min(fillRadius, Math.floor(w / 2))}px;` +
        ` width: ${w}px; min-width: ${w}px;` +
        ` height: ${pillH - 2}px; min-height: ${pillH - 2}px;`;
    }
  }

  destroy() {
    this.actor.destroy();
  }
}

export default class ThirdsExtension extends Extension {
  enable() {
    this._tokens = this._loadTokens();
    this._bar = new ThirdsBar(this._tokens);
    Main.panel._rightBox.insert_child_at_index(this._bar.actor, 0);
    this._lastKey = null;
    this._source = null;
    this._tick();
    this._scheduleTick();
    this._maybeDebugShot();
  }

  // Dev affordance: with THIRDS_DEBUG_SHOT=<png-path>, capture the stage
  // from inside the shell once after unlock. Used by test-headless.sh to
  // verify rendering without a real display session.
  async _maybeDebugShot() {
    const shotPath = GLib.getenv('THIRDS_DEBUG_SHOT');
    if (!shotPath) return;
    try {
      if (Main.screenShield?.setLocked)
        Main.screenShield.setLocked(false);
      else if (Main.screenShield?.deactivate)
        Main.screenShield.deactivate();
      await new Promise(r => GLib.timeout_add(GLib.PRIORITY_DEFAULT, 3000, () => { r(); return GLib.SOURCE_REMOVE; }));
      Gio._promisify(Shell.Screenshot.prototype, 'screenshot');
      const shooter = new Shell.Screenshot();
      const stream = Gio.MemoryOutputStream.new_resizable();
      await shooter.screenshot(false, stream);
      stream.close(null);
      const out = Gio.File.new_for_path(shotPath)
        .replace(null, false, Gio.FileCreateFlags.NONE, null);
      out.write_bytes(stream.steal_as_bytes(), null);
      out.close(null);
      console.log(`THIRDS-DEBUG shot written: ${shotPath}`);
    } catch (e) {
      console.log(`THIRDS-DEBUG shot failed: ${e}`);
    }
  }

  disable() {
    if (this._source !== null) {
      GLib.source_remove(this._source);
      this._source = null;
    }
    if (this._bar) {
      this._bar.destroy();
      this._bar = null;
    }
    this._tokens = null;
    this._lastKey = null;
  }

  _loadTokens() {
    const path = this.dir.get_child('tokens.json').get_path();
    const [, bytes] = GLib.file_get_contents(path);
    return JSON.parse(new TextDecoder().decode(bytes));
  }

  _tick() {
    const now = GLib.DateTime.new_now_local();
    const state = thirdState(now.get_hour(), now.get_minute());
    const key = `${state.period}:${state.quarters}`;
    if (key !== this._lastKey) {
      this._bar.render(state);
      this._lastKey = key;
    }
  }

  // One-shot timer to the next wall-clock :00/:15/:30/:45.
  _scheduleTick() {
    const now = GLib.DateTime.new_now_local();
    const secOfDay = now.get_hour() * 3600 + now.get_minute() * 60 + now.get_second();
    const wait = QUARTER_SECONDS - (secOfDay % QUARTER_SECONDS);
    this._source = GLib.timeout_add_seconds(GLib.PRIORITY_DEFAULT, wait, () => {
      this._source = null;
      this._tick();
      this._scheduleTick();
      return GLib.SOURCE_REMOVE;
    });
  }
}
