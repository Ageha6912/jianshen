import html
import re
import subprocess
import sys
import time

ADB = r"E:\Android\Sdk\platform-tools\adb.exe"


def sh(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, errors="replace")
    if r.returncode != 0:
        print("CMD FAIL:", cmd, (r.stderr or "")[:300], file=sys.stderr)
        sys.exit(1)
    return r.stdout


def dump():
    sh(f"{ADB} shell uiautomator dump /sdcard/ui.xml")
    sh(f"{ADB} pull /sdcard/ui.xml _ui.xml")
    with open("_ui.xml", encoding="utf-8") as f:
        return f.read()


def nodes(xml):
    out = []
    for m in re.finditer(
        r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml
    ):
        t = html.unescape(m.group(1)).strip()
        if t:
            x1, y1, x2, y2 = map(int, m.groups()[1:])
            out.append((t, (x1 + x2) // 2, (y1 + y2) // 2))
    for m in re.finditer(
        r'content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml
    ):
        t = html.unescape(m.group(1)).strip()
        if t:
            x1, y1, x2, y2 = map(int, m.groups()[1:])
            out.append((t, (x1 + x2) // 2, (y1 + y2) // 2))
    return out


def main():
    xml = dump()
    ns = nodes(xml)
    if len(sys.argv) < 2 or sys.argv[1] == "--list":
        for t, x, y in ns:
            print(f"{x},{y} {t}")
        return
    want = sys.argv[1]
    nth = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    hits = [n for n in ns if want in n[0]]
    if not hits:
        print("NOT FOUND:", want)
        print("--- all nodes ---")
        for t, x, y in ns:
            print(f"{x},{y} {t}")
        sys.exit(2)
    if nth >= len(hits):
        print(f"ONLY {len(hits)} HITS")
        sys.exit(3)
    t, x, y = hits[nth]
    print(f"TAP {x},{y} -> {t}")
    sh(f"{ADB} shell input tap {x} {y}")


if __name__ == "__main__":
    main()
