#!/usr/bin/env python3
import json
import shutil
import struct
import zlib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
dist = root / "dist"
apk = dist / "HotspotDevices-v1.0.0.apk"
package_apk = dist / "uk.co.divadiow.hotspotdevices.apk"
icon = dist / "icon.png"
xapk = dist / "HotspotDevices-v1.0.0.xapk"


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def make_icon(path: Path) -> None:
    size = 256
    blue = (13, 116, 206, 255)
    white = (255, 255, 255, 255)
    pixels = [[blue for _ in range(size)] for _ in range(size)]

    def circle(cx: int, cy: int, radius: int) -> None:
        r2 = radius * radius
        for y in range(max(0, cy - radius), min(size, cy + radius + 1)):
            for x in range(max(0, cx - radius), min(size, cx + radius + 1)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r2:
                    pixels[y][x] = white

    def rect(x1: int, y1: int, x2: int, y2: int) -> None:
        for y in range(max(0, y1), min(size, y2)):
            for x in range(max(0, x1), min(size, x2)):
                pixels[y][x] = white

    circle(128, 74, 24)
    circle(72, 166, 21)
    circle(184, 166, 21)
    rect(120, 96, 136, 140)
    rect(72, 130, 184, 146)
    rect(64, 140, 80, 154)
    rect(176, 140, 192, 154)

    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels)
    png = b"\x89PNG\r\n\x1a\n"
    png += png_chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += png_chunk(b"IDAT", zlib.compress(raw, 9))
    png += png_chunk(b"IEND", b"")
    path.write_bytes(png)


if not apk.exists():
    raise SystemExit(f"Missing {apk}")
shutil.copy2(apk, package_apk)
make_icon(icon)

manifest = {
    "xapk_version": 2,
    "package_name": "uk.co.divadiow.hotspotdevices",
    "name": "Hotspot Devices",
    "version_code": "1",
    "version_name": "1.0.0",
    "min_sdk_version": "26",
    "target_sdk_version": "36",
    "permissions": [
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE"
    ],
    "total_size": package_apk.stat().st_size,
    "split_configs": [],
    "split_apks": [
        {"file": package_apk.name, "id": "base"}
    ]
}

with zipfile.ZipFile(xapk, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    archive.writestr("manifest.json", json.dumps(manifest, indent=2) + "\n")
    archive.write(package_apk, package_apk.name)
    archive.write(icon, "icon.png")

package_apk.unlink(missing_ok=True)
icon.unlink(missing_ok=True)
print(xapk)
