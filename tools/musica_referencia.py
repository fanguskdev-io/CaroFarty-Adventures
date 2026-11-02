#!/usr/bin/env python3
"""The music of the prototype, driven the way the Kotlin port drives it in real time (AUD-26).

    python3 tools/musica_referencia.py                     # the fingerprints of PrototypeFidelityTest
    python3 tools/musica_referencia.py --salida /tmp/ref   # and, for each scenario, the notes (JSON) and the music
                                                           # (raw little-endian float64 at 32 kHz, before the master gain)

It imports the approved prototype (../backlog-juego-plataformas/diseno/musica: generador.py and sinte.py) without
touching it, and plays the demo run of its maquetas.py (seed 7) with the rules of MusicEngine:
- a bar is decided 512 samples before it starts, with the controls given so far (the levels are given 512 samples
  early so the bars are the prototype's);
- a section is composed when its first bar is decided, always with the humanization of its 8 bars;
- the next section is chosen when one ends, avoiding A at level 3 (the prototype knows when the melody will come in;
  a run does not); the walking bass of the Nieve peeks at that choice;
- the notes of a section cut short are held until the cut, as the voices do it.
If MusicEngine changes these rules, this has to change with it.
"""
import argparse
import json
import random
import struct
import sys
from pathlib import Path

sys.dont_write_bytecode = True       # nothing is written into the prototype's folder


def prototype_folder():
    """The prototype, beside the project (or beside any folder above it, for a worktree inside the project)."""
    for folder in Path(__file__).resolve().parents:
        candidate = folder / "backlog-juego-plataformas" / "diseno" / "musica"
        if (candidate / "generador.py").exists():
            return candidate
    sys.exit("the prototype (../backlog-juego-plataformas/diseno/musica) is not there")


sys.path.insert(0, str(prototype_folder()))
import generador as g  # noqa: E402
import sinte as s  # noqa: E402

FS = s.FS
LOOKAHEAD = 512
FADE = s.ms(200)
GATE = g.Compositor.GATE
TRANSITIONS = {'A': [('A2', 0.5), ('B', 0.5)], 'A2': [('B', 0.6), ('P', 0.4)],
               'B': [('A', 0.4), ('A2', 0.3), ('P', 0.3)], 'P': [('A', 0.7), ('B', 0.3)], 'T': [('T', 1.0)]}
LAYER_LEVEL = {'C0': 1, 'C1': 2, 'C2': 3, 'C3': 4}


class Run:
    def __init__(self, esc, seed):
        self.esc, self.P = esc, esc.pasos
        self.comp = esc.compositor(esc, seed)
        self.plan = random.Random(seed * 101)
        self.bank = s.banco()
        self.requested, self.level, self.lower = 1, 1, 0
        self.warning = self.end_at = None
        self.stopped = False
        self.kind, self.bars, self.semi, self.section = None, 0, 0.0, 0
        self.next_bar = 0.0
        self.last_start, self.last_semi, self.last_level = -1.0, -1.0, -1
        self.at_top = 0
        self.notes, self.sections = [], []
        self.written, self.cursor = None, None
        self.layer_on = {}
        self.automation = {c: [] for c in range(6)}

    def next_beat(self, time):
        if self.last_start >= 0:
            for k in range(self.esc.tiempos):
                t = self.last_start + self.esc.por_tiempo * k * self.last_semi
                if t >= time:
                    return t
        return max(time, self.next_bar)

    def decide(self, now):
        while not self.stopped and now >= int(self.next_bar) - LOOKAHEAD:
            if self.end_at is not None and self.next_bar >= self.end_at + FADE:
                self.stopped = True
                return
            self.open_bar(self.next_bar, now)

    def open_bar(self, start, now):
        if self.requested < self.level:
            self.lower += 1
            if self.lower >= 2:
                self.level, self.lower = self.requested, 0
        else:
            self.level, self.lower = self.requested, 0
        enters = self.last_level >= 0 and self.level >= 4 > self.last_level
        tension = self.warning is not None and start >= self.warning
        kind = None
        if self.kind is None:
            kind = 'A'
        elif tension and self.kind != 'T':
            kind = 'T'
        elif not tension and self.kind == 'T':
            kind = 'A'
        elif enters:
            kind = 'A'
        elif self.bars == 8:
            kind = self.choose_next(self.kind, self.plan)
        if kind:
            self.start_section(kind, start, now)
        self.at_top = self.at_top + 1 if self.level >= 5 else 0
        if tension:
            intensity = 0.95
        elif self.level >= 5:
            intensity = g.INTENSIDAD_CICLO[(16 + self.at_top - 1) % len(g.INTENSIDAD_CICLO)]
        else:
            intensity = 0.88 + 0.03 * (self.level - 1)
        self.schedule_bar(self.bars, start, self.semi, intensity)
        self.layers(tension, start, self.semi)
        self.bars += 1
        self.last_start, self.last_semi, self.last_level = start, self.semi, self.level
        self.next_bar = start + self.P * self.semi

    def start_section(self, kind, start, now):
        if self.kind is not None and self.bars < 8:
            self.cut(self.bars * self.P, now)
        if kind == 'T':
            bpm = self.esc.bpm if self.last_semi < 0 else FS * 60.0 / (self.esc.por_tiempo * self.last_semi)
        else:
            bpm = self.esc.bpm * (1 + min(0.12, 0.03 * (self.level - 1)))
        self.semi = FS * 60.0 / (bpm * self.esc.por_tiempo)
        self.section += 1
        self.kind, self.bars = kind, 0
        peek = random.Random()
        peek.setstate(self.plan.getstate())
        heading = self.esc.inicio.get(self.choose_next(kind, peek), self.esc.inicio['A'])
        self.written = self.compose(kind, heading)
        self.cursor = {c: 0 for c in range(6)}
        self.sections.append((kind, start, bpm))
        self.comp.mutar()

    def choose_next(self, kind, rng):
        options = [(k, w) for k, w in TRANSITIONS[kind] if k != kind or kind == 'T']
        avoid = self.level == 3 and len(options) > 1
        allowed = [(k, w) for k, w in options if not (avoid and k == 'A')]
        total = 0.0
        for _, w in allowed:
            total += w
        r = rng.uniform(0, total)
        last = kind
        for k, w in allowed:
            last = k
            r -= w
            if r <= 0:
                return k
        return last

    def compose(self, kind, heading):
        comp, esc, rng, P = self.comp, self.esc, self.comp.rng, self.P
        if kind == 'T':
            raw = comp.tension(8)
        else:
            chords = comp.acordes(kind)
            raw = comp.partes(kind, chords, [None] * 8, heading)
            c3 = esc.instr['c3b'] if kind == 'B' else esc.instr['c3']
            raw[5] = [(p, d, h, 0.92, c3, a) for p, d, h, a in comp.melodia(kind, chords)]
        limit = 8 * P
        out = {c: [] for c in range(6)}
        for c, notes in raw.items():
            for p, d, h, v, ins, extra in notes:
                if p >= limit or p < 0:
                    continue
                d = min(d, limit - p)
                offset = max(-s.ms(5), min(s.ms(5), rng.gauss(0, s.ms(1.0 if c in esc.percusion else 1.5))))
                spread = 1 + rng.uniform(-0.1, 0.1)
                out[c].append([p, d, h, v, ins, extra, offset, spread])
        for c in out:
            out[c].sort(key=lambda n: n[0])
        return out

    def schedule_bar(self, b, start, semi, intensity):
        P, esc = self.P, self.esc
        for c in range(6):
            written = self.written[c]
            new = []
            while self.cursor[c] < len(written) and written[self.cursor[c]][0] < (b + 1) * P:
                p, d, h, v, ins, extra, offset, spread = written[self.cursor[c]]
                self.cursor[c] += 1
                local = p - P * (p // P)
                pair = 2 * (local // 2)
                rest = local - pair
                at = pair + rest * 2 * esc.swing if rest < 1 else pair + 2 * esc.swing + (rest - 1) * 2 * (1 - esc.swing)
                accent = 1.06 if p % P == 0 else (1.0 if p % esc.por_tiempo == 0 else 0.93)
                vel = v * accent * intensity * spread
                held = max(1, int(d * semi * GATE[c]))
                new.append({'t': int(max(0.0, start + at * semi + offset)), 'c': c, 'ins': ins, 'h': h, 'held': held,
                            'held0': held, 'vel': vel, 'gain': vel * self.bank[ins].volumen * esc.volumen[c],
                            'extra': extra, 'section': self.section, 'p': p, 'd': d, 'semi': semi})
            new.sort(key=lambda n: n['t'])
            self.notes.extend(new)

    def cut(self, limit, now):
        for c in range(6):
            channel = [n for n in self.notes if n['c'] == c]
            started = [n for n in channel if n['t'] < now]
            active = started[-1] if started else None
            for n in channel:
                if n['section'] != self.section or n['p'] + n['d'] <= limit:
                    continue
                held = max(1, int((limit - n['p']) * n['semi'] * GATE[c]))
                if n['t'] >= now:
                    n['held'] = min(n['held'], held)
                elif n is active and now - n['t'] < n['held'] and held < n['held']:
                    n['held'] = max(held, now - n['t'])

    def layers(self, tension, start, semi):
        if self.end_at is not None:
            return
        for layer, channels in self.esc.capas.items():
            on = self.level >= LAYER_LEVEL[layer] or tension
            if self.last_level < 0:
                self.layer_on[layer] = on
                for c in channels:
                    self.automation[c] = [(0, 1.0 if on else 0.0)]
            elif on != self.layer_on[layer]:
                self.layer_on[layer] = on
                a, b = int(start), int(start + self.esc.por_tiempo * semi)
                for c in channels:
                    self.automation[c] += [(a, 0.0 if on else 1.0), (b, 1.0 if on else 0.0)]

    def play(self, cues, total):
        """Decides the bars as the engine renders blocks, giving each control at its sample."""
        now, i = 0, 0
        while now < total:
            while i < len(cues) and cues[i][0] <= now:
                what = cues[i][1]
                if what == 'level':
                    self.requested = max(1, min(5, cues[i][2]))
                elif what == 'tense' and self.end_at is None:
                    self.warning = self.next_beat(now)
                elif what == 'end' and self.end_at is None:
                    self.end_at = self.next_beat(now)
                i += 1
            call = min(1024, total - now)
            if i < len(cues):
                call = min(call, cues[i][0] - now)
            done = 0
            while done < call:
                self.decide(now)
                n = min(256, call - done)
                if not self.stopped:
                    n = min(n, int(self.next_bar) - LOOKAHEAD - now)
                now += n
                done += n
        if self.end_at is not None:
            for c in range(6):
                last = self.automation[c][-1][1] if self.automation[c] else 1.0
                self.automation[c] += [(int(self.end_at), last), (int(self.end_at + FADE), 0.0)]


def demo_cues():
    return [(int(20.0 * FS) - LOOKAHEAD, 'level', 2), (int(40.0 * FS) - LOOKAHEAD, 'level', 3),
            (int(60.0 * FS) - LOOKAHEAD, 'level', 4), (int(72.0 * FS), 'tense'), (int(84.0 * FS), 'end')]


def fingerprint(notes):
    """FNV-1a (64 bits) of every note: start, channel, instrument, pitch, length and gain (to 1e-9)."""
    text = ''.join('%d,%d,%s,%d,%d,%d;' % (n['t'], n['c'], s.banco()[n['ins']].nombre, n['h'], n['held0'],
                                          round(n['gain'] * 1e9)) for n in notes)
    h = 0xcbf29ce484222325
    for byte in text.encode('utf-8'):
        h = ((h ^ byte) * 0x100000001b3) & 0xFFFFFFFFFFFFFFFF
    return h


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--salida', type=Path, help='folder for the notes and the music of each scenario')
    args = parser.parse_args()
    total = int(88.4 * FS)
    for name in ('nieve', 'desierto', 'bosque'):
        run = Run(g.ESCENARIOS[name], 7)
        run.play(demo_cues(), total)
        print(f'{name}: {len(run.notes)} notes, fingerprint 0x{fingerprint(run.notes):016x}; sections',
              ', '.join(f'{k} {t / FS:.3f} s ({bpm:.1f} BPM)' for k, t, bpm in run.sections))
        if args.salida:
            args.salida.mkdir(parents=True, exist_ok=True)
            notes = [g.Nota(n['t'], n['c'], run.bank[n['ins']], n['h'], n['held'], n['vel'], 0, n['extra'])
                     for n in run.notes]
            music, _ = g.renderizar(notes, total, g.canales_de(run.esc), s.Eco(*run.esc.eco), automatizacion=run.automation)
            (args.salida / f'{name}.f64').write_bytes(struct.pack(f'<{len(music)}d', *music))
            (args.salida / f'{name}-notes.json').write_text(json.dumps(
                [[n['t'], n['c'], n['ins'], n['h'], n['held'], n['vel'], n['extra']] for n in run.notes]))


if __name__ == '__main__':
    main()
