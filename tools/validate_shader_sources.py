#!/usr/bin/env python3
"""Compile and link version-specific CrystalConfig shader pairs with glslangValidator.

Requires `glslangValidator` from glslang-tools. This catches GLSL syntax and
interface/linking errors; it does NOT substitute for in-game rendering tests.
"""

from pathlib import Path
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory

MC_VERSIONS = ("26.1", "26.2", "26.3")
SHADER_NAMES = ("msdf_text", "sdf_rect")
ROOT = Path(__file__).resolve().parent.parent


def main() -> int:
    validator = shutil.which("glslangValidator")
    if not validator:
        print("ERROR: glslangValidator not installed (install glslang-tools)", file=sys.stderr)
        return 1

    for version in MC_VERSIONS:
        source_dir = ROOT / "crystal-config" / "src" / "compat" / version / "resources/assets/crystalconfig/shaders/core"
        for name in SHADER_NAMES:
            with TemporaryDirectory(prefix="crystalconfig-glsl-") as dirname:
                tmp = Path(dirname)
                vertex = tmp / f"{name}.vert"
                fragment = tmp / f"{name}.frag"
                shutil.copyfile(source_dir / f"{name}.vsh", vertex)
                shutil.copyfile(source_dir / f"{name}.fsh", fragment)
                print(f"Validating Minecraft {version}: {name} vertex + fragment...", flush=True)
                try:
                    # -l also checks that the vertex outputs match fragment inputs.
                    subprocess.run([validator, "-l", str(vertex), str(fragment)], check=True)
                except subprocess.CalledProcessError as exc:
                    print(f"FAIL: Minecraft {version}: shader {name} ({exc})", file=sys.stderr)
                    return 1
    print("All six CrystalConfig shader programs compiled and linked.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
