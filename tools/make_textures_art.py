"""Arrows, looks, home icons, logo and colour wheel for tools/make-textures.py.

Original vector-style art drawn with PIL at 3x and reduced with a premultiplied
Lanczos (no dark halos). Layouts are fixed by the engine / Kotlin code:
  arrow_skins.png       5 x 4 cells of 256 px, right-facing (android_arrows.c)
  dvd_accessories.png   8 x 8 cells of 256 px (android_look.c, DvdLook.kt)
  home_navigation_icons 4 x 4 cells of 256 px (renderer.c)
"""
import math

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont, ImageOps

from make_textures_core import ROOT, TEX, C, hexc

S = 3


def down(img, size):
    a = np.asarray(img.convert("RGBA"), np.float32)
    al = a[..., 3:4] / 255.0
    a[..., :3] *= al
    p = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").resize((size, size), Image.LANCZOS)
    r = np.asarray(p, np.float32).copy()
    oa = r[..., 3:4]
    nz = oa[..., 0] > 0
    r[..., :3][nz] = np.clip(r[..., :3][nz] * 255.0 / oa[nz], 0, 255)
    return Image.fromarray(r.astype(np.uint8), "RGBA")


# ------------------------------------------------------------ mask utilities
class Cv:
    """A 256-unit cell drawn at S x; shapes are masks shaded into layers."""

    def __init__(self, unit=256):
        self.u = unit
        self.n = unit * S
        self.img = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 0))

    def mask(self):
        return Image.new("L", (self.n, self.n), 0)

    def circle(self, m, cx, cy, r):
        ImageDraw.Draw(m).ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)

    def ellipse(self, m, cx, cy, rx, ry):
        ImageDraw.Draw(m).ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], fill=255)

    def poly(self, m, pts):
        ImageDraw.Draw(m).polygon([(x * S, y * S) for x, y in pts], fill=255)

    def line(self, m, pts, w):
        ImageDraw.Draw(m).line([(x * S, y * S) for x, y in pts], fill=255, width=int(w * S), joint="curve")

    def sub(self, a, b):
        return ImageChops.subtract(a, b)

    def add(self, a, b):
        return ImageChops.lighter(a, b)

    def fill(self, m, light, dark, direction=(-0.5, -0.85), outline=None, ow=1.4, alpha=1.0):
        """Shade mask m with a light->dark gradient across its bounding box."""
        bb = m.getbbox()
        if not bb:
            return
        x0, y0, x1, y1 = bb
        ys, xs = np.mgrid[y0:y1, x0:x1].astype(np.float32)
        gx = (xs - x0) / max(1, x1 - x0) - 0.5
        gy = (ys - y0) / max(1, y1 - y0) - 0.5
        t = np.clip(0.5 - (gx * direction[0] + gy * direction[1]), 0, 1)[..., None]
        lc, dc = np.array(light, np.float32), np.array(dark, np.float32)
        rgb = lc * (1 - t) + dc * t
        layer = np.zeros((self.n, self.n, 4), np.float32)
        layer[y0:y1, x0:x1, :3] = rgb
        layer[..., 3] = np.asarray(m, np.float32) * alpha
        L = Image.fromarray(layer.astype(np.uint8), "RGBA")
        self.img.alpha_composite(L)
        if outline:
            er = m.filter(ImageFilter.MinFilter(max(3, int(ow * S) | 1)))
            edge = ImageChops.subtract(m, er)
            o = Image.new("RGBA", (self.n, self.n), tuple(outline) + (255,))
            o.putalpha(edge)
            self.img.alpha_composite(o)

    def flat(self, m, color, alpha=255):
        o = Image.new("RGBA", (self.n, self.n), tuple(color) + (255,))
        o.putalpha(m.point(lambda v: v * alpha // 255))
        self.img.alpha_composite(o)

    def shadow(self, m, dx=2, dy=3, blur=3, a=90):
        sh = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 255))
        sm = ImageChops.offset(m, int(dx * S), int(dy * S)).filter(ImageFilter.GaussianBlur(blur * S))
        sh.putalpha(sm.point(lambda v: v * a // 255))
        self.img.alpha_composite(sh)

    def out(self):
        return down(self.img, self.u)


def clip_to(m, other):
    return ImageChops.multiply(m, other)


# =============================================================== arrow skins
SHAPES = {
    "dart": [(0.08, 0.08), (0.96, 0.5), (0.08, 0.92), (0.30, 0.5)],
    "dart2": [(0.10, 0.1), (0.95, 0.5), (0.10, 0.9), (0.26, 0.5)],
    "chev": [(0.18, 0.08), (0.44, 0.08), (0.96, 0.5), (0.44, 0.92), (0.18, 0.92), (0.70, 0.5)],
    "tri": [(0.12, 0.10), (0.96, 0.5), (0.12, 0.90)],
    "kite": [(0.07, 0.27), (0.95, 0.5), (0.07, 0.73), (0.03, 0.5)],
    "wing": [(0.03, 0.06), (0.95, 0.5), (0.03, 0.94), (0.26, 0.64), (0.16, 0.5), (0.26, 0.36)],
    "volt": [(0.10, 0.06), (0.62, 0.22), (0.50, 0.32), (0.96, 0.5), (0.50, 0.68), (0.62, 0.78), (0.10, 0.94), (0.32, 0.5)],
    "trap": [(0.08, 0.20), (0.96, 0.5), (0.08, 0.80)],
}
# (shape, top, bottom, outline, glow or None)
ARROWS = [
    ("dart", 0x4A9BFF, 0x1B52B8, 0x0A2A70, None), ("chev", 0x66B8FF, 0x2A66B8, 0x0A3070, None),
    ("dart", 0xF26060, 0x9A1C1C, 0x5A0A0A, None), ("dart", 0xFFD84A, 0xB88A00, 0x6A5000, None),
    ("dart2", 0xFF4848, 0xA81C1C, 0x600000, None),
    ("wing", 0xF4F6FF, 0x9AA0C0, 0x5A6088, None), ("dart", 0xFAFAFC, 0xA0A4AC, 0x606878, None),
    ("chev", 0xF4F6FA, 0x9096A4, 0x606878, None), ("kite", 0xF0F2F6, 0x9AA0AC, 0x606878, None),
    ("tri", 0xA8E8FF, 0x30C0FF, 0x1A9ADA, 0x30C8FF),
    ("tri", 0xFF8AE0, 0xFF30B8, 0xC81898, 0xFF40C0), ("volt", 0xEEFF60, 0xB4E400, 0x7A9A00, 0xC8FF30),
    ("wing", 0xFFA848, 0xD03C00, 0x782000, None), ("dart", 0x70F0E0, 0x18A098, 0x0A6860, None),
    ("chev", 0xFFC060, 0xC05818, 0x782800, None),
    ("kite", 0x70E0A0, 0x2A9060, 0x105838, None), ("dart", 0xF0F2F6, 0x80848E, 0x484C58, None),
    ("double", 0xD8FF50, 0x84E000, 0x4A8A00, None), ("chev", 0xFFA070, 0xB03060, 0x701838, None),
    ("streak", 0xB8D8FF, 0x4A78FF, 0x1A38B8, 0x4A80FF),
]


def arrow_cell(spec):
    shape, top, bot, out, glow = spec
    cv = Cv()
    pts_scale = cv.u
    m = cv.mask()
    def P(pts):
        return [(x * 0.84 * pts_scale + 0.08 * pts_scale, y * 0.84 * pts_scale + 0.08 * pts_scale) for x, y in pts]
    if shape == "double":
        for ox in (0.0, 0.30):
            cv.poly(m, P([(x * 0.7 + ox, y) for x, y in SHAPES["chev"]]))
    elif shape == "streak":
        cv.poly(m, P([(0.40 + x * 0.60, y) for x, y in SHAPES["tri"]]))
        for y in (0.40, 0.62):
            cv.poly(m, P([(0.0, y - 0.04), (0.28, y - 0.04), (0.28, y + 0.04), (0.0, y + 0.04)]))
    else:
        cv.poly(m, P(SHAPES[shape]))
    if glow:
        g = m.filter(ImageFilter.GaussianBlur(7 * S))
        gl = Image.new("RGBA", (cv.n, cv.n), C(glow) + (255,))
        gl.putalpha(g.point(lambda v: min(255, v * 2)))
        cv.img.alpha_composite(gl)
    cv.shadow(m, 0, 4, 4, 70)
    cv.fill(m, C(top), C(bot), direction=(0, -1), outline=C(out), ow=2.4)
    # glossy band across the upper third
    band = cv.mask()
    ImageDraw.Draw(band).rectangle([0, int(0.27 * cv.n), cv.n, int(0.37 * cv.n)], fill=255)
    cv.flat(clip_to(band, m), (255, 255, 255), 105)
    return cv.out()


def make_arrows():
    sheet = Image.new("RGBA", (5 * 256, 4 * 256), (0, 0, 0, 0))
    for i, spec in enumerate(ARROWS):
        sheet.alpha_composite(arrow_cell(spec), ((i % 5) * 256, (i // 5) * 256))
    return sheet


# ================================================================== the looks
HL, HD, HO = (240, 241, 247), (172, 175, 190), (120, 124, 140)


def mirror_pair(cv, build):
    """Draw an ear in the upper half with `build(cv)`, then mirror it to the
    lower half of the cell."""
    build(cv)
    up = cv.img.copy()
    cv.img.alpha_composite(ImageOps.flip(up))


def hair_cell(style):
    cv = Cv()
    R = 256 / 4.4
    hx, hy = 128 + 0.6 * R, 128.0
    m = cv.mask()

    def crescent(ro, shift, ri):
        outer = cv.mask(); cv.circle(outer, hx, hy, ro)
        inner = cv.mask(); cv.circle(inner, hx + shift * R, hy, ri * R)
        return cv.sub(outer, inner), outer

    if style == 0:  # fluffy: scalloped crescent
        m, _ = crescent(1.5 * R, 0.55 * R / R, 1.05)
        for a in np.linspace(math.radians(100), math.radians(260), 13):
            cv.circle(m, hx + math.cos(a) * 1.5 * R, hy + math.sin(a) * 1.5 * R, 0.30 * R)
        m = cv.sub(m, _inner(cv, hx, hy, R, 0.55, 1.05))
    elif style == 1:  # flame: spikes
        m, _ = crescent(1.35 * R, 0.55, 1.05)
        for a in np.linspace(math.radians(95), math.radians(265), 11):
            c, s_ = math.cos(a), math.sin(a)
            cv.poly(m, [(hx + c * 1.2 * R - s_ * 0.2 * R, hy + s_ * 1.2 * R + c * 0.2 * R),
                        (hx + c * 1.95 * R, hy + s_ * 1.95 * R),
                        (hx + c * 1.2 * R + s_ * 0.2 * R, hy + s_ * 1.2 * R - c * 0.2 * R)])
        m = cv.sub(m, _inner(cv, hx, hy, R, 0.55, 1.05))
    elif style == 2:  # pom: cloud of circles
        for a in np.linspace(math.radians(95), math.radians(265), 9):
            cv.circle(m, hx + math.cos(a) * 1.18 * R, hy + math.sin(a) * 1.18 * R, 0.52 * R)
        m = cv.sub(m, _inner(cv, hx, hy, R, 0.5, 0.95))
    elif style == 3:  # dreadlocks: wavy strands
        for k, a in enumerate(np.linspace(math.radians(85), math.radians(275), 17)):
            pts = []
            for t in np.linspace(0.85, 1.75, 8):
                w = math.sin(t * 7 + k) * 0.08
                pts.append((hx + math.cos(a + w) * t * R, hy + math.sin(a + w) * t * R))
            cv.line(m, pts, 0.17 * R)
        base, _ = crescent(1.25 * R, 0.5, 1.0)
        m = cv.add(m, base)
        m = cv.sub(m, _inner(cv, hx, hy, R, 0.5, 1.0))
    elif style == 4:  # mohawk: jagged ridge along the head's axis
        pts_top, pts_bot = [], []
        n = 12
        for k in range(n + 1):
            x = hx - 1.25 * R + k * (2.2 * R) / n
            w = (0.34 if k % 2 == 0 else 0.2) * R
            pts_top.append((x, hy - w)); pts_bot.append((x, hy + w))
        cv.poly(m, pts_top + pts_bot[::-1])
    elif style in (5, 6, 7, 8):  # tied styles: a close cap + the tie
        m, _ = crescent(1.3 * R, 0.5, 1.0)
        roots = {5: [(-0.9, 0)], 6: [(-0.55, -0.8), (-0.55, 0.8)], 7: [(-0.95, 0)], 8: [(-0.6, 0)]}[style]
        for rx, ry in roots:
            cv.circle(m, hx + rx * R, hy + ry * R, 0.26 * R)
    elif style == 9:  # space buns
        m, _ = crescent(1.2 * R, 0.45, 0.95)
        for sgn in (-1, 1):
            cv.circle(m, hx - 0.2 * R, hy + sgn * 1.3 * R, 0.5 * R)
    elif style == 10:  # man bun
        m, _ = crescent(1.2 * R, 0.45, 0.95)
        cv.circle(m, hx - 1.45 * R, hy, 0.55 * R)
    else:  # 11 bob
        m, _ = crescent(1.65 * R, 0.5, 1.05)
        flat = cv.mask(); cv.poly(flat, [(0, 0), (256, 0), (256, 256), (0, 256)])
    cv.shadow(m, 2, 3, 3, 90)
    cv.fill(m, HL, HD, outline=HO, ow=1.2)
    # a few strand lines to read as hair
    lines = cv.mask()
    for k in range(9):
        a = math.radians(95 + k * 21)
        cv.line(lines, [(hx + math.cos(a) * 0.9 * R, hy + math.sin(a) * 0.9 * R), (hx + math.cos(a) * 1.45 * R, hy + math.sin(a) * 1.45 * R)], 0.5)
    cv.flat(clip_to(lines, m), HO, 110)
    return cv.out()


def _inner(cv, hx, hy, R, shift, ri):
    inner = cv.mask(); cv.circle(inner, hx + shift * R, hy, ri * R)
    return inner


def flow_cell(style):
    cv = Cv()
    m = cv.mask()
    cx, cy = 128, 128
    if style == 12:  # ponytail: a lens
        pts_t = [(8 + k * 240 / 24, cy - 40 * math.sin(math.pi * k / 24) ** 0.8) for k in range(25)]
        cv.poly(m, pts_t + [(x, 2 * cy - y) for x, y in pts_t[::-1]])
    elif style == 13:  # pigtail
        pts_t = [(8 + k * 240 / 24, cy - 28 * math.sin(math.pi * k / 24) ** 0.8) for k in range(25)]
        cv.poly(m, pts_t + [(x, 2 * cy - y) for x, y in pts_t[::-1]])
    elif style == 14:  # braid: chain of beads
        for k in range(9):
            cv.circle(m, 22 + k * 26.5, cy + (4 if k % 2 else -4), 17 - k * 0.5)
    else:  # 15 long hair: a wide tapering band
        pts_t = [(4 + k * 248 / 24, cy - (52 - 14 * k / 24) * (0.25 + 0.75 * math.sin(math.pi * (0.1 + 0.9 * k / 24)))) for k in range(25)]
        cv.poly(m, pts_t + [(x, 2 * cy - y) for x, y in pts_t[::-1]])
    cv.shadow(m, 0, 3, 3, 80)
    cv.fill(m, HL, HD, direction=(0, -1), outline=HO, ow=1.2)
    lines = cv.mask()
    for k in range(-3, 4):
        cv.line(lines, [(10, cy + k * 11), (246, cy + k * 11)], 0.8)
    cv.flat(clip_to(lines, m), HO, 100)
    return cv.out()


def ear_cell(style):
    cv = Cv()
    R = 64.0
    x0, y0 = 120.0, 128 - 0.85 * R

    def build(cv):
        m = cv.mask(); inner = cv.mask()
        if style == 0:  # panda
            cv.circle(m, x0, y0, 0.46 * R); cv.circle(inner, x0, y0, 0.24 * R)
            cv.shadow(m); cv.fill(m, (70, 70, 78), (20, 20, 24), outline=(10, 10, 12)); cv.fill(inner, (120, 120, 130), (60, 60, 68))
        elif style == 1:  # bunny
            cv.poly(m, [(x0 - 0.2 * R, y0 + 0.4 * R), (x0 + 0.2 * R, y0 + 0.4 * R), (x0 + 0.34 * R, y0 - 0.5 * R), (x0 - 0.34 * R, y0 - 0.5 * R)])
            cv.poly(inner, [(x0 - 0.1 * R, y0 + 0.3 * R), (x0 + 0.1 * R, y0 + 0.3 * R), (x0 + 0.17 * R, y0 - 0.38 * R), (x0 - 0.17 * R, y0 - 0.38 * R)])
            cv.shadow(m); cv.fill(m, (255, 255, 255), (222, 222, 232), outline=(150, 150, 165)); cv.fill(inner, (255, 190, 215), (240, 130, 175))
        elif style == 2:  # lop bunny
            ang = math.radians(-65)
            def rot(px, py): return (x0 + px * math.cos(ang) - py * math.sin(ang), y0 + px * math.sin(ang) + py * math.cos(ang))
            cv.poly(m, [rot(-0.5 * R, -0.2 * R), rot(0.6 * R, -0.28 * R), rot(0.62 * R, 0.28 * R), rot(-0.5 * R, 0.2 * R)])
            cv.poly(inner, [rot(-0.4 * R, -0.08 * R), rot(0.5 * R, -0.14 * R), rot(0.5 * R, 0.14 * R), rot(-0.4 * R, 0.08 * R)])
            cv.shadow(m); cv.fill(m, (255, 255, 255), (222, 222, 232), outline=(150, 150, 165)); cv.fill(inner, (255, 190, 215), (240, 130, 175))
        elif style in (3, 7, 8, 10):  # cat, fox, wolf, bat: triangles
            col = {3: ((255, 150, 60), (210, 90, 10), (150, 60, 0), (255, 170, 190)),
                   7: ((255, 140, 50), (200, 80, 10), (120, 50, 0), (255, 240, 225)),
                   8: ((190, 194, 206), (110, 114, 128), (70, 74, 88), (255, 190, 205)),
                   10: ((90, 70, 130), (40, 28, 70), (20, 12, 40), (170, 90, 210))}[style]
            w = 0.5 if style != 10 else 0.34
            h = 0.6 if style != 10 else 0.8
            cv.poly(m, [(x0 - w * R, y0 + 0.4 * R), (x0 + w * R, y0 + 0.4 * R), (x0 + 0.05 * R, y0 - h * R)])
            cv.poly(inner, [(x0 - w * 0.55 * R, y0 + 0.3 * R), (x0 + w * 0.55 * R, y0 + 0.3 * R), (x0 + 0.04 * R, y0 - h * 0.55 * R)])
            cv.shadow(m); cv.fill(m, col[0], col[1], outline=col[2]); cv.fill(inner, col[3], tuple(int(c * 0.8) for c in col[3]))
        elif style in (4, 5, 6, 9):  # mouse, bear, koala, tiger: round
            rim, rim_d, ink, face = {4: ((190, 190, 200), (130, 130, 145), (90, 90, 105), (255, 180, 200)),
                                     5: ((170, 112, 66), (110, 66, 34), (70, 40, 20), (214, 160, 110)),
                                     6: ((176, 178, 190), (120, 122, 136), (80, 82, 96), (250, 250, 252)),
                                     9: ((255, 196, 70), (220, 140, 20), (140, 80, 0), (255, 235, 190))}[style]
            r = {4: 0.55, 5: 0.42, 6: 0.55, 9: 0.44}[style]
            cv.circle(m, x0, y0, r * R); cv.circle(inner, x0, y0, r * 0.58 * R)
            cv.shadow(m); cv.fill(m, rim, rim_d, outline=ink); cv.fill(inner, face, tuple(int(c * 0.85) for c in face))
            if style == 9:
                st = cv.mask(); cv.line(st, [(x0 - 0.3 * R, y0 - 0.35 * R), (x0 - 0.1 * R, y0 - 0.12 * R)], 0.12 * R); cv.line(st, [(x0 + 0.3 * R, y0 - 0.35 * R), (x0 + 0.1 * R, y0 - 0.12 * R)], 0.12 * R)
                cv.flat(clip_to(st, m), (90, 40, 0), 220)
        else:  # 11 dragon: a curved horn
            pts = [(x0 - 0.3 * R, y0 + 0.45 * R), (x0 - 0.05 * R, y0 + 0.1 * R), (x0 + 0.2 * R, y0 - 0.25 * R), (x0 + 0.5 * R, y0 - 0.6 * R)]
            cv.line(m, pts, 0.3 * R)
            cv.shadow(m); cv.fill(m, (90, 220, 110), (20, 130, 50), outline=(10, 70, 30))
            tip = cv.mask(); cv.circle(tip, pts[-1][0], pts[-1][1], 0.2 * R)
            cv.fill(tip, (255, 120, 220), (190, 40, 160))

    mirror_pair(cv, build)
    return cv.out()


def glasses_cell(style):
    cv = Cv()
    R = 64.0
    cx = 128 + 0.5 * R
    dy = 0.55 * R
    arm = cv.mask()
    for sgn in (-1, 1):
        cv.line(arm, [(cx - 0.05 * R, 128 + sgn * (dy + 0.25 * R)), (cx - 1.5 * R, 128 + sgn * (dy + 0.9 * R))], 1.6)
    cv.flat(arm, (130, 136, 150), 220)

    def pair(draw_lens):
        for sgn in (-1, 1):
            draw_lens(cx, 128 + sgn * dy, sgn)

    def lens(m, x, y, rx, ry):
        cv.ellipse(m, x, y, rx, ry)

    if style == 0:  # heart, its point facing forward
        def d(x, y, s):
            m = cv.mask(); P = []
            for k in range(60):
                t = k / 60 * 2 * math.pi
                a_ = 16 * math.sin(t) ** 3 / 17 * 0.5 * R
                b_ = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)) / 17 * 0.5 * R
                P.append((x + b_, y - a_))
            cv.poly(m, P)
            cv.shadow(m); cv.fill(m, (255, 120, 160), (220, 40, 100), outline=(150, 20, 70))
        pair(d)
    elif style == 1:  # cat-eye
        def d(x, y, s):
            m = cv.mask()
            cv.poly(m, [(x - 0.35 * R, y - s * 0.42 * R), (x + 0.45 * R, y + s * 0.05 * R), (x + 0.3 * R, y + s * 0.38 * R), (x - 0.4 * R, y + s * 0.2 * R)])
            cv.shadow(m); cv.fill(m, (60, 56, 80), (18, 16, 30), outline=(8, 8, 14))
        pair(d)
    elif style == 2:  # flower
        def d(x, y, s):
            m = cv.mask()
            for a in range(5):
                ang = a * 2 * math.pi / 5
                cv.circle(m, x + math.cos(ang) * 0.28 * R, y + math.sin(ang) * 0.28 * R, 0.24 * R)
            cv.shadow(m); cv.fill(m, (255, 160, 200), (230, 90, 150), outline=(170, 50, 110))
            c = cv.mask(); cv.circle(c, x, y, 0.16 * R); cv.fill(c, (255, 240, 120), (240, 190, 40))
        pair(d)
    elif style == 3:  # pastel round
        def d(x, y, s):
            m = cv.mask(); cv.circle(m, x, y, 0.5 * R); i = cv.mask(); cv.circle(i, x, y, 0.4 * R)
            cv.shadow(m); cv.fill(m, (250, 190, 230), (210, 140, 200), outline=(160, 90, 150)); cv.fill(i, (170, 190, 220), (120, 150, 190))
        pair(d)
    elif style == 4:  # nerd squares
        def d(x, y, s):
            m = cv.mask(); i = cv.mask()
            cv.poly(m, [(x - 0.45 * R, y - 0.45 * R), (x + 0.45 * R, y - 0.45 * R), (x + 0.45 * R, y + 0.45 * R), (x - 0.45 * R, y + 0.45 * R)])
            cv.poly(i, [(x - 0.35 * R, y - 0.35 * R), (x + 0.35 * R, y - 0.35 * R), (x + 0.35 * R, y + 0.35 * R), (x - 0.35 * R, y + 0.35 * R)])
            cv.shadow(m); cv.fill(m, (70, 80, 100), (20, 24, 34), outline=(8, 10, 16)); cv.fill(i, (170, 190, 215), (110, 130, 160))
        pair(d)
    elif style == 5:  # sparkle stars
        def d(x, y, s):
            m = cv.mask(); P = []
            for k in range(10):
                rr = 0.58 * R if k % 2 == 0 else 0.26 * R
                a = -math.pi / 2 + math.pi * k / 5
                P.append((x + rr * math.cos(a), y + rr * math.sin(a)))
            cv.poly(m, P); cv.shadow(m); cv.fill(m, (255, 240, 130), (235, 190, 50), outline=(160, 120, 10))
        pair(d)
    elif style == 6:  # aviator
        def d(x, y, s):
            m = cv.mask()
            cv.poly(m, [(x - 0.4 * R, y - s * 0.5 * R), (x + 0.3 * R, y - s * 0.45 * R), (x + 0.45 * R, y + s * 0.25 * R), (x - 0.3 * R, y + s * 0.42 * R)])
            cv.shadow(m); cv.fill(m, (70, 90, 70), (24, 34, 26), outline=(10, 16, 10))
        pair(d)
    elif style == 7:  # pixel
        def d(x, y, s):
            m = cv.mask()
            cv.poly(m, [(x - 0.42 * R, y - 0.42 * R), (x + 0.42 * R, y - 0.42 * R), (x + 0.42 * R, y + 0.42 * R), (x - 0.42 * R, y + 0.42 * R)])
            cv.shadow(m); cv.fill(m, (30, 30, 34), (4, 4, 6))
            w = cv.mask(); cv.poly(w, [(x + 0.12 * R, y - 0.34 * R), (x + 0.3 * R, y - 0.34 * R), (x + 0.3 * R, y - 0.2 * R), (x + 0.12 * R, y - 0.2 * R)])
            cv.flat(w, (255, 255, 255))
        pair(d)
    elif style in (8, 9):  # visors
        m = cv.mask()
        cv.line(m, [(cx, 128 - dy - 0.2 * R), (cx + 0.15 * R, 128), (cx, 128 + dy + 0.2 * R)], 0.55 * R)
        col = ((90, 240, 255), (10, 170, 210), (0, 90, 120)) if style == 8 else ((255, 90, 90), (190, 20, 30), (100, 0, 10))
        cv.shadow(m); cv.fill(m, col[0], col[1], outline=col[2], ow=1.8)
        g = cv.mask(); cv.line(g, [(cx + 0.02 * R, 128 - dy), (cx + 0.1 * R, 128), (cx + 0.02 * R, 128 + dy)], 0.12 * R)
        cv.flat(clip_to(g, m), (255, 255, 255), 150)
    elif style == 10:  # steampunk
        def d(x, y, s):
            m = cv.mask(); i = cv.mask(); cv.circle(m, x, y, 0.5 * R); cv.circle(i, x, y, 0.36 * R)
            cv.shadow(m); cv.fill(m, (240, 190, 90), (150, 90, 20), outline=(90, 50, 5)); cv.fill(i, (150, 200, 220), (70, 120, 150))
        pair(d)
    else:  # 11 punk: spiked rings
        def d(x, y, s):
            m = cv.mask()
            for a in range(10):
                ang = a * 2 * math.pi / 10
                cv.poly(m, [(x + math.cos(ang - 0.18) * 0.4 * R, y + math.sin(ang - 0.18) * 0.4 * R), (x + math.cos(ang) * 0.68 * R, y + math.sin(ang) * 0.68 * R), (x + math.cos(ang + 0.18) * 0.4 * R, y + math.sin(ang + 0.18) * 0.4 * R)])
            cv.circle(m, x, y, 0.42 * R)
            cv.shadow(m); cv.fill(m, (225, 228, 235), (120, 124, 136), outline=(60, 62, 72))
            i = cv.mask(); cv.circle(i, x, y, 0.3 * R); cv.fill(i, (40, 40, 46), (4, 4, 8))
        pair(d)
    return cv.out()


def make_looks():
    sheet = Image.new("RGBA", (2048, 2048), (0, 0, 0, 0))
    cells = [hair_cell(i) for i in range(12)]
    cells += [flow_cell(i) for i in (12, 13, 14, 15)]
    cells += [ear_cell(i) for i in range(12)]
    cells += [glasses_cell(i) for i in range(12)]
    for i, c in enumerate(cells):
        sheet.alpha_composite(c, ((i % 8) * 256, (i // 8) * 256))
    return sheet


# ============================================================ icons / logo
def make_icons():
    """4 x 4 grid of 256 px white glyphs (tinted by the UI)."""
    sheet = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    glyphs = ["home", "play", "skin", "gear", "team", "trophy", "chat", "map", "eye", "bolt", "star", "heart", "plus", "check", "close", "info"]
    for i, g in enumerate(glyphs):
        cv = Cv()
        m = cv.mask()
        if g == "home":
            cv.poly(m, [(128, 40), (216, 120), (196, 120), (196, 210), (60, 210), (60, 120), (40, 120)])
        elif g == "play":
            cv.poly(m, [(84, 52), (210, 128), (84, 204)])
        elif g == "skin":
            cv.circle(m, 128, 80, 46); cv.poly(m, [(60, 210), (70, 150), (186, 150), (196, 210)])
        elif g == "gear":
            cv.circle(m, 128, 128, 70)
            for a in range(8):
                ang = a * math.pi / 4
                cv.poly(m, [(128 + math.cos(ang - 0.2) * 60, 128 + math.sin(ang - 0.2) * 60), (128 + math.cos(ang - 0.14) * 100, 128 + math.sin(ang - 0.14) * 100), (128 + math.cos(ang + 0.14) * 100, 128 + math.sin(ang + 0.14) * 100), (128 + math.cos(ang + 0.2) * 60, 128 + math.sin(ang + 0.2) * 60)])
            hole = cv.mask(); cv.circle(hole, 128, 128, 30); m = cv.sub(m, hole)
        elif g == "team":
            for x in (88, 168):
                cv.circle(m, x, 90, 32); cv.poly(m, [(x - 52, 190), (x - 40, 130), (x + 40, 130), (x + 52, 190)])
        elif g == "trophy":
            cv.poly(m, [(76, 50), (180, 50), (172, 120), (128, 150), (84, 120)]); cv.poly(m, [(116, 150), (140, 150), (144, 190), (112, 190)]); cv.poly(m, [(84, 190), (172, 190), (172, 210), (84, 210)])
        elif g == "chat":
            cv.poly(m, [(50, 60), (206, 60), (206, 160), (120, 160), (80, 200), (84, 160), (50, 160)])
        elif g == "map":
            cv.poly(m, [(50, 70), (100, 55), (156, 75), (206, 58), (206, 186), (156, 203), (100, 183), (50, 198)])
        elif g == "eye":
            cv.ellipse(m, 128, 128, 88, 52); hole = cv.mask(); cv.circle(hole, 128, 128, 30); m = cv.sub(m, hole); cv.circle(m, 128, 128, 14)
        elif g == "bolt":
            cv.poly(m, [(150, 30), (70, 140), (118, 140), (100, 226), (190, 106), (140, 106)])
        elif g == "star":
            P = []
            for k in range(10):
                rr = 96 if k % 2 == 0 else 40; a = -math.pi / 2 + math.pi * k / 5
                P.append((128 + rr * math.cos(a), 132 + rr * math.sin(a)))
            cv.poly(m, P)
        elif g == "heart":
            P = []
            for k in range(80):
                t = k / 80 * 2 * math.pi
                P.append((128 + 16 * math.sin(t) ** 3 * 5.2, 124 - (13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)) * 5.2))
            cv.poly(m, P)
        elif g == "plus":
            cv.poly(m, [(112, 50), (144, 50), (144, 112), (206, 112), (206, 144), (144, 144), (144, 206), (112, 206), (112, 144), (50, 144), (50, 112), (112, 112)])
        elif g == "check":
            cv.line(m, [(60, 136), (108, 184), (200, 80)], 26)
        elif g == "close":
            cv.line(m, [(64, 64), (192, 192)], 26); cv.line(m, [(192, 64), (64, 192)], 26)
        else:  # info
            cv.circle(m, 128, 128, 90); hole = cv.mask(); cv.circle(hole, 128, 80, 12); cv.poly(hole, [(118, 108), (138, 108), (138, 176), (118, 176)]); m = cv.sub(m, hole)
        cv.flat(m, (255, 255, 255))
        sheet.alpha_composite(cv.out(), ((i % 4) * 256, (i // 4) * 256))
    return sheet


def make_logo():
    """DAVID.IO wordmark, white, 1024 x 256, transparent."""
    font_path = ROOT / "app" / "res" / "fonts" / "nunito_black.ttf"
    font = ImageFont.truetype(str(font_path), 150 * S) if font_path.is_file() else ImageFont.load_default()
    img = Image.new("RGBA", (1024 * S, 256 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    text = "DAVID.IO"
    bb = d.textbbox((0, 0), text, font=font)
    x = (1024 * S - (bb[2] - bb[0])) // 2 - bb[0]
    y = (256 * S - (bb[3] - bb[1])) // 2 - bb[1]
    d.text((x, y), "DAVID", font=font, fill=(255, 255, 255, 255))
    wd = d.textlength("DAVID", font=font)
    d.text((x + wd, y), ".IO", font=font, fill=(110, 200, 255, 255))
    a = np.asarray(img, np.float32)
    al = a[..., 3:4] / 255.0
    a[..., :3] *= al
    p = Image.fromarray(a.astype(np.uint8), "RGBA").resize((1024, 256), Image.LANCZOS)
    r = np.asarray(p, np.float32).copy()
    nz = r[..., 3] > 0
    r[..., :3][nz] = np.clip(r[..., :3][nz] * 255 / r[..., 3:4][nz], 0, 255)
    return Image.fromarray(r.astype(np.uint8), "RGBA")


def make_wheel(size=768):
    """Hue by angle, saturation by radius, full value; soft outer edge."""
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    u = (xs + 0.5) / size * 2 - 1
    v = (ys + 0.5) / size * 2 - 1
    r = np.sqrt(u * u + v * v)
    h = (np.arctan2(v, u) / (2 * np.pi)) % 1.0
    s = np.clip(r, 0, 1)
    i = np.floor(h * 6).astype(int) % 6
    f = h * 6 - np.floor(h * 6)
    p, q, t = 1 - s, 1 - s * f, 1 - s * (1 - f)
    R = np.choose(i, [np.ones_like(s), q, p, p, t, np.ones_like(s)])
    G = np.choose(i, [t, np.ones_like(s), np.ones_like(s), q, p, p])
    B = np.choose(i, [p, p, t, np.ones_like(s), np.ones_like(s), q])
    a = np.clip((1 - r) * size / 2.0, 0, 1)
    rgba = np.dstack([R, G, B, a])
    return Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA")
