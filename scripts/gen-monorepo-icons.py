#!/usr/bin/env python3
"""Generates the 16x16 folder icons of less-monorepo-plugin (light + _dark variants).

Each icon is a folder tinted with the ecosystem's colour plus a small glyph on the front flap.
Usage: python3 scripts/gen-monorepo-icons.py   (rewrites less-monorepo-plugin/src/main/resources/icons/monorepo/)
"""
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "less-monorepo-plugin", "src", "main", "resources", "icons", "monorepo")

W = "#FFFFFF"

# name: (light colour, dark colour, glyph svg using {fg} for the glyph colour, optional (light fg, dark fg))
GLYPHS = {
    # build graph: one root node feeding two tasks
    "gradle": ("#1C9BB8", "#35B4CF",
               '<path d="M8 8.2 L5.8 11.8 M8 8.2 L10.2 11.8" stroke="{fg}" stroke-width="1" fill="none"/>'
               '<circle cx="8" cy="8" r="1.2" fill="{fg}"/><circle cx="5.6" cy="12" r="1.2" fill="{fg}"/>'
               '<circle cx="10.4" cy="12" r="1.2" fill="{fg}"/>'),
    "maven": ("#C71A36", "#E0475F",
              '<path d="M5 12.8 V7.6 L8 10.6 L11 7.6 V12.8" stroke="{fg}" stroke-width="1.4" fill="none" '
              'stroke-linecap="round" stroke-linejoin="round"/>'),
    "spring": ("#5FA134", "#77BE48",
               '<path d="M11.3 7.1 C7 6.9 4.7 9 5.1 12.9 C9 13.1 11.5 10.8 11.3 7.1 Z" fill="{fg}"/>'
               '<path d="M5.7 12.4 L9.2 9.4" stroke="{c}" stroke-width="0.9" stroke-linecap="round"/>'),
    "node": ("#4E8E3F", "#6DB35A",
             '<path d="M8 7 L10.6 8.5 V11.5 L8 13 L5.4 11.5 V8.5 Z" stroke="{fg}" stroke-width="1.2" fill="none" '
             'stroke-linejoin="round"/>'),
    "react": ("#1590BA", "#4FC3DF",
              '<g stroke="{fg}" stroke-width="0.8" fill="none">'
              '<ellipse cx="8" cy="10" rx="3.5" ry="1.35"/>'
              '<ellipse cx="8" cy="10" rx="3.5" ry="1.35" transform="rotate(60 8 10)"/>'
              '<ellipse cx="8" cy="10" rx="3.5" ry="1.35" transform="rotate(120 8 10)"/></g>'
              '<circle cx="8" cy="10" r="0.8" fill="{fg}"/>'),
    "next": ("#1E1E1E", "#DADADA",
             '<path d="M5.6 12.8 V7.4 L10.4 12.8 V7.4" stroke="{fg}" stroke-width="1.4" fill="none" '
             'stroke-linecap="round" stroke-linejoin="round"/>', (W, "#1E1E1E")),
    "vue": ("#3AA876", "#4FC08D",
            '<path d="M4.8 7.4 L8 12.9 L11.2 7.4" stroke="{fg}" stroke-width="1.5" fill="none" '
            'stroke-linecap="round" stroke-linejoin="round"/>'
            '<path d="M6.7 7.4 L8 9.7 L9.3 7.4" stroke="#35495E" stroke-width="1" fill="none" '
            'stroke-linecap="round" stroke-linejoin="round"/>'),
    "angular": ("#D2002F", "#EE3D5F",
                '<path d="M5.2 12.8 L8 7.2 L10.8 12.8 M6.3 10.8 H9.7" stroke="{fg}" stroke-width="1.4" fill="none" '
                'stroke-linecap="round" stroke-linejoin="round"/>'),
    "svelte": ("#F03D00", "#FF6A3D",
               '<path d="M10.5 8 C9.8 7.1 8.6 7 7.7 7.4 C6.3 8 6.2 9.4 7.6 10 L8.5 10.4 C9.9 11 9.8 12.4 8.4 12.9 '
               'C7.2 13.3 6 12.8 5.5 12" stroke="{fg}" stroke-width="1.4" fill="none" stroke-linecap="round"/>'),
    "nest": ("#D7244C", "#EA4C6E",
             '<path d="M5.6 12.8 V7.4 L10.4 12.8 V7.4" stroke="{fg}" stroke-width="1.4" fill="none" '
             'stroke-linecap="round" stroke-linejoin="round"/><circle cx="11.6" cy="7.4" r="0.7" fill="{fg}"/>'),
    "python": ("#3572A5", "#4B8BC8",
               '<rect x="4.8" y="7" width="4.2" height="3.6" rx="1.1" fill="{fg}"/>'
               '<rect x="7" y="9.4" width="4.2" height="3.6" rx="1.1" fill="#FFD43B"/>'),
    "rust": ("#B7410E", "#DE6A35",
             '<circle cx="8" cy="10" r="2.85" stroke="{fg}" stroke-width="1.3" stroke-dasharray="1.1 0.9" fill="none"/>'
             '<circle cx="8" cy="10" r="2.1" fill="{fg}"/><circle cx="8" cy="10" r="0.85" fill="{c}"/>'),
    "go": ("#00A3CC", "#2BC0E6",
           '<circle cx="6.2" cy="9.9" r="1.6" fill="{fg}"/><circle cx="9.8" cy="9.9" r="1.6" fill="{fg}"/>'
           '<circle cx="6.7" cy="9.7" r="0.65" fill="#1B1B1B"/><circle cx="10.3" cy="9.7" r="0.65" fill="#1B1B1B"/>'),
    "dotnet": ("#512BD4", "#7B5CE6",
               '<path d="M6.7 7.2 L6.1 12.8 M9.7 7.2 L9.1 12.8 M4.9 9 H11.3 M4.7 11 H11.1" stroke="{fg}" '
               'stroke-width="1.1" fill="none" stroke-linecap="round"/>'),
    "swift": ("#E8492F", "#F4704F",
              '<path d="M4.8 8.2 Q7.6 11.6 10.9 10.6 M9.1 7.1 Q11.7 8.9 10.9 10.6 Q10.6 12.4 8.2 12.8 Q6.6 13 5.3 12.2" '
              'stroke="{fg}" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>'),
    "dart": ("#0175C2", "#3BA0E6",
             '<path d="M10.8 7 L5.6 12.2 M10.8 10.4 L8.4 12.8" stroke="{fg}" stroke-width="1.5" fill="none" '
             'stroke-linecap="round"/>'),
    "ruby": ("#C2302A", "#E0524B",
             '<path d="M6 7.6 H10 L11.4 9.4 L8 13 L4.6 9.4 Z M4.6 9.4 H11.4" stroke="{fg}" stroke-width="1.1" '
             'fill="none" stroke-linejoin="round"/>'),
    "php": ("#6F74B0", "#8F93D0",
            '<ellipse cx="8" cy="10" rx="3.5" ry="2.4" stroke="{fg}" stroke-width="1.1" fill="none"/>'
            '<path d="M7.1 11.4 V8.7 H8.3 A0.8 0.8 0 0 1 8.3 10.3 H7.1" stroke="{fg}" stroke-width="0.9" fill="none" '
            'stroke-linejoin="round"/>'),
    "cmake": ("#1D5E9B", "#4A8ED0",
              '<path d="M8 7.1 L11.3 12.8 H4.7 Z M8 7.1 V12.8" stroke="{fg}" stroke-width="1.1" fill="none" '
              'stroke-linejoin="round"/>'),
    "scala": ("#D3302D", "#E8554F",
              '<path d="M5.4 8.1 L10.6 7.3 M5.4 10.4 L10.6 9.6 M5.4 12.7 L10.6 11.9" stroke="{fg}" '
              'stroke-width="1.4" fill="none" stroke-linecap="round"/>'),
    # phone outline with a home indicator
    "ios": ("#2F6FE4", "#4A8CF7",
            '<rect x="5.7" y="6.8" width="4.6" height="6.4" rx="1.1" stroke="{fg}" stroke-width="1.1" fill="none"/>'
            '<path d="M7.3 12 H8.7" stroke="{fg}" stroke-width="0.8" stroke-linecap="round"/>'),
    # robot head: dome, antennae, eyes
    "android": ("#2E9E5E", "#3DBE76",
                '<path d="M4.9 12.6 A3.1 3.1 0 0 1 11.1 12.6 Z" fill="{fg}"/>'
                '<path d="M6.1 9.9 L5.3 8.5 M9.9 9.9 L10.7 8.5" stroke="{fg}" stroke-width="0.8" stroke-linecap="round"/>'
                '<circle cx="6.9" cy="11.3" r="0.45" fill="{c}"/><circle cx="9.1" cy="11.3" r="0.45" fill="{c}"/>'),
    # React's atom in React's cyan on a dark folder, to tell it apart from a web React module
    "react-native": ("#23272F", "#566074",
                     '<g stroke="#61DAFB" stroke-width="0.8" fill="none">'
                     '<ellipse cx="8" cy="10" rx="3.5" ry="1.35"/>'
                     '<ellipse cx="8" cy="10" rx="3.5" ry="1.35" transform="rotate(60 8 10)"/>'
                     '<ellipse cx="8" cy="10" rx="3.5" ry="1.35" transform="rotate(120 8 10)"/></g>'
                     '<circle cx="8" cy="10" r="0.8" fill="#61DAFB"/>'),
    # chevron with an inner arch
    "expo": ("#1B1B2F", "#DEDEE6",
             '<path d="M4.8 12.6 L8 7.3 L11.2 12.6" stroke="{fg}" stroke-width="1.4" fill="none" '
             'stroke-linecap="round" stroke-linejoin="round"/>'
             '<path d="M6.6 12.6 Q8 10.2 9.4 12.6" stroke="{fg}" stroke-width="1" fill="none" stroke-linecap="round"/>',
             (W, "#1B1B2F")),
}


def mix(hex_colour: str, other: str, t: float) -> str:
    """Linear blend of two #RRGGBB colours, t=0 -> hex_colour, t=1 -> other."""
    a = [int(hex_colour[i:i + 2], 16) for i in (1, 3, 5)]
    b = [int(other[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(a, b))


def icon(name: str, colour: str, glyph: str, fg: str) -> str:
    """Builds one folder icon: darker back tab, gradient front flap, glyph on top."""
    back = mix(colour, "#000000", 0.28)
    top = mix(colour, "#FFFFFF", 0.22)
    gid = f"g_{name}"
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">'
        f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{colour}"/></linearGradient></defs>'
        f'<path d="M1.5 4 Q1.5 2.5 3 2.5 H6 L7.5 4 H13 Q14.5 4 14.5 5.5 V12.5 Q14.5 14 13 14 H3 Q1.5 14 1.5 12.5 Z" fill="{back}"/>'
        f'<path d="M1.5 6.6 Q1.5 5.5 2.6 5.5 H13.4 Q14.5 5.5 14.5 6.6 V12.6 Q14.5 14 13.1 14 H2.9 Q1.5 14 1.5 12.6 Z" fill="url(#{gid})"/>'
        + glyph.format(fg=fg, c=colour)
        + "</svg>\n"
    )


def main() -> None:
    """Writes <name>.svg and <name>_dark.svg for every glyph."""
    os.makedirs(OUT, exist_ok=True)
    for name, spec in GLYPHS.items():
        light, dark, glyph = spec[:3]
        fg_light, fg_dark = spec[3] if len(spec) > 3 else (W, W)
        with open(os.path.join(OUT, f"{name}.svg"), "w") as f:
            f.write(icon(name, light, glyph, fg_light))
        with open(os.path.join(OUT, f"{name}_dark.svg"), "w") as f:
            f.write(icon(name + "_dark", dark, glyph, fg_dark))
    print(f"Wrote {len(GLYPHS) * 2} icons to {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
