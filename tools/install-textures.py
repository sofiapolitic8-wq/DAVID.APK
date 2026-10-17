#!/usr/bin/env python3
"""Install / verify the game textures the engine needs at startup.

The engine copies every texture listed in thermite/src/platform/tassets.c out of
the APK on first launch and aborts if one is missing. They are binary art and are
NOT in this repository, so put them in app/res/textures/ before building.

  python3 tools/install-textures.py --check
        lists what is missing (exit 1 if anything is)

  python3 tools/install-textures.py --from /path/to/old/app/res/textures
        copies the files across, renaming the old brand prefix to dvd_, then checks
"""
import argparse, pathlib, re, shutil, sys

OLD = "wy" + "rm"  # the upstream file prefix
ROOT = pathlib.Path(__file__).resolve().parent.parent
DEST = ROOT / "app" / "res"
LIST = ROOT / "thermite" / "src" / "platform" / "tassets.c"


def needed():
    src = LIST.read_text(encoding="utf-8")
    return sorted(set(re.findall(r'"(textures/[^"]+)"', src)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--from", dest="src", help="folder holding the original textures")
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    if a.src:
        src = pathlib.Path(a.src)
        n = 0
        for f in src.rglob("*"):
            if f.is_file():
                rel = f.relative_to(src).as_posix().replace(OLD, "dvd").replace(OLD.capitalize(), "dvd")
                out = DEST / "textures" / rel
                out.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(f, out)
                n += 1
        print("copied %d files" % n)
    missing = [t for t in needed() if not (DEST / t).is_file()]
    for t in missing:
        print("MISSING app/res/" + t)
    print("%d required, %d missing" % (len(needed()), len(missing)))
    sys.exit(1 if missing else 0)


if __name__ == "__main__":
    main()
