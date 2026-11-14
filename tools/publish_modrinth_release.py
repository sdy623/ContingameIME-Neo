#!/usr/bin/env python3
"""Publish loader-specific JARs from a GitHub release as Modrinth versions."""

import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
import urllib.error
import urllib.request


PROJECT_ID = "5hsGnriG"
API = "https://api.modrinth.com/v2"
ASSET_RE = re.compile(
    r"^ContingameIME-Neo-(?P<release>\d+\.\d+\.\d+)-"
    r"(?P<minecraft>\d+\.\d+(?:\.\d+)?)-"
    r"(?P<loader>fabric|forge|neoforge)\.jar$"
)
REQUIRED_DEPENDENCIES = {
    "fabric": [
        "P7dR8mSH",  # Fabric API
        "Ha28R6CL",  # Fabric Language Kotlin
        "lhGA9TYQ",  # Architectury API
        "9s6osm5g",  # Cloth Config API
    ],
    "forge": [
        "ordsPcFz",  # Kotlin for Forge
        "lhGA9TYQ",  # Architectury API
        "9s6osm5g",  # Cloth Config API
    ],
    "neoforge": [
        "ordsPcFz",  # Kotlin for Forge
        "lhGA9TYQ",  # Architectury API
        "9s6osm5g",  # Cloth Config API
    ],
}


def api_request(method, path, token, body=None, content_type=None):
    headers = {
        "Authorization": token,
        "User-Agent": "sdy623/ContingameIME-Neo release publisher",
        "Accept": "application/json",
    }
    if content_type:
        headers["Content-Type"] = content_type
    request = urllib.request.Request(API + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            payload = response.read()
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")[:1500]
        raise RuntimeError(f"Modrinth API returned HTTP {error.code}: {detail}") from None
    if not payload:
        return None
    return json.loads(payload)


def multipart(fields, filename, file_bytes):
    boundary = "----CodexModrinth" + os.urandom(18).hex()
    chunks = []
    for name, value in fields.items():
        chunks.extend([
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="{name}"\r\n'.encode(),
            b"Content-Type: application/json\r\n\r\n",
            value if isinstance(value, bytes) else str(value).encode(),
            b"\r\n",
        ])
    chunks.extend([
        f"--{boundary}\r\n".encode(),
        b'Content-Disposition: form-data; name="file"; filename="'
        + filename.encode("utf-8")
        + b'"\r\nContent-Type: application/java-archive\r\n\r\n',
        file_bytes,
        b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ])
    return boundary, b"".join(chunks)


def sha512(path):
    digest = hashlib.sha512()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def upload(path, match, token):
    release_version = match.group("release")
    minecraft = match.group("minecraft")
    loader = match.group("loader")
    version_number = f"{release_version}-{minecraft}-{loader}"
    content_hash = sha512(path)

    fields = {
        "data": json.dumps({
            "project_id": PROJECT_ID,
            "name": f"ContingameIME-Neo {release_version} - Minecraft {minecraft} ({loader})",
            "version_number": version_number,
            "changelog": (
                f"ContingameIME-Neo {release_version} for Minecraft {minecraft} "
                f"using {loader}. See the GitHub release for the complete build matrix."
            ),
            "dependencies": [
                {"project_id": project_id, "dependency_type": "required"}
                for project_id in REQUIRED_DEPENDENCIES[loader]
            ],
            "game_versions": [minecraft],
            "loaders": [loader],
            "version_type": "release",
            "featured": False,
            "file_parts": ["file"],
        }, separators=(",", ":")),
    }
    boundary, body = multipart(fields, path.name, path.read_bytes())
    response = api_request(
        "POST", "/version", token, body,
        f"multipart/form-data; boundary={boundary}",
    )
    print(f"Published {version_number}: {path.name} ({content_hash[:16]})")
    return response


def main(argv):
    if len(argv) != 3:
        raise SystemExit("usage: publish_modrinth_release.py ASSET_DIRECTORY RELEASE_TAG")
    asset_dir = Path(argv[1])
    tag = argv[2]
    tag_match = re.fullmatch(r"v?(\d+\.\d+\.\d+)", tag)
    if not tag_match:
        raise SystemExit(f"Release tag must contain a semantic version: {tag!r}")
    release_version = tag_match.group(1)
    token = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not token:
        raise SystemExit(
            "The GitHub Actions secret MODRINTH_TOKEN is unavailable to this workflow. "
            "Add it as a repository or workflow-environment Actions secret, then rerun this workflow."
        )

    assets = []
    for path in sorted(asset_dir.glob("*.jar")):
        match = ASSET_RE.fullmatch(path.name)
        if match and match.group("release") == release_version:
            assets.append((path, match))
    if not assets:
        raise SystemExit(f"No release JARs for {release_version} found in {asset_dir}")

    existing = api_request("GET", f"/project/{PROJECT_ID}/version", token) or []
    by_number = {version["version_number"]: version for version in existing}
    published = skipped = 0
    for path, match in assets:
        number = (
            f"{release_version}-{match.group('minecraft')}-{match.group('loader')}"
        )
        known = by_number.get(number)
        if known:
            expected_hash = sha512(path)
            hashes = {
                file_info.get("hashes", {}).get("sha512")
                for file_info in known.get("files", [])
            }
            if expected_hash not in hashes:
                raise SystemExit(
                    f"Modrinth version {number} already exists with a different JAR; "
                    "refusing to replace it."
                )
            print(f"Already published: {number}")
            skipped += 1
            continue
        upload(path, match, token)
        published += 1
        time.sleep(0.25)
    print(f"Finished: {published} published, {skipped} already present.")


if __name__ == "__main__":
    main(sys.argv)
