#!/usr/bin/env bash
#
# PostToolUse hook: run the offline checks after any source edit and feed findings back.
#
# Why this exists: in this environment **nothing compiles** — the sandbox cannot reach
# `dl.google.com` or `api.foojay.io`, so no Gradle task runs. Without this hook the shortest way to
# learn that an edit broke import order or resource parity is a full CI round trip: five to fifteen
# minutes. `tools/checks.py` answers the same question in seconds.
#
# Exit codes are the contract with Claude Code:
#   0  silent success — nothing to say
#   2  findings — stderr is fed back to the agent as feedback to act on
#
# It never blocks on anything else: a missing python3, a checks.py crash or an edit to a file the
# checks do not cover all exit 0. A hook that fails noisily on unrelated edits gets disabled, and a
# disabled hook checks nothing.

set -uo pipefail

project_dir="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
payload="$(cat)"

# The edited path arrives in the tool payload. `jq` is not guaranteed to exist, so fall back to a
# grep that is good enough for a single well-known key.
if command -v jq >/dev/null 2>&1; then
  edited="$(printf '%s' "$payload" | jq -r '.tool_input.file_path // empty')"
else
  edited="$(printf '%s' "$payload" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | cut -d'"' -f4)"
fi

case "$edited" in
  *.kt | *.kts | *.xml) ;;
  *) exit 0 ;;
esac

command -v python3 >/dev/null 2>&1 || exit 0

output="$(cd "$project_dir" && python3 tools/checks.py 2>&1)"
status=$?

if [ "$status" -eq 0 ]; then
  exit 0
fi

# Any non-zero status that is not "findings" (1) means the checker itself broke. Report it, but do
# not turn a broken tool into a blocked edit.
if [ "$status" -ne 1 ]; then
  printf 'tools/checks.py exited with %s; findings unknown:\n%s\n' "$status" "$output" >&2
  exit 0
fi

printf 'tools/checks.py found problems after editing %s.\n\n%s\n\nFix these before continuing: they are the same checks `Verify` runs first.\n' \
  "$edited" "$output" >&2
exit 2
