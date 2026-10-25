"""Check packaged DEX resource constants against the APK resource table.

Run after assembling, before installing:
  python scripts/check_apk_resource_ids.py app.apk --sdk C:/Android
This catches stale R classes that source-level Android unit tests cannot detect.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess


def run(*command):
    return subprocess.check_output(command, text=True, encoding="utf-8", errors="replace")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--package", default="typodev.keyboard")
    args = parser.parse_args()
    windows = os.name == "nt"
    versions = [p for p in (args.sdk / "build-tools").iterdir()
                if (p / ("aapt2.exe" if windows else "aapt2")).is_file()]
    build_tools = max(versions, key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.name)))
    aapt = build_tools / ("aapt2.exe" if windows else "aapt2")
    analyzer = args.sdk / "cmdline-tools/latest/bin" / ("apkanalyzer.bat" if windows else "apkanalyzer")
    resources = {}
    for value, kind, name in re.findall(r"resource (0x7f[0-9a-fA-F]+) (\w+)/([^\s]+)",
                                      run(str(aapt), "dump", "resources", str(args.apk))):
        resources[(kind, name.replace(".", "_"))] = int(value, 16)
    if not resources:
        parser.error("No application resources found in APK")
    packages = run(str(analyzer), "dex", "packages", "--defined-only", str(args.apk))
    # Non-transitive R classes omit resource types contributed only by libraries.
    kinds = re.findall(r"^C[^\n]*\s" + re.escape(args.package) + r"\.R\$(\w+)\s*$",
                       packages, re.MULTILINE)
    kinds = sorted(set(kinds) - {"styleable"})
    if "layout" not in kinds:
        parser.error("No application R$layout class found; use an unminified APK")
    checked = 0
    mismatches = []
    for kind in kinds:
        code = run(str(analyzer), "dex", "code", "--class",
                   args.package + ".R$" + kind, str(args.apk))
        fields = re.findall(r"\.field public static(?: final)? (\w+):I = (0x[0-9a-fA-F]+)", code)
        if not fields:
            raise RuntimeError("No resource constants found for R$" + kind)
        for name, value in fields:
            expected = resources.get((kind, name))
            if expected != int(value, 16):
                mismatches.append(f"{kind}/{name}: DEX={value}, resource table={expected and hex(expected)}")
            checked += 1
    if mismatches:
        print("FAIL: packaged resource IDs do not match:")
        print("\n".join(mismatches))
        return 1
    print(f"PASS: {checked} packaged resource IDs match the APK resource table.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
