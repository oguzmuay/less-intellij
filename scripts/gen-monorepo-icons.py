#!/usr/bin/env python3
"""Generates the 16x16 folder icons of less-monorepo-plugin (light + _dark variants).

Each icon is a folder tinted with the ecosystem's colour plus a small glyph on the front flap.
Usage: python3 scripts/gen-monorepo-icons.py   (rewrites less-monorepo-plugin/src/main/resources/icons/monorepo/)
"""
import math
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "less-monorepo-plugin", "src", "main", "resources", "icons", "monorepo")

W = "#FFFFFF"


def rays(cx: float, cy: float, inner: float, outer: float, n: int, start: float = 0) -> str:
    """Path data for n straight rays around (cx, cy), e.g. a starburst or wheel spokes."""
    d = []
    for i in range(n):
        a = math.radians(start + 360 * i / n)
        d.append(f"M{cx + inner * math.cos(a):.2f} {cy + inner * math.sin(a):.2f} "
                 f"L{cx + outer * math.cos(a):.2f} {cy + outer * math.sin(a):.2f}")
    return " ".join(d)

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
    # page with text lines
    "docs": ("#D9A21B", "#F0B93A",
             '<rect x="5.3" y="6.9" width="5.4" height="6.1" rx="0.8" fill="{fg}"/>'
             '<path d="M6.5 8.7 H9.5 M6.5 10.2 H9.5 M6.5 11.7 H8.4" stroke="{c}" stroke-width="0.7" stroke-linecap="round"/>',
             ),
    # two stacked server units with status LEDs
    "infra": ("#5E6E82", "#8597AD",
              '<rect x="4.6" y="6.9" width="6.8" height="2.5" rx="0.7" fill="{fg}"/>'
              '<rect x="4.6" y="10.1" width="6.8" height="2.5" rx="0.7" fill="{fg}"/>'
              '<circle cx="6" cy="8.15" r="0.5" fill="{c}"/><circle cx="6" cy="11.35" r="0.5" fill="{c}"/>'),
    # docker: stacked containers riding a wave
    "docker": ("#2496ED", "#4DB0F7",
               '<rect x="4.2" y="8.6" width="2.4" height="2" rx="0.3" fill="{fg}"/>'
               '<rect x="6.8" y="8.6" width="2.4" height="2" rx="0.3" fill="{fg}"/>'
               '<rect x="9.4" y="8.6" width="2.4" height="2" rx="0.3" fill="{fg}"/>'
               '<rect x="6.8" y="6.3" width="2.4" height="2" rx="0.3" fill="{fg}"/>'
               '<path d="M3.8 11.7 Q5.9 13.2 8 11.9 Q10.1 13.2 12.2 11.7" stroke="{fg}" stroke-width="0.9" '
               'fill="none" stroke-linecap="round"/>'),
    # kubernetes: ship's wheel (heptagon, hub, seven spokes)
    "kubernetes": ("#326CE5", "#5C8CF0",
                   '<path d="M8 6.6 L10.7 7.9 L11.4 10.8 L9.5 13.1 H6.5 L4.6 10.8 L5.3 7.9 Z" stroke="{fg}" '
                   'stroke-width="0.9" fill="none" stroke-linejoin="round"/>'
                   '<path d="M8 10 V6.6 M8 10 L10.7 7.9 M8 10 L11.4 10.8 M8 10 L9.5 13.1 M8 10 L6.5 13.1 '
                   'M8 10 L4.6 10.8 M8 10 L5.3 7.9" stroke="{fg}" stroke-width="0.7"/>'
                   '<circle cx="8" cy="10" r="1" fill="{fg}"/>'),
    # .github: a branch that forks off and merges back
    "github": ("#24292F", "#C9D1D9",
               '<path d="M6 7.4 V12.4 M10.2 8.6 Q10.2 11 6 11.4" stroke="{fg}" stroke-width="1" fill="none" '
               'stroke-linecap="round"/><circle cx="6" cy="7.4" r="1.15" fill="{fg}"/>'
               '<circle cx="6" cy="12.4" r="1.15" fill="{fg}"/><circle cx="10.2" cy="8.4" r="1.15" fill="{fg}"/>',
               (W, "#24292F")),
    # .idea: a light bulb (idea)
    "idea": ("#D63B6F", "#EE5C8C",
             '<path d="M8 6.6 A2.5 2.5 0 0 1 9.5 11.1 V11.6 H6.5 V11.1 A2.5 2.5 0 0 1 8 6.6 Z" fill="{fg}"/>'
             '<path d="M6.9 12.7 H9.1" stroke="{fg}" stroke-width="0.9" stroke-linecap="round"/>'),
    # scripts: a shell prompt
    "scripts": ("#3B4252", "#6B7686",
                '<path d="M5 7.8 L7.2 9.9 L5 12" stroke="#7EE787" stroke-width="1.3" fill="none" '
                'stroke-linecap="round" stroke-linejoin="round"/>'
                '<path d="M8.4 12.1 H11.2" stroke="{fg}" stroke-width="1.3" stroke-linecap="round"/>'),
    # config: two sliders
    "config": ("#8A63D2", "#A585E6",
               '<path d="M4.8 8 H11.2 M4.8 11.3 H11.2" stroke="{fg}" stroke-width="0.9" stroke-linecap="round"/>'
               '<circle cx="9.4" cy="8" r="1.25" fill="{fg}"/><circle cx="6.6" cy="11.3" r="1.25" fill="{fg}"/>'
               '<circle cx="9.4" cy="8" r="0.5" fill="{c}"/><circle cx="6.6" cy="11.3" r="0.5" fill="{c}"/>'),
    # livekit: a broadcasting dot (live)
    "livekit": ("#E5484D", "#F2666A",
                '<circle cx="8" cy="10" r="1.1" fill="{fg}"/>'
                '<path d="M6.4 8.4 A2.3 2.3 0 0 0 6.4 11.6 M9.6 8.4 A2.3 2.3 0 0 1 9.6 11.6 '
                'M5 7.1 A4 4 0 0 0 5 12.9 M11 7.1 A4 4 0 0 1 11 12.9" stroke="{fg}" stroke-width="0.9" '
                'fill="none" stroke-linecap="round"/>'),
    # whisper: a sound wave
    "whisper": ("#10A37F", "#2FC79C",
                '<path d="M4.8 9.3 V10.7 M6.4 8.2 V11.8 M8 7 V13 M9.6 8.4 V11.6 M11.2 9.1 V10.9" stroke="{fg}" '
                'stroke-width="1" stroke-linecap="round"/>'),
    # keycloak: a key
    "keycloak": ("#0089B8", "#33B5E0",
                 '<circle cx="6.3" cy="10" r="1.55" stroke="{fg}" stroke-width="1.1" fill="none"/>'
                 '<path d="M7.85 10 H11.5 M10.4 10 V11.5 M11.5 10 V11.2" stroke="{fg}" stroke-width="1.1" '
                 'stroke-linecap="round"/>'),
    # seaweedfs: three seaweed fronds
    "seaweedfs": ("#0E7C86", "#22A9B5",
                  '<path d="M5.6 13 Q4.8 11.6 5.6 10.2 Q6.4 8.8 5.6 7.6 M8.2 13 Q7.4 11.2 8.2 9.4 Q9 7.8 8.2 6.6 '
                  'M10.8 13 Q10 11.8 10.8 10.4 Q11.4 9.4 10.9 8.6" stroke="#9BE7A8" stroke-width="1.05" fill="none" '
                  'stroke-linecap="round"/>'),
    # .claude / .codex / .gemini: the same marks as their instruction-file badges
    "claude-dir": ("#D97757", "#E58B6D",
                   f'<path d="{rays(8, 10, 0.9, 3.3, 8, -90)}" stroke="{{fg}}" stroke-width="1.2" stroke-linecap="round"/>'),
    "codex-dir": ("#202123", "#ECECF1",
                  '<path d="M5 7.9 L7.2 10 L5 12.1 M8.4 12.2 H11.2" stroke="{fg}" stroke-width="1.3" fill="none" '
                  'stroke-linecap="round" stroke-linejoin="round"/>', (W, "#202123")),
    "gemini-dir": ("#5B6CF0", "#7C8BF7",
                   '<path d="M8 6.4 C8.3 8.8 9.1 9.6 11.6 10 C9.1 10.3 8.3 11.1 8 13.6 C7.7 11.1 6.9 10.3 4.4 10 '
                   'C6.9 9.6 7.7 8.8 8 6.4 Z" fill="{fg}"/>'),
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


S = 'stroke="{fg}" fill="none" stroke-linecap="round" stroke-linejoin="round"'

# File badges (rounded square): name: (light colour, dark colour, glyph, optional (light fg, dark fg))
BADGES = {
    # M + down arrow
    "markdown": ("#2F6FD0", "#4E8BEA",
                 f'<path d="M3.9 10.6 V5.4 L6 7.9 L8.1 5.4 V10.6 M11.2 5.4 V10.2 M9.5 8.5 L11.2 10.4 L12.9 8.5" '
                 f'stroke-width="1.35" {S}/>'),
    # CLAUDE.md: starburst
    "claude": ("#D97757", "#E58B6D", f'<path d="{rays(8, 8, 1.3, 4.7, 8, -90)}" stroke-width="1.5" {S}/>'),
    # AGENTS.md (Codex and the agents.md standard): a prompt
    "agents": ("#202123", "#ECECF1",
               f'<path d="M4.9 5.6 L7.5 8 L4.9 10.4" stroke-width="1.45" {S}/>'
               f'<path d="M8.6 10.6 H11.4" stroke-width="1.45" {S}/>', (W, "#202123")),
    # GEMINI.md: four-point sparkle
    "gemini": ("#5B6CF0", "#7C8BF7",
               '<path d="M8 3.4 C8.4 6.6 9.4 7.6 12.6 8 C9.4 8.4 8.4 9.4 8 12.6 C7.6 9.4 6.6 8.4 3.4 8 '
               'C6.6 7.6 7.6 6.6 8 3.4 Z" fill="{fg}"/>'),
}

HELM = ("#0F1689", "#5468FF")
# Kubernetes manifests (heptagon): name: glyph (white, {c} = background), optional colours (default k8s blue)
K8S = {
    "k8s-deployment": f'<path d="M10.9 7.3 A3 3 0 1 0 11 10.3" stroke-width="1.2" {S}/>'
                      f'<path d="M11.3 5.6 L11 7.6 L9 7.4" stroke-width="1.2" {S}/>',
    "k8s-pod": f'<path d="M8 5.2 L11 6.8 V10.4 L8 12 L5 10.4 V6.8 Z M5 6.8 L8 8.4 L11 6.8 M8 8.4 V12" stroke-width="1" {S}/>',
    "k8s-service": '<rect x="6.7" y="5.2" width="2.6" height="2" rx="0.4" fill="{fg}"/>'
                   f'<path d="M8 7.2 V8.8 M5.6 10.2 V8.8 H10.4 V10.2 M8 8.8 V10.2" stroke-width="0.9" {S}/>'
                   '<rect x="4.5" y="10.2" width="2.2" height="1.8" rx="0.4" fill="{fg}"/>'
                   '<rect x="6.9" y="10.2" width="2.2" height="1.8" rx="0.4" fill="{fg}"/>'
                   '<rect x="9.3" y="10.2" width="2.2" height="1.8" rx="0.4" fill="{fg}"/>',
    "k8s-ingress": f'<path d="M4.5 8.6 H9 M7.3 6.9 L9 8.6 L7.3 10.3" stroke-width="1.2" {S}/>'
                   f'<path d="M10 5.8 H11.4 V11.4 H10" stroke-width="1.2" {S}/>',
    "k8s-configmap": f'<path d="M4.8 7.2 H11.2 M4.8 10.2 H11.2" stroke-width="0.9" {S}/>'
                     '<circle cx="9.4" cy="7.2" r="1.2" fill="{fg}"/><circle cx="6.6" cy="10.2" r="1.2" fill="{fg}"/>',
    "k8s-secret": '<rect x="5.4" y="8.2" width="5.2" height="3.9" rx="0.7" fill="{fg}"/>'
                  f'<path d="M6.6 8.2 V7.1 A1.4 1.4 0 0 1 9.4 7.1 V8.2" stroke-width="1.1" {S}/>'
                  '<circle cx="8" cy="10.1" r="0.6" fill="{c}"/>',
    "k8s-statefulset": '<path d="M5.2 6.6 V10.8 A2.8 1.1 0 0 0 10.8 10.8 V6.6 Z" fill="{fg}"/>'
                       '<ellipse cx="8" cy="6.6" rx="2.8" ry="1.1" fill="{fg}"/>'
                       '<path d="M5.2 6.6 A2.8 1.1 0 0 0 10.8 6.6 M5.2 8.7 A2.8 1.1 0 0 0 10.8 8.7" stroke="{c}" '
                       'stroke-width="0.6" fill="none"/>',
    "k8s-daemonset": ''.join(f'<rect x="{x}" y="{y}" width="2.5" height="2.5" rx="0.5" fill="{{fg}}"/>'
                             for x in (5.3, 8.2) for y in (5.9, 8.8)),
    "k8s-job": '<path d="M6.3 5.9 L11 8.6 L6.3 11.3 Z" fill="{fg}" stroke="{fg}" stroke-width="0.8" stroke-linejoin="round"/>',
    "k8s-cronjob": f'<circle cx="8" cy="8.6" r="3.1" stroke-width="1.1" {S}/><path d="M8 6.8 V8.6 L9.4 9.5" stroke-width="1" {S}/>',
    "k8s-namespace": f'<rect x="4.8" y="5.9" width="6.4" height="5.6" rx="1" stroke-width="1" stroke-dasharray="1.4 0.9" {S}/>',
    "k8s-volume": '<rect x="4.6" y="6.5" width="6.8" height="4.4" rx="0.9" fill="{fg}"/>'
                  '<path d="M5.8 9.4 H8.2" stroke="{c}" stroke-width="0.8" stroke-linecap="round"/>'
                  '<circle cx="9.8" cy="9.4" r="0.55" fill="{c}"/>',
    "k8s-hpa": f'<path d="M4.6 8.6 H11.4 M6.2 7 L4.6 8.6 L6.2 10.2 M9.8 7 L11.4 8.6 L9.8 10.2" stroke-width="1.1" {S}/>',
    "k8s-rbac": '<circle cx="8" cy="7" r="1.6" fill="{fg}"/><path d="M4.9 12 A3.1 3.1 0 0 1 11.1 12 Z" fill="{fg}"/>',
    "k8s-networkpolicy": '<path d="M8 5.2 L11 6.3 V8.7 Q11 11.2 8 12.2 Q5 11.2 5 8.7 V6.3 Z" fill="{fg}"/>',
    "k8s-kustomization": '<path d="M8 5.4 L11.4 7.2 L8 9 L4.6 7.2 Z" fill="{fg}"/>'
                         f'<path d="M4.6 9 L8 10.8 L11.4 9 M4.6 10.8 L8 12.6 L11.4 10.8" stroke-width="0.9" {S}/>',
    "helm-chart": (f'<circle cx="8" cy="8.6" r="2.3" stroke-width="1" {S}/>'
                   f'<path d="{rays(8, 8.6, 0, 3.7, 8, 22.5)}" stroke-width="0.8" {S}/>'
                   '<circle cx="8" cy="8.6" r="0.9" fill="{fg}"/>', HELM),
    "helm-values": (f'<path d="M4.8 7.2 H11.2 M4.8 10.2 H11.2" stroke-width="0.9" {S}/>'
                    '<circle cx="9.4" cy="7.2" r="1.2" fill="{fg}"/><circle cx="6.6" cy="10.2" r="1.2" fill="{fg}"/>',
                    HELM),
}
K8S_BLUE = ("#326CE5", "#5C8CF0")


def file_icon(name: str, colour: str, shape: str, glyph: str, fg: str) -> str:
    """Builds one file badge: a rounded square or heptagon with a vertical gradient and a glyph."""
    top = mix(colour, "#FFFFFF", 0.2)
    gid = f"g_{name}"
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">'
        f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{colour}"/></linearGradient></defs>'
        + shape.format(fill=f"url(#{gid})") + glyph.format(fg=fg, c=colour) + "</svg>\n"
    )


SQUARE = '<rect x="1.5" y="1.5" width="13" height="13" rx="3" fill="{fill}"/>'
HEPTAGON = ('<path d="M8 1 L13.6 3.7 L15 9.8 L11.1 14.6 H4.9 L1 9.8 L2.4 3.7 Z" fill="{fill}" '
            'stroke="{fill}" stroke-width="1" stroke-linejoin="round"/>')


def write_files() -> int:
    """Writes the file badges (Markdown, AI instruction files, Kubernetes/Helm manifests); returns the count."""
    count = 0
    jobs = [(n, spec[0], spec[1], SQUARE, spec[2], spec[3] if len(spec) > 3 else (W, W)) for n, spec in BADGES.items()]
    for n, spec in K8S.items():
        glyph, (light, dark) = (spec, K8S_BLUE) if isinstance(spec, str) else spec
        jobs.append((n, light, dark, HEPTAGON, glyph, (W, W)))
    for n, light, dark, shape, glyph, (fg_l, fg_d) in jobs:
        for suffix, col, fg in (("", light, fg_l), ("_dark", dark, fg_d)):
            with open(os.path.join(OUT, f"{n}{suffix}.svg"), "w") as f:
                f.write(file_icon(n + suffix, col, shape, glyph, fg))
            count += 1
    return count


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
    # VERSION file: a price-tag shape (no folder), light + dark
    for suffix, col in (("", "#6B7280"), ("_dark", "#A3ABB8")):
        with open(os.path.join(OUT, f"version{suffix}.svg"), "w") as f:
            f.write('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">'
                    f'<path fill-rule="evenodd" d="M2 3.2 Q2 2 3.2 2 H8 L14 8 L8 14 L2 8 Z M5 4.2 A1.1 1.1 0 1 0 5 6.4 A1.1 1.1 0 1 0 5 4.2 Z" fill="{col}"/></svg>\n')
    files = write_files()
    print(f"Wrote {len(GLYPHS) * 2 + 2 + files} icons to {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
