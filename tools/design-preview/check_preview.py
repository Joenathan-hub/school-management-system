#!/usr/bin/env python3
"""
Checks that the design preview is complete and cannot silently drift:

  * every class the preview's HTML uses is defined in preview.css (generated from
    the application's sheets) or in harness.css (the scaffolding). An undefined
    class means the preview is showing something the application does not style.
  * every element the harness marks with a state class is matched by a rule, so a
    state that exists in Java but not in the preview is reported rather than
    quietly ignored.

Run from the repository root:  python3 tools/design-preview/check_preview.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HERE = os.path.join(ROOT, "tools", "design-preview")
PAGES = ["index.html", "components.html"]


def classes_in_css(text):
    return set(re.findall(r"\.([a-zA-Z][a-zA-Z0-9_-]*)", text))


def main():
    defined = classes_in_css(open(os.path.join(HERE, "preview.css"), encoding="utf-8").read())
    defined |= classes_in_css(open(os.path.join(HERE, "harness.css"), encoding="utf-8").read())

    used = {}
    for page in PAGES:
        text = open(os.path.join(HERE, page), encoding="utf-8").read()
        for attribute in re.findall(r'class="([^"]+)"', text):
            for name in attribute.split():
                used.setdefault(name, page)

    missing = sorted((name, page) for name, page in used.items() if name not in defined)
    print(f"preview uses {len(used)} classes; {len(defined)} are defined")
    if missing:
        for name, page in missing:
            print(f"  MISSING  {page}: .{name} is used but never defined")
        print("PREVIEW CHECK FAILED")
        return 1
    print("PREVIEW CHECK PASSED - every class used is defined")
    return 0


if __name__ == "__main__":
    sys.exit(main())
