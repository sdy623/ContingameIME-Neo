#!/usr/bin/env python3
"""Build one configured Minecraft target without rewriting gradle.properties.

Only profiles with source adapters are accepted. Listing a target describes its
build configuration; Windows launch and IME validation are separate checks.
"""

import argparse
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
PROFILE_DIR = ROOT / "versions"
PROFILE_KEYS = (
    "minecraft_version",
    "minecraft_compat_version",
    "java_version",
    "enabled_platforms",
    "architectury_version",
    "kotlin_version",
    "neoforge_version",
    "forge_version",
    "kotlinforforge_version",
    "fabric_loader_version",
    "fabric_version",
    "fabric_language_kotlin_version",
    "cloth_config_version",
    "rei_version",
    "forge_rei_version",
    "satin_version",
)


def available_versions():
    return sorted(
        (p.stem for p in PROFILE_DIR.glob("*.properties")
         if re.fullmatch(r"\d+\.\d+(?:\.\d+)?", p.stem)),
        key=lambda version: tuple(map(int, version.split("."))),
    )


def load_profile(version):
    if version not in available_versions():
        raise ValueError(
            f"Minecraft {version!r} has no configured source adapter/profile. "
            f"Configured targets: {', '.join(available_versions())}"
        )
    properties = {}
    for line_number, raw in enumerate(
        (PROFILE_DIR / f"{version}.properties").read_text(encoding="utf-8").splitlines(), 1
    ):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if (not separator or key not in PROFILE_KEYS or key in properties
                or not re.fullmatch(r"[A-Za-z0-9.,+_-]+", value)):
            raise ValueError(f"Invalid profile entry: {version}, line {line_number}")
        properties[key] = value
    missing = {
        "minecraft_version", "enabled_platforms", "architectury_version", "kotlin_version",
        "fabric_loader_version", "fabric_language_kotlin_version", "cloth_config_version",
    } - properties.keys()
    platforms = properties.get("enabled_platforms", "").split(",")
    if (not platforms or len(platforms) != len(set(platforms))
            or any(platform not in ("fabric", "forge", "neoforge") for platform in platforms)):
        raise ValueError(f"Unimplemented loader combination in profile {version}")
    if "fabric" in platforms:
        missing |= {"fabric_version", "rei_version"} - properties.keys()
    if "neoforge" in platforms:
        missing |= {"neoforge_version", "kotlinforforge_version", "rei_version"} - properties.keys()
    if "forge" in platforms:
        missing |= {"forge_version", "kotlinforforge_version"} - properties.keys()
    if missing:
        raise ValueError(f"Incomplete profile {version}: missing {', '.join(sorted(missing))}")
    if properties["minecraft_version"] != version:
        raise ValueError(f"Minecraft version in profile {version} does not match its filename")
    compatibility_version = properties.get("minecraft_compat_version", version)
    if compatibility_version not in ("1.20.1", "1.20.6", "1.21.1", "1.21.6", "1.21.9", "1.21.10", "1.21.11"):
        raise ValueError(f"Unimplemented Minecraft API family in profile {version}: {compatibility_version}")
    return properties


def build_command(properties, loader="all", windows=None):
    if loader not in ("all", *properties["enabled_platforms"].split(",")):
        raise ValueError(f"Loader {loader!r} is not configured for this target")
    if windows is None:
        windows = os.name == "nt"
    # Run from ROOT so the batch filename has no path spaces. Profile values are
    # restricted above and cannot contain cmd.exe metacharacters.
    wrapper = ["cmd.exe", "/d", "/c", "gradlew.bat"] if windows else ["bash", "gradlew"]
    task = "build" if loader == "all" else f":{loader}:build"
    selected_properties = dict(properties)
    if loader != "all":
        selected_properties["enabled_platforms"] = loader
    return wrapper + [task, "--no-daemon", "--max-workers=2", "--console=plain"] + [
        f"-P{key}={value}" for key, value in selected_properties.items()
    ]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", nargs="?", help="configured Minecraft version")
    parser.add_argument("--loader", choices=("all", "fabric", "forge", "neoforge"), default="all")
    parser.add_argument("--list", action="store_true", help="list configured targets, not validated support")
    parser.add_argument("--dry-run", action="store_true", help="print the build command without executing")
    args = parser.parse_args(argv)
    if args.list:
        if args.version or args.dry_run or args.loader != "all":
            parser.error("--list cannot be combined with a version or build options")
        for version in available_versions():
            platforms = load_profile(version)["enabled_platforms"].replace(",", ", ")
            print(f"{version}: {platforms} (configured; see MULTIVERSION.md for validation status)")
        return 0
    if not args.version:
        parser.error("specify a Minecraft version or use --list")
    try:
        command = build_command(load_profile(args.version), args.loader)
    except (ValueError, OSError) as error:
        parser.error(str(error))
    if args.dry_run:
        print(subprocess.list2cmdline(command) if os.name == "nt" else shlex.join(command))
        return 0
    try:
        return subprocess.run(command, cwd=ROOT, check=False).returncode
    except OSError as error:
        print(f"Cannot start Gradle: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
