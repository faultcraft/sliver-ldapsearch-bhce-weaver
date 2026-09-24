#!/usr/bin/env bash
#
# Build BHCE Weaver manual PDF from docs/MANUAL.md.
#
# Requires: pandoc, latexmk + texlive-xetex (or bare xelatex).
# Output:   docs/weaver-manual.pdf
#
# Install deps (Ubuntu/Debian):
#   sudo apt install pandoc latexmk texlive-xetex texlive-fonts-extra

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
INPUT="${PROJECT_ROOT}/docs/MANUAL.md"
OUTPUT="${PROJECT_ROOT}/docs/weaver-manual.pdf"
LUA_CODEBLOCK="${SCRIPT_DIR}/lua/codeblock-lang.lua"
LUA_MDSTRIP="${SCRIPT_DIR}/lua/pdf-mdlink-strip.lua"

if [[ ! -f "${INPUT}" ]]; then
  echo "error: ${INPUT} not found" >&2
  exit 1
fi

if ! command -v pandoc >/dev/null 2>&1; then
  echo "error: pandoc not found." >&2
  echo "       Install: sudo apt install pandoc" >&2
  exit 1
fi

PDF_ENGINE=""
PDF_ENGINE_OPTS=()
if command -v latexmk >/dev/null 2>&1 && command -v xelatex >/dev/null 2>&1; then
  PDF_ENGINE="latexmk"
  PDF_ENGINE_OPTS=(--pdf-engine-opt=-pdfxe --pdf-engine-opt=-interaction=nonstopmode)
elif command -v xelatex >/dev/null 2>&1; then
  PDF_ENGINE="xelatex"
else
  echo "error: no PDF engine found." >&2
  echo "       Install: sudo apt install latexmk texlive-xetex" >&2
  exit 1
fi

SOURCE_DATE_EPOCH=$(git -C "${PROJECT_ROOT}" log -1 --format=%ct -- docs/ 2>/dev/null || date +%s)
export SOURCE_DATE_EPOCH
BUILD_DATE="$(date -u -d "@${SOURCE_DATE_EPOCH}" +%Y-%m-%d 2>/dev/null \
            || date -u -r "${SOURCE_DATE_EPOCH}" +%Y-%m-%d 2>/dev/null \
            || echo "unknown")"

LUA_OPTS=()
if [[ -f "${LUA_CODEBLOCK}" ]]; then
  LUA_OPTS+=(--lua-filter="${LUA_CODEBLOCK}")
fi
if [[ -f "${LUA_MDSTRIP}" ]]; then
  LUA_OPTS+=(--lua-filter="${LUA_MDSTRIP}")
fi

FONT_OPTS=()
if fc-list "IBM Plex Mono" 2>/dev/null | grep -q Plex; then
  FONT_OPTS+=(-V monofont="IBM Plex Mono" -V monofontoptions="Scale=0.85")
elif fc-list "DejaVu Sans Mono" 2>/dev/null | grep -q DejaVu; then
  FONT_OPTS+=(-V monofont="DejaVu Sans Mono" -V monofontoptions="Scale=0.85")
fi
if fc-list "Source Serif 4" 2>/dev/null | grep -q "Source Serif"; then
  FONT_OPTS+=(-V mainfont="Source Serif 4")
fi
if fc-list "Inter" 2>/dev/null | grep -q Inter; then
  FONT_OPTS+=(-V sansfont="Inter")
fi

PANDOC_FROM="markdown+smart+pipe_tables+backtick_code_blocks-tex_math_dollars-tex_math_single_backslash-raw_tex"

pandoc "${INPUT}" \
  --from="${PANDOC_FROM}" \
  --standalone \
  --toc \
  --toc-depth=3 \
  --pdf-engine="${PDF_ENGINE}" \
  "${PDF_ENGINE_OPTS[@]}" \
  "${LUA_OPTS[@]}" \
  --highlight-style=tango \
  "${FONT_OPTS[@]}" \
  --resource-path="${PROJECT_ROOT}/docs" \
  --metadata title="BHCE Weaver Manual" \
  --metadata subtitle="v0.1.0 — ${BUILD_DATE}" \
  --metadata author="Sogon Security" \
  --metadata date="${BUILD_DATE}" \
  --metadata colorlinks=true \
  --metadata papersize=letter \
  --metadata lang=en \
  -o "${OUTPUT}" \
  2> >(grep -v 'Input index file.*not found' \
     | grep -v '^Usage: makeindex' \
     | grep -v 'system returned with code 256' \
     | grep -v 'Using bibtex to make bibliography' >&2)

printf 'pdf: %s (via %s)\n' "${OUTPUT}" "${PDF_ENGINE}"
