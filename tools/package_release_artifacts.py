#!/usr/bin/env python3
"""Package and verify loader-specific JARs for a tagged GitHub release."""

import re
from pathlib import Path
import shutil
import sys
import zipfile


ROOT = Path(__file__).resolve().parents[1]
METADATA = {
    "fabric": "fabric.mod.json",
    "forge": "META-INF/mods.toml",
    "neoforge": "META-INF/neoforge.mods.toml",
}


def read_properties(path):
    properties = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def release_version(tag):
    match = re.fullmatch(r"v?(\d+\.\d+\.\d+)", tag)
    if not match:
        raise SystemExit(f"Release tag must contain a semantic version: {tag!r}")
    return match.group(1)


def expected_artifacts(version):
    expected = {}
    for profile in sorted((ROOT / "versions").glob("*.properties")):
        minecraft = profile.stem
        if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", minecraft):
            continue
        properties = read_properties(profile)
        for loader in properties.get("enabled_platforms", "").split(","):
            if loader not in METADATA:
                raise SystemExit(f"Unsupported loader in {profile}: {loader}")
            filename = f"ContingameIME-Neo-{version}-{minecraft}-{loader}.jar"
            artifact = ROOT / loader / "build" / minecraft / "libs" / filename
            expected[filename] = (artifact, loader, minecraft)
    return expected


def validate_jar(path, loader):
    required = METADATA[loader]
    try:
        with zipfile.ZipFile(path) as archive:
            if required not in archive.namelist():
                raise SystemExit(f"{path.name} is missing loader metadata {required}")
            corrupt_entry = archive.testzip()
    except zipfile.BadZipFile as error:
        raise SystemExit(f"Invalid JAR {path.name}: {error}") from None
    if corrupt_entry:
        raise SystemExit(f"Corrupt entry in {path.name}: {corrupt_entry}")


def package(minecraft, tag, output_dir):
    version = release_version(tag)
    root_version = read_properties(ROOT / "gradle.properties").get("mod_version")
    if root_version != version:
        raise SystemExit(
            f"Release tag {tag} does not match gradle.properties mod_version {root_version}"
        )
    profiles = expected_artifacts(version)
    selected = {
        name: details for name, details in profiles.items()
        if details[2] == minecraft
    }
    if not selected:
        raise SystemExit(f"No configured release artifacts for Minecraft {minecraft}")
    output_dir.mkdir(parents=True, exist_ok=True)
    for filename, (artifact, loader, _) in selected.items():
        if not artifact.is_file():
            raise SystemExit(f"Missing built artifact: {artifact}")
        validate_jar(artifact, loader)
        shutil.copy2(artifact, output_dir / filename)
        print(f"Packaged {filename}")


def verify_all(output_dir, tag):
    version = release_version(tag)
    root_version = read_properties(ROOT / "gradle.properties").get("mod_version")
    if root_version != version:
        raise SystemExit(
            f"Release tag {tag} does not match gradle.properties mod_version {root_version}"
        )
    expected = expected_artifacts(version)
    actual = {path.name: path for path in output_dir.glob("*.jar")}
    missing = sorted(expected.keys() - actual.keys())
    extra = sorted(actual.keys() - expected.keys())
    if missing or extra:
        if missing:
            print("Missing JARs: " + ", ".join(missing), file=sys.stderr)
        if extra:
            print("Unexpected JARs: " + ", ".join(extra), file=sys.stderr)
        raise SystemExit("Release asset set does not match the configured build matrix")
    for filename, (_, loader, _) in expected.items():
        validate_jar(actual[filename], loader)
    print(f"Verified {len(actual)} release JARs for {tag}.")


def main(argv):
    if len(argv) == 4 and argv[1] == "--verify-all":
        verify_all(Path(argv[2]), argv[3])
        return
    if len(argv) == 4:
        package(argv[1], argv[2], Path(argv[3]))
        return
    raise SystemExit(
        "usage: package_release_artifacts.py MINECRAFT_VERSION TAG OUTPUT_DIR\n"
        "   or: package_release_artifacts.py --verify-all OUTPUT_DIR TAG"
    )


if __name__ == "__main__":
    main(sys.argv)
