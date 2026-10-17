"""Shared helpers + the sprite atlas (tex_atlas_8k.png) for tools/make-textures.py.

Everything here is original, procedurally drawn art (no upstream artwork). The
layout is fixed by the engine (see app/src/game/game_data.c, redraw.c and
android/.../SkinTextures.kt): a 7 x 9 grid of 448 px cells.
"""
import json
import math
import pathlib

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "app" / "res" / "textures"
ART = ROOT / "tools" / "ext-art"

CELL = 448
ATLAS = (7 * CELL, 9 * CELL)


def hexc(v):
    return np.array([(v >> 16) & 255, (v >> 8) & 255, v & 255], np.float32) / 255.0


def lerp(a, b, t):
    return a + (b - a) * t


# --------------------------------------------------------------------- noise
def value_noise(size, cells, seed, octaves=3):
    """Tileless smooth noise in 0..1, size x size."""
    rng = np.random.default_rng(seed)
    out = np.zeros((size, size), np.float32)
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = cells * (2 ** o) + 1
        g = rng.random((n, n)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BICUBIC)
        out += np.asarray(im, np.float32) / 255.0 * amp
        total += amp
        amp *= 0.5
    return out / total


def tex_from(draw_fn, size=256, bg=(0, 0, 0)):
    im = Image.new("RGB", (size, size), bg)
    draw_fn(ImageDraw.Draw(im), size)
    return im


# ------------------------------------------------------------ sphere shading
def shade_sphere(albedo, size, spec=0.75, ambient=0.34, edge=0.0):
    """albedo: HxWx3 float 0..1 texture sampled planar over the disc, or an RGB
    triple. Returns RGBA uint8 of a lit sphere filling the cell."""
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    u = (xs + 0.5) / size * 2 - 1
    v = (ys + 0.5) / size * 2 - 1
    r2 = u * u + v * v
    z = np.sqrt(np.clip(1 - r2, 0, 1))
    L = np.array([-0.38, -0.5, 0.78], np.float32)
    L /= np.linalg.norm(L)
    nd = np.clip(u * L[0] + v * L[1] + z * L[2], 0, 1)
    H = L + np.array([0, 0, 1], np.float32)
    H /= np.linalg.norm(H)
    ns = np.clip(u * H[0] + v * H[1] + z * H[2], 0, 1)
    if np.ndim(albedo) == 1:
        alb = np.broadcast_to(np.asarray(albedo, np.float32), (size, size, 3))
    else:
        alb = albedo
    light = ambient + (1 - ambient) * nd
    light = light * (1.0 - 0.18 * (1 - z) ** 2)
    col = alb * light[..., None] + spec * (ns ** 48)[..., None] * 0.9
    col += 0.10 * np.clip((ns - 0.82) / 0.18, 0, 1)[..., None] * 0.0
    col = np.clip(col, 0, 1)
    a = np.clip((1 - np.sqrt(r2)) * size / 2.0, 0, 1)
    rgba = np.dstack([col, a])
    return Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA")


def sample_tex(tex, size, squash=0.92):
    """Planar-with-bulge sampling of a square PIL texture over the disc."""
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    u = (xs + 0.5) / size * 2 - 1
    v = (ys + 0.5) / size * 2 - 1
    r = np.sqrt(u * u + v * v)
    k = np.where(r > 1e-4, np.arcsin(np.clip(r, 0, 1)) / (np.pi / 2) / np.maximum(r, 1e-4), 1.0)
    su, sv = u * k * squash, v * k * squash
    t = np.asarray(tex.convert("RGB"), np.float32) / 255.0
    th, tw = t.shape[:2]
    ix = np.clip(((su * 0.5 + 0.5) * tw).astype(int), 0, tw - 1)
    iy = np.clip(((sv * 0.5 + 0.5) * th).astype(int), 0, th - 1)
    return t[iy, ix]


def soft_blob(px, alpha=0.55, power=2.2):
    ys, xs = np.mgrid[0:px, 0:px].astype(np.float32)
    d = np.sqrt(((xs + 0.5) / px * 2 - 1) ** 2 + ((ys + 0.5) / px * 2 - 1) ** 2)
    a = np.clip(1 - d, 0, 1) ** power * alpha
    rgba = np.zeros((px, px, 4), np.uint8)
    rgba[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


# --------------------------------------------------------- bead 2D textures
def _noise_rgb(size, cells, seed, stops):
    n = value_noise(size, cells, seed)
    return _ramp(n, stops)


def _ramp(t, stops):
    """stops: [(pos, rgb 0..1)] sorted; t: array."""
    out = np.zeros(t.shape + (3,), np.float32)
    pos = [p for p, _ in stops]
    for c in range(3):
        out[..., c] = np.interp(t, pos, [s[1][c] for s in stops])
    return out


def C(v):
    return tuple(int(x * 255) for x in hexc(v))


def _stripes(d, s, c1, c2, n=8, ang=0.6):
    d.rectangle((0, 0, s, s), fill=c1)
    for i in range(-n, 2 * n):
        x = i * s / n * 0.5
        d.polygon([(x, 0), (x + s / n * 0.25, 0), (x + s / n * 0.25 - s * ang, s), (x - s * ang, s)], fill=c2)


def _dots(d, s, c1, c2, n=6, r=0.28):
    d.rectangle((0, 0, s, s), fill=c1)
    for j in range(-1, n + 1):
        for i in range(-1, n + 1):
            cx = (i + 0.5 * (j % 2) + 0.5) * s / n
            cy = (j + 0.5) * s / n
            d.ellipse((cx - r * s / n, cy - r * s / n, cx + r * s / n, cy + r * s / n), fill=c2)


def _checker(d, s, c1, c2, n=6):
    for j in range(n):
        for i in range(n):
            d.rectangle((i * s / n, j * s / n, (i + 1) * s / n, (j + 1) * s / n), fill=c1 if (i + j) % 2 == 0 else c2)


def _hexes(d, s, c1, c2, n=5):
    d.rectangle((0, 0, s, s), fill=c1)
    w = s / n
    for j in range(-1, n * 2):
        for i in range(-1, n + 1):
            cx = (i + 0.5 * (j % 2)) * w + w / 2
            cy = j * w * 0.866 * 0.66 + w / 2
            pts = [(cx + w * 0.5 * math.cos(math.pi / 3 * k + math.pi / 6), cy + w * 0.5 * math.sin(math.pi / 3 * k + math.pi / 6)) for k in range(6)]
            d.polygon(pts, outline=c2, width=max(2, int(s / 90)))


def _star(d, cx, cy, r, color, pts=5, inner=0.42, rot=-math.pi / 2):
    P = []
    for k in range(pts * 2):
        rr = r if k % 2 == 0 else r * inner
        a = rot + math.pi * k / pts
        P.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(P, fill=color)


def _heart(d, cx, cy, r, color):
    P = []
    for k in range(60):
        t = k / 60 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        P.append((cx + x / 17 * r, cy + y / 17 * r))
    d.polygon(P, fill=color)


def _bolt(d, s, color):
    P = [(0.58, 0.12), (0.30, 0.55), (0.48, 0.55), (0.40, 0.9), (0.72, 0.42), (0.53, 0.42), (0.64, 0.12)]
    d.polygon([(x * s, y * s) for x, y in P], fill=color)


def bead_texture(i, name):
    """A 256 px texture (PIL RGB) for Dvd bead i."""
    s = 256

    def mk(fn, bg=(0, 0, 0)):
        return tex_from(fn, s, bg)

    rng = np.random.default_rng(i * 7 + 3)
    if name == "India":
        def f(d, s):
            d.rectangle((0, 0, s, s * 0.36), fill=C(0xFF9933)); d.rectangle((0, s * 0.36, s, s * 0.64), fill=(246, 246, 246)); d.rectangle((0, s * 0.64, s, s), fill=C(0x138808))
            d.ellipse((s * .44, s * .44, s * .56, s * .56), outline=C(0x000080), width=4)
        return mk(f)
    if name == "Star":
        return mk(lambda d, s: (d.rectangle((0, 0, s, s), fill=C(0x1F3A8A)), _star(d, s / 2, s / 2, s * .34, (245, 245, 250))))
    if name == "Heart":
        return mk(lambda d, s: (d.rectangle((0, 0, s, s), fill=C(0xF7B6C8)), _heart(d, s / 2, s * .52, s * .3, C(0xE0507A))))
    if name == "Dragon scales":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x1D7A44))
            n = 7
            for j in range(-1, n + 1):
                for k in range(-1, n + 1):
                    cx = (k + 0.5 * (j % 2)) * s / n + s / n / 2; cy = j * s / n * 0.55
                    d.pieslice((cx - s / n * .55, cy - s / n * .55, cx + s / n * .55, cy + s / n * .55), 0, 180, fill=C(0x2FA45E), outline=C(0x0F4A28), width=2)
        return mk(f)
    if name == "Stripes":
        return mk(lambda d, s: _stripes(d, s, C(0xD7263D), C(0x6A0F1E), 7))
    if name == "Dots":
        return mk(lambda d, s: _dots(d, s, C(0x1F5FD6), (190, 215, 255)))
    if name == "Lightning":
        return mk(lambda d, s: (d.rectangle((0, 0, s, s), fill=C(0x4B2A8C)), _bolt(d, s, C(0xFFE14A))))
    if name == "Flame":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xFF6A1A))
            d.polygon([(s * .5, s * .1), (s * .8, s * .55), (s * .68, s * .88), (s * .32, s * .88), (s * .2, s * .55)], fill=C(0xFFC43A))
            d.ellipse((s * .38, s * .5, s * .62, s * .85), fill=(255, 245, 190))
        return mk(f)
    if name == "Crescent":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x16245A)); d.ellipse((s * .22, s * .2, s * .78, s * .8), fill=(245, 240, 215)); d.ellipse((s * .36, s * .16, s * .88, s * .76), fill=C(0x16245A))
        return mk(f)
    if name in ("Chrome", "Pearl", "Copper", "Rose gold", "Gold"):
        base = {"Chrome": 0xC0C4CA, "Pearl": 0xF4F0F4, "Copper": 0xC4782E, "Rose gold": 0xE8AAA0, "Gold": 0xE0A838}[name]
        c = hexc(base)
        y = np.linspace(0, 1, s)[:, None]
        band = 0.5 + 0.5 * np.sin(y * 9 + 0.7 * np.sin(y * 21)) 
        arr = np.clip(c[None, None, :] * (0.55 + 0.6 * band[..., None]) + 0.08, 0, 1) * np.ones((s, s, 1), np.float32)
        return Image.fromarray((arr * 255).astype(np.uint8), "RGB")
    if name == "Galaxy":
        n = value_noise(s, 3, i, 4)
        arr = _ramp(n, [(0, hexc(0x0C0828)), (0.5, hexc(0x3A2A7A)), (0.8, hexc(0x9A5AD0)), (1, hexc(0xE8A0E8))])
        a8 = (arr * 255).astype(np.uint8)
        for _ in range(60):
            x, y = rng.integers(0, s, 2); a8[y, x] = 255
        return Image.fromarray(a8, "RGB")
    if name == "Honeycomb":
        return mk(lambda d, s: _hexes(d, s, C(0xFFC43A), C(0x9A6A00), 5))
    if name == "Argyle":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x1E2F5C)); n = 4
            for j in range(n):
                for k in range(n):
                    cx, cy, w = (k + .5) * s / n, (j + .5) * s / n, s / n / 2
                    d.polygon([(cx, cy - w), (cx + w, cy), (cx, cy + w), (cx - w, cy)], fill=C(0xFF8A1E) if (j + k) % 2 else C(0x2C4A8C))
        return mk(f)
    if name == "Zigzag":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xFF8A1E))
            for r in range(-1, 8):
                pts = [(x, r * s / 6 + (s / 14 if (x // (s // 8)) % 2 else -s / 14)) for x in range(0, s + 1, s // 8)]
                d.line(pts, fill=(40, 20, 0), width=int(s / 22))
        return mk(f)
    if name == "Carbon fibre":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=(22, 22, 26)); n = 12
            for j in range(n):
                for k in range(n):
                    c = (58, 60, 68) if (j + k) % 2 else (30, 31, 36)
                    d.rectangle((k * s / n + 1, j * s / n + 1, (k + 1) * s / n - 1, (j + 1) * s / n - 1), fill=c)
        return mk(f)
    if name == "Tiger":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xF28A1C))
            for k in range(7):
                x = k * s / 6 + rng.integers(-8, 8)
                d.polygon([(x - 7, 0), (x + 7, 0), (x + 3 + rng.integers(-10, 10), s / 2), (x + rng.integers(-6, 6), s)], fill=(30, 15, 5))
        return mk(f)
    if name == "Circuit":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x0E5A34))
            for _ in range(14):
                x, y = rng.integers(10, s - 10, 2); l = int(rng.integers(30, 90))
                d.line([(x, y), (x + l, y), (x + l, y + l // 2)], fill=C(0x55E39B), width=4); d.ellipse((x - 5, y - 5, x + 5, y + 5), fill=C(0xB8FFD8))
        return mk(f)
    if name == "Leopard":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xD6A048))
            for _ in range(26):
                x, y = rng.integers(0, s, 2); r = int(rng.integers(12, 22))
                d.ellipse((x - r, y - r, x + r, y + r), outline=(50, 28, 8), width=6)
        return mk(f)
    if name in ("Lava", "Marble", "Ice", "Camo", "Giraffe", "Python", "Wood", "Denim", "Sunset", "Aurora", "Holographic", "Waves", "Peacock", "Bubblegum"):
        cells = {"Marble": 3, "Ice": 4, "Camo": 3, "Giraffe": 3}.get(name, 3)
        if name == "Lava":
            arr = _ramp(value_noise(s, 3, i, 4), [(0, hexc(0x2A0800)), (0.45, hexc(0xC8501A)), (0.7, hexc(0xFF9A20)), (1, hexc(0xFFE060))])
        elif name == "Ice":
            arr = _ramp(value_noise(s, 4, i, 4), [(0, hexc(0x4A90C0)), (0.6, hexc(0x96CDF0)), (1, hexc(0xFFFFFF))])
        elif name == "Marble":
            n = value_noise(s, 3, i, 5); v = np.abs(np.sin((np.arange(s)[None, :] / s * 5 + n * 5) * math.pi))
            arr = _ramp(v ** 0.35, [(0, hexc(0x50505C)), (1, hexc(0xF2F0EC))])
        elif name == "Camo":
            n = value_noise(s, 3, i, 3)
            arr = _ramp(np.floor(n * 4) / 3, [(0, hexc(0x2A3418)), (0.4, hexc(0x4A5A28)), (0.7, hexc(0x6A7A42)), (1, hexc(0x9A8A58))])
        elif name == "Giraffe":
            n = value_noise(s, 4, i, 2); arr = _ramp((n > 0.5).astype(np.float32), [(0, hexc(0xF0DCA0)), (1, hexc(0xA8662A))])
        elif name == "Python":
            n = value_noise(s, 5, i, 2); arr = _ramp(((n * 6) % 1.0), [(0, hexc(0x5A4A20)), (0.5, hexc(0xA89650)), (1, hexc(0x3A3010))])
        elif name == "Wood":
            n = value_noise(s, 2, i, 3); v = (np.arange(s)[None, :] / s * 14 + n * 3) % 1.0
            arr = _ramp(v, [(0, hexc(0xB0703A)), (0.5, hexc(0x8A5028)), (1, hexc(0xB0703A))])
        elif name == "Denim":
            yy, xx = np.mgrid[0:s, 0:s]; v = 0.5 + 0.25 * ((yy // 3 + xx // 3) % 2) + 0.2 * value_noise(s, 8, i, 2)
            arr = _ramp(v, [(0, hexc(0x1C3A6C)), (1, hexc(0x4C78B8))])
        elif name == "Sunset":
            y = np.linspace(0, 1, s)[:, None] * np.ones((1, s)); arr = _ramp(y, [(0, hexc(0x24083C)), (0.45, hexc(0xE0507A)), (0.75, hexc(0xFF9A3C)), (1, hexc(0xFFE08A))])
        elif name == "Aurora":
            n = value_noise(s, 3, i, 3); y = np.linspace(0, 1, s)[:, None] * np.ones((1, s))
            arr = _ramp((y * 0.6 + n * 0.5) % 1.0, [(0, hexc(0x0A1028)), (0.4, hexc(0x20C080)), (0.7, hexc(0x60D0F0)), (1, hexc(0xA070F0))])
        elif name == "Holographic":
            yy, xx = np.mgrid[0:s, 0:s]; h = ((xx + yy) / (2.0 * s) * 1.5 + value_noise(s, 3, i, 2) * 0.4) % 1.0
            arr = _ramp(h, [(0, hexc(0xFF7AD0)), (0.25, hexc(0xFFE27A)), (0.5, hexc(0x7AFFC8)), (0.75, hexc(0x7AB8FF)), (1, hexc(0xFF7AD0))])
        elif name == "Waves":
            yy, xx = np.mgrid[0:s, 0:s]; v = np.sin((yy / s * 9 + np.sin(xx / s * 8) * 0.6) * math.pi) * 0.5 + 0.5
            arr = _ramp(v, [(0, hexc(0x1C6AA8)), (1, hexc(0x78D2F0))])
        elif name == "Peacock":
            yy, xx = np.mgrid[0:s, 0:s]; d = np.sqrt((xx - s / 2) ** 2 + (yy - s / 2) ** 2) / s
            arr = _ramp(np.clip(d * 2.2, 0, 1), [(0, hexc(0x1E3AA0)), (0.25, hexc(0x20C0B0)), (0.5, hexc(0x20A050)), (0.8, hexc(0x14783C)), (1, hexc(0x0A3C28))])
        else:  # Bubblegum
            n = value_noise(s, 3, i, 3); arr = _ramp(n, [(0, hexc(0xFF8AC0)), (1, hexc(0xFFD0E8))])
        return Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGB")
    if name == "Checker":
        return mk(lambda d, s: _checker(d, s, (28, 28, 32), (232, 232, 236), 6))
    if name == "Tartan":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xB21824))
            for k in range(0, s, s // 4):
                d.rectangle((k, 0, k + s // 16, s), fill=(20, 40, 90)); d.rectangle((0, k, s, k + s // 16), fill=(20, 40, 90))
                d.rectangle((k + s // 8, 0, k + s // 8 + 3, s), fill=(240, 220, 120)); d.rectangle((0, k + s // 8, s, k + s // 8 + 3), fill=(240, 220, 120))
        return mk(f)
    if name == "Rainbow":
        def f(d, s):
            cols = [0xE02020, 0xFF8A1E, 0xFFE020, 0x20B040, 0x2080F0, 0x8040D0]
            for k, c in enumerate(cols):
                d.rectangle((0, k * s / 6, s, (k + 1) * s / 6 + 1), fill=C(c))
        return mk(f)
    if name == "Sakura":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xFCD6E0))
            for (cx, cy) in [(.3, .3), (.7, .5), (.35, .75)]:
                for a in range(5):
                    ang = a * 2 * math.pi / 5; x, y = s * cx + math.cos(ang) * s * .07, s * cy + math.sin(ang) * s * .07
                    d.ellipse((x - s * .06, y - s * .06, x + s * .06, y + s * .06), fill=C(0xE8708C))
                d.ellipse((s * cx - 4, s * cy - 4, s * cx + 4, s * cy + 4), fill=C(0xFFE060))
        return mk(f)
    if name == "Snowflake":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x8CC0EC))
            for a in range(6):
                ang = a * math.pi / 3; x, y = s / 2 + math.cos(ang) * s * .32, s / 2 + math.sin(ang) * s * .32
                d.line((s / 2, s / 2, x, y), fill=(250, 252, 255), width=7)
                mx, my = s / 2 + math.cos(ang) * s * .2, s / 2 + math.sin(ang) * s * .2
                for da in (-0.7, 0.7):
                    d.line((mx, my, mx + math.cos(ang + da) * s * .1, my + math.sin(ang + da) * s * .1), fill=(250, 252, 255), width=5)
        return mk(f)
    if name == "Skull":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=(18, 18, 22)); d.ellipse((s * .26, s * .2, s * .74, s * .66), fill=(240, 240, 236)); d.rectangle((s * .36, s * .55, s * .64, s * .76), fill=(240, 240, 236))
            d.ellipse((s * .33, s * .38, s * .46, s * .52), fill=(18, 18, 22)); d.ellipse((s * .54, s * .38, s * .67, s * .52), fill=(18, 18, 22)); d.polygon([(s * .5, s * .52), (s * .46, s * .6), (s * .54, s * .6)], fill=(18, 18, 22))
        return mk(f)
    if name == "Music":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x1E9E98)); d.ellipse((s * .3, s * .6, s * .5, s * .74), fill=(250, 250, 250)); d.rectangle((s * .46, s * .22, s * .5, s * .68), fill=(250, 250, 250)); d.polygon([(s * .5, s * .22), (s * .72, s * .32), (s * .72, s * .4), (s * .5, s * .32)], fill=(250, 250, 250))
        return mk(f)
    if name == "Paw":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xECD0A8)); c = C(0x7A4A28)
            d.ellipse((s * .33, s * .5, s * .67, s * .8), fill=c)
            for (x, y) in [(.26, .42), (.4, .3), (.6, .3), (.74, .42)]: d.ellipse((s * x - s * .07, s * y - s * .08, s * x + s * .07, s * y + s * .08), fill=c)
        return mk(f)
    if name in ("Diamond", "Ruby", "Emerald"):
        base = {"Diamond": 0x78D2F0, "Ruby": 0xC81432, "Emerald": 0x14A05A}[name]; c = hexc(base)
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=tuple(int(x * 255 * .7) for x in c)); cx = s / 2
            for k in range(8):
                a = k * math.pi / 4
                d.polygon([(cx, cx), (cx + math.cos(a) * s, cx + math.sin(a) * s), (cx + math.cos(a + math.pi / 8) * s, cx + math.sin(a + math.pi / 8) * s)], fill=tuple(min(255, int(x * 255 * (1.15 if k % 2 else .85))) for x in c))
        return mk(f)
    if name == "Neon grid":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x24083C))
            for k in range(0, s + 1, s // 6):
                d.line((k, 0, k, s), fill=C(0xFF3CC8), width=4); d.line((0, k, s, k), fill=C(0x3CE0FF), width=4)
        return mk(f)
    if name == "Zebra":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=(244, 242, 236))
            for k in range(-2, 9):
                pts = [(k * s / 6 + math.sin(y / s * 7 + k) * 14, y) for y in range(0, s + 1, 12)]
                d.line(pts, fill=(18, 18, 20), width=int(s / 14))
        return mk(f)
    if name == "Cow":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=(248, 246, 240))
            for _ in range(7):
                x, y = rng.integers(0, s, 2); w = int(rng.integers(30, 60)); d.ellipse((x - w, y - w * .7, x + w, y + w * .7), fill=(24, 24, 26))
        return mk(f)
    if name == "Sun":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xFFB428))
            for k in range(14):
                a = k * math.pi / 7; d.polygon([(s / 2 + math.cos(a) * s * .18, s / 2 + math.sin(a) * s * .18), (s / 2 + math.cos(a + .15) * s * .5, s / 2 + math.sin(a + .15) * s * .5), (s / 2 + math.cos(a - .15) * s * .5, s / 2 + math.sin(a - .15) * s * .5)], fill=C(0xFF8A1E))
            d.ellipse((s * .3, s * .3, s * .7, s * .7), fill=C(0xFFE060))
        return mk(f)
    if name == "Electric":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0x0C143C)); _bolt(d, s, (120, 220, 255))
            d.line([(s * .1, s * .2), (s * .3, s * .3), (s * .22, s * .45)], fill=(160, 230, 255), width=4)
        return mk(f)
    if name == "Watermelon":
        def f(d, s):
            d.rectangle((0, 0, s, s), fill=C(0xEC4052))
            for k in range(-2, 8):
                d.line((k * s / 5, 0, k * s / 5 - s * .25, s), fill=C(0x1E8A3C), width=int(s / 16))
            for _ in range(14):
                x, y = rng.integers(10, s - 10, 2); d.ellipse((x - 4, y - 7, x + 4, y + 7), fill=(24, 20, 20))
        return mk(f)
    if name == "Pixel":
        def f(d, s):
            n = 8
            for j in range(n):
                for k in range(n):
                    h = (j * 7 + k * 13 + 5) % 5; c = [(52, 152, 219), (80, 180, 235), (30, 110, 180), (120, 210, 250), (60, 130, 200)][h]
                    d.rectangle((k * s / n, j * s / n, (k + 1) * s / n, (j + 1) * s / n), fill=c)
        return mk(f)
    return tex_from(lambda d, s: d.rectangle((0, 0, s, s), fill=(128, 128, 128)))


# ----------------------------------------------------------------- the atlas
BEAD_NAMES = [
    "India", "Star", "Heart", "Dragon scales", "Stripes", "Dots", "Lightning", "Flame",
    "Crescent", "Chrome", "Gold", "Galaxy", "Honeycomb", "Argyle", "Zigzag", "Carbon fibre",
    "Tiger", "Circuit", "Leopard", "Lava", "Ice", "Marble", "Holographic", "Camo",
    "Checker", "Tartan", "Rainbow", "Sakura", "Snowflake", "Skull", "Music", "Paw",
    "Diamond", "Ruby", "Emerald", "Pearl", "Copper", "Rose gold", "Neon grid", "Sunset",
    "Waves", "Zebra", "Cow", "Giraffe", "Python", "Peacock", "Wood", "Denim",
    "Bubblegum", "Aurora", "Sun", "Electric", "Watermelon", "Pixel",
]
DVD_BEAD_CELLS = [(7, 3), (7, 6), (8, 0), (8, 1), (8, 2), (8, 3)]  # (row, col)


def make_atlas(preview=None):
    atlas = Image.new("RGBA", ATLAS, (0, 0, 0, 0))

    def put(img, col, row, dx=0, dy=0):
        atlas.alpha_composite(img, (int(col * CELL + dx), int(row * CELL + dy)))

    cg = json.loads((ART / "cg_colors.json").read_text())
    for cid, rgb in enumerate(cg[:42]):
        put(shade_sphere(np.array(rgb, np.float32), CELL), cid % 7, cid // 7)

    # shadows (SHADOW_UV 1.5 cells at (0, 6); air shadow 102/64 cells at (4, 6))
    put(soft_blob(int(CELL * 1.5), 0.62), 0, 6)
    put(soft_blob(int(CELL * 102 / 64), 0.62), 4, 6)

    # AIR beads: plain white (tinted by the engine) and dark core with light rim
    put(shade_sphere(np.array([1, 1, 1], np.float32), CELL, ambient=0.5), 2, 6)
    ys, xs = np.mgrid[0:CELL, 0:CELL].astype(np.float32)
    rr = np.sqrt(((xs + .5) / CELL * 2 - 1) ** 2 + ((ys + .5) / CELL * 2 - 1) ** 2)
    core = np.clip((0.62 - rr) / 0.4, 0, 1)
    alb = (1 - core[..., None] * 0.72) * np.ones((1, 1, 3), np.float32)
    put(shade_sphere(alb, CELL, ambient=0.55), 3, 6)
    put(shade_sphere(np.array([1, 1, 1], np.float32), CELL, ambient=0.5), 6, 6)  # SKIN_LESS

    # cursor: a ring with a dot (CURSOR_UV, cell (2,7))
    cur = Image.new("RGBA", (CELL * 4, CELL * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(cur)
    c = CELL * 2
    d.ellipse((c - 1.9 * CELL, c - 1.9 * CELL, c + 1.9 * CELL, c + 1.9 * CELL), outline=(150, 156, 170, 255), width=int(CELL * 0.34))
    d.ellipse((c - 0.12 * CELL, c - 0.12 * CELL, c + 0.12 * CELL, c + 0.12 * CELL), fill=(255, 255, 255, 255))
    put(cur.resize((CELL, CELL), Image.LANCZOS), 2, 7)

    # Dvd beads: nine to a cell, patterned
    sub = CELL // 3
    for kind, name in enumerate(BEAD_NAMES):
        row, col = DVD_BEAD_CELLS[kind // 9]
        tex = bead_texture(kind, name)
        bead = shade_sphere(sample_tex(tex, sub), sub, spec=0.6, ambient=0.4)
        put(bead, col, row, (kind % 3) * sub, ((kind % 9) // 3) * sub)

    # accessories: 8 x 4 cells of 112 px starting at (5, 8)
    acc = json.loads((ART / "accessories.json").read_text())
    ac = CELL // 4
    R = 24.0
    ox, oy = 5 * CELL, 8 * CELL
    for k, e in enumerate(acc):
        im = Image.open(ART / "s" / e["file"]).convert("RGBA")
        w, h, px, py = im.size[0], im.size[1], e["px"], e["py"]
        # size: the extension's own scale (I) mapped through the engine's per-item
        # scale (sc), then shrunk if it would leave the cell around its anchor
        kk = ac * e["I"] / (2 * R * e["sc"]) * 2.0
        kk = min(kk, 1.0)
        nw, nh = max(1, int(w * kk)), max(1, int(h * kk))
        small = im.resize((nw, nh), Image.LANCZOS)
        cell = Image.new("RGBA", (ac, ac), (0, 0, 0, 0))
        _paste_clipped(cell, small, int(ac / 2 - px * kk), int(ac / 2 - py * kk))
        atlas.alpha_composite(cell, (ox + (k % 8) * ac, oy + (k // 8) * ac))
    return atlas


def _paste_clipped(dst, src, x, y):
    layer = Image.new("RGBA", dst.size, (0, 0, 0, 0))
    layer.paste(src, (x, y))
    dst.alpha_composite(layer)
