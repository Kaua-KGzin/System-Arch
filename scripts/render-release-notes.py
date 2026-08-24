#!/usr/bin/env python3
"""Renders .github/release-notes-template.md, substituting {VERSION} and
{CHANGES}. Used by .github/workflows/release.yml — kept as a standalone
script (rather than inline in the workflow YAML) so it can be indented
normally and isn't at the mercy of YAML block-scalar indentation rules.

Usage: render-release-notes.py <template> <version> <changes-file> <output>
"""
import pathlib
import sys


def main() -> None:
    template_path, version, changes_path, output_path = sys.argv[1:5]

    changes = pathlib.Path(changes_path).read_text().strip()
    if not changes:
        changes = "_Sem notas para esta versao._"

    notes = pathlib.Path(template_path).read_text()
    notes = notes.replace("{VERSION}", version).replace("{CHANGES}", changes)

    pathlib.Path(output_path).write_text(notes)


if __name__ == "__main__":
    main()
