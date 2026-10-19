"""Regenerates arch-line.json from arch-line.css (the source of truth).

Usage: python3 brand/build-tokens.py
"""

import json
import re
from pathlib import Path

HERE = Path(__file__).parent
css = (HERE / "arch-line.css").read_text(encoding="utf-8")
tokens = {m.group(1): m.group(2).strip() for m in re.finditer(r"--arch-([a-z0-9-]+):\s*([^;]+);", css)}
out = {"name": "arch-line", "version": 1, "tokens": tokens}
(HERE / "arch-line.json").write_text(json.dumps(out, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{len(tokens)} tokens -> arch-line.json")
