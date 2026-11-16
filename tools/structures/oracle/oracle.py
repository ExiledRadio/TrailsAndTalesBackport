"""Vanilla 1.20.1 oracle for trail ruins and desert pyramids.

    python -I oracle.py <server.jar> <workdir> <seed> [regions]

Makes <workdir>/<seed>/, runs the vanilla 1.20.1 server with that seed and
  1. force-loads the candidate chunk of every trail_ruins region (random_spread 34/8, salt 83469867) and every
     desert_pyramids region (32/8, salt 14357617) in regions -R..R-1, saves;
  2. reads the saved starts, force-loads every chunk a valid start's box touches, saves, stops;
then writes <workdir>/<seed>.json: per candidate the start (valid or not; pieces with element, position, rotation,
box, processors) and every brushable block entity (suspicious sand and gravel) inside the start's box, with its
LootTable and LootTableSeed. tools/structures/oracle120/*.json are this script's output; the self-test reads them.
"""
import json
import os
import subprocess
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtread  # noqa: E402

SETS = {
    'minecraft:trail_ruins': (34, 8, 83469867),
    'minecraft:desert_pyramid': (32, 8, 14357617),
}
M = (1 << 48) - 1
JAVA = r'C:\Program Files\Java\jdk-21.0.11\bin\java.exe'


def jrandom(seed):
    return (seed ^ 0x5DEECE66D) & M


def nxt(state, bits):
    state = (state * 0x5DEECE66D + 0xB) & M
    r = state >> (48 - bits)
    if bits == 32 and r >= 1 << 31:
        r -= 1 << 32
    return state, r


def next_int(state, bound):
    if bound & (bound - 1) == 0:
        state, r = nxt(state, 31)
        return state, (bound * r) >> 31
    while True:
        state, bits = nxt(state, 31)
        val = bits % bound
        if bits - val + (bound - 1) < (1 << 31):
            return state, val


def candidate(seed, rx, rz, spacing, separation, salt):
    s = (rx * 341873128712 + rz * 132897987541 + seed + salt) & ((1 << 64) - 1)
    st = jrandom(s)
    st, ox = next_int(st, spacing - separation)
    st, oz = next_int(st, spacing - separation)
    return rx * spacing + ox, rz * spacing + oz


def candidates(seed, radius):
    out = []
    for sid, (sp, se, sa) in SETS.items():
        for rx in range(-radius, radius):
            for rz in range(-radius, radius):
                cx, cz = candidate(seed, rx, rz, sp, se, sa)
                out.append((sid, cx, cz))
    return out


def read_start(region, sid, cx, cz):
    tag = nbtread.chunk(region, cx, cz)
    if tag is None:
        return {'structure': sid, 'chunk': [cx, cz], 'missing': True}
    s = tag.get('structures', {}).get('starts', {}).get(sid)
    if s is None or s.get('id') == 'INVALID':
        return {'structure': sid, 'chunk': [cx, cz], 'valid': False}
    pieces = []
    for c in s.get('Children', []):
        el = c.get('pool_element', {})
        pieces.append({'id': c.get('id'), 'location': el.get('location'), 'processors': el.get('processors'),
                       'type': el.get('element_type'), 'pos': [c.get('PosX'), c.get('PosY'), c.get('PosZ')],
                       'rotation': c.get('rotation'), 'ground': c.get('ground_level_delta'), 'bb': list(c.get('BB', [])),
                       'orientation': c.get('O'), 'junctions': c.get('junctions')})
    return {'structure': sid, 'chunk': [cx, cz], 'valid': True, 'pieces': pieces}


def box_of(start):
    bbs = [p['bb'] for p in start['pieces'] if p['bb']]
    return [min(b[0] for b in bbs), min(b[1] for b in bbs), min(b[2] for b in bbs),
            max(b[3] for b in bbs), max(b[4] for b in bbs), max(b[5] for b in bbs)]


def main():
    jar, work, seed = sys.argv[1], sys.argv[2], int(sys.argv[3])
    radius = int(sys.argv[4]) if len(sys.argv) > 4 else 2
    d = os.path.join(work, str(seed))
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, 'eula.txt'), 'w') as f:
        f.write('eula=true\n')
    with open(os.path.join(d, 'server.properties'), 'w') as f:
        f.write('level-seed=%d\nonline-mode=false\nserver-port=25991\nspawn-protection=0\n'
                'view-distance=2\nsimulation-distance=2\nlevel-name=world\nsync-chunk-writes=true\nmax-tick-time=-1\n' % seed)
    cands = candidates(seed, radius)
    region = os.path.join(d, 'world', 'region')
    p = subprocess.Popen([JAVA, '-Xmx4G', '-jar', os.path.abspath(jar), 'nogui'], cwd=d, stdin=subprocess.PIPE,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    print('pid', p.pid, flush=True)
    lines = []
    done = threading.Event()

    def reader():
        for line in p.stdout:
            lines.append(line)
            if 'Done (' in line:
                done.set()
    threading.Thread(target=reader, daemon=True).start()
    done.wait(900)

    def cmd(c):
        p.stdin.write(c + '\n')
        p.stdin.flush()
    for sid, cx, cz in cands:
        cmd('forceload add %d %d' % (cx * 16, cz * 16))
    time.sleep(60 + 2 * len(cands))
    cmd('save-all flush')
    time.sleep(30)
    starts = [read_start(region, sid, cx, cz) for sid, cx, cz in cands]
    n = 0
    for s in starts:
        if s.get('valid'):
            b = box_of(s)
            cmd('forceload add %d %d %d %d' % (b[0] - 16, b[2] - 16, b[3] + 16, b[5] + 16))
            n += 1
    print('valid starts', n, flush=True)
    time.sleep(60 + 40 * n)
    cmd('save-all flush')
    time.sleep(40)
    cmd('stop')
    p.wait(600)
    with open(os.path.join(d, 'oracle-server.log'), 'w') as f:
        f.writelines(lines)
    parse(d, work, seed, cands)


def parse(d, work, seed, cands):
    region = os.path.join(d, 'world', 'region')
    out = {'seed': seed, 'version': '1.20.1', 'starts': []}
    for sid, cx, cz in cands:
        s = read_start(region, sid, cx, cz)
        if s.get('valid'):
            b = box_of(s)
            sus = []
            for x in range(b[0] >> 4, (b[3] >> 4) + 1):
                for z in range(b[2] >> 4, (b[5] >> 4) + 1):
                    tag = nbtread.chunk(region, x, z)
                    if tag is None:
                        s.setdefault('missingChunks', []).append([x, z])
                        continue
                    if tag.get('Status') not in ('minecraft:full', 'full'):
                        s.setdefault('unfinishedChunks', []).append([x, z, tag.get('Status')])
                    for be in tag.get('block_entities', []):
                        if be.get('id') == 'minecraft:brushable_block':
                            sus.append({'pos': [be['x'], be['y'], be['z']], 'loot': be.get('LootTable'),
                                        'seed': be.get('LootTableSeed')})
            s['box'] = b
            s['suspicious'] = sorted(sus, key=lambda e: e['pos'])
        out['starts'].append(s)
    with open(os.path.join(work, '%d.json' % seed), 'w') as f:
        json.dump(out, f, indent=1)
    print('wrote', sum(1 for s in out['starts'] if s.get('valid')), 'valid of', len(cands), flush=True)


if __name__ == '__main__':
    if len(sys.argv) > 5 and sys.argv[5] == 'parse':
        w, sd = sys.argv[2], int(sys.argv[3])
        parse(os.path.join(w, str(sd)), w, sd, candidates(sd, int(sys.argv[4])))
    else:
        main()
