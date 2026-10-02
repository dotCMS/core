Run the /security-review skill against the diff between
base $BASE_SHA and head $HEAD_SHA (PR #$PR_NUMBER).

Follow the three-phase methodology (identify → parallel
false-positive filter → keep only confidence >= 8).

After producing the markdown report, ALSO write a
machine-readable summary to $GITHUB_WORKSPACE/security-findings.json
with this schema:

{
  "findings_count": <int, total kept findings>,
  "high_count": <int>,
  "medium_count": <int>,
  "report_markdown": "<full markdown report>"
}

If no findings clear the bar, write:
{"findings_count": 0, "high_count": 0, "medium_count": 0, "report_markdown": ""}

DO NOT post any PR comment yourself. Downstream steps handle
disclosure routing.
