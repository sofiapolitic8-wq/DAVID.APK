#!/usr/bin/env python3
"""Generate every texture the engine needs that is not in the repository.

    python3 tools/make-textures.py            # everything
    python3 tools/make-textures.py atlas arrows looks icons logo wheel tags backgrounds

All art is original/procedural except the david.io extension's own images
(tools/ext-art: 9 floor jpgs and the 32 accessory webps), which are the
owner's. Output goes to app/res/textures/. Then run:
    python3 tools/install-textures.py --check
"""
import pathlib
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import make_textures_core as core  # noqa: E402
import make_textures_art as art    # noqa: E402
from PIL import Image              # noqa: E402

TEX = core.TEX


def save(img, name):
    path = TEX / name
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", path.relative_to(core.ROOT), img.size)


def backgrounds():
    bg = TEX / "backgrounds"
    bg.mkdir(parents=True, exist_ok=True)
    # the nine floors of the david.io extension, copied byte for byte (never re-encoded)
    for f in ("bg45", "bg54", "dd", "greenlava", "grid", "mars", "metalgrid", "noise", "redlava"):
        (bg / (f + ".jpg")).write_bytes((core.ART / "bg" / (f + ".jpg")).read_bytes())
    old = TEX / "background_4k.png"
    if old.exists():
        old.unlink()
    subprocess.check_call([sys.executable, str(HERE / "generate-focus-backgrounds.py")], stdout=subprocess.DEVNULL)
    print("backgrounds: 9 extension jpgs (identical to david.io) + 7 dvd_* tiles")


STEPS = {
    "backgrounds": backgrounds,
    "atlas": lambda: save(core.make_atlas(), "tex_atlas_8k.png"),
    "arrows": lambda: save(art.make_arrows(), "arrow_skins.png"),
    "looks": lambda: save(art.make_looks(), "dvd_accessories.png"),
    "icons": lambda: save(art.make_icons(), "home_navigation_icons.png"),
    "logo": lambda: save(art.make_logo(), "dvd_logo.png"),
    "wheel": lambda: save(art.make_wheel(), "air_colour_wheel.png"),
    "tags": lambda: subprocess.check_call([sys.executable, str(HERE / "dvd-tag-append.py")]),
}

if __name__ == "__main__":
    names = sys.argv[1:] or list(STEPS)
    for n in names:
        STEPS[n]()
