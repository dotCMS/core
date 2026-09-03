#!/usr/bin/env bash
# Audit a Language.properties key before removing it.
#
# FR-031 permits removing a definition only where it is unreferenced. "Unreferenced" must mean
# across the WHOLE repo, not merely within Content Drive: Language.properties is shared with legacy
# JSP/VTL surfaces and with Java, so a key that no Angular file mentions may still be in use.
#
# Usage:  ./audit-message-key.sh content-drive.toast.workflow-in-progress [...more keys]
# Exit:   0 if every key given is safe to remove, 1 if any is still referenced.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROPS="$ROOT/dotCMS/src/main/webapp/WEB-INF/messages/Language.properties"
status=0

for key in "$@"; do
  echo "=== $key"

  if grep -qE "^${key}=" "$PROPS"; then echo "  defined:      yes"; else echo "  defined:      NO (already absent)"; fi

  fe=$(grep -rl --include='*.ts' --include='*.html' -F "$key" "$ROOT/core-web/libs" "$ROOT/core-web/apps" 2>/dev/null | wc -l | tr -d ' ')
  legacy=$(grep -rl --include='*.jsp' --include='*.jspf' --include='*.vtl' -F "$key" "$ROOT/dotCMS/src/main/webapp" 2>/dev/null | wc -l | tr -d ' ')
  java=$(grep -rl --include='*.java' -F "$key" "$ROOT/dotCMS/src/main/java" 2>/dev/null | wc -l | tr -d ' ')

  echo "  core-web:     $fe file(s)"
  echo "  legacy views: $legacy file(s)"
  echo "  java:         $java file(s)"

  if [ "$fe" -eq 0 ] && [ "$legacy" -eq 0 ] && [ "$java" -eq 0 ]; then
    echo "  VERDICT:      SAFE TO REMOVE"
  else
    echo "  VERDICT:      STILL REFERENCED - do not remove"
    grep -rn --include='*.ts' --include='*.html' --include='*.jsp' --include='*.jspf' --include='*.vtl' --include='*.java' \
      -F "$key" "$ROOT/core-web/libs" "$ROOT/core-web/apps" "$ROOT/dotCMS/src/main/webapp" "$ROOT/dotCMS/src/main/java" 2>/dev/null \
      | sed "s|$ROOT/||" | head -8 | sed 's/^/      /'
    status=1
  fi
done

echo
echo "LIMIT: this is a literal-string search. A key assembled at runtime from a prefix plus a"
echo "suffix will not be found. Check by eye for any key whose stem is used as a prefix elsewhere."
exit $status
