/**
 * Fallback issue-link detection, mirroring the PR gate.
 *
 * `closingIssuesReferences` only knows about closing links (Closes/Fixes/
 * Resolves keywords and the Development panel). The gate that every PR must
 * pass (.github/workflows/issue_comp_link-issue-to-pr.yml) also accepts forms
 * GitHub does not treat as closing:
 *   - a non-closing body reference: "Refs #N", "Part of #N", "Related to #N"
 *   - a trailing "(#N)" on the PR title
 *   - an issue number in the branch name: "123-foo", "user/123-foo", "issue-123"
 *
 * Without this fallback a PR that the gate accepted shows up as an orphan in
 * the release report. Keep the patterns and their priority in step with the
 * gate's "Check if PR already has linked issues" and "Extract issue number
 * from branch name" steps; like the gate, only the first match is returned.
 *
 * One ordering difference is intentional. The gate checks "Refs #N" and the
 * title suffix before closingIssuesReferences; the report checks
 * closingIssuesReferences first. They only disagree when a PR has both a
 * closing link the gate's regex misses (e.g. "fixes [#A](url)") and a
 * "Refs #B": the gate picks #B, the report picks #A. For QA the issue the PR
 * actually closes is the right one to read labels from.
 */

const CLOSING_BODY_REF = /(close[ds]?|fix(e[ds])?|resolve[ds]?):?\s+#(\d+)/i;
const NON_CLOSING_BODY_REF =
  /(refs?|references?|related to|part of|contributes to):?\s+#(\d+)/i;
const TITLE_SUFFIX_REF = /\(#(\d+)\)\s*$/;
const BRANCH_REFS: RegExp[] = [/(^|\/)(\d+)-/, /^issue-(\d+)-/, /issue-(\d+)/];

/**
 * Finds the same-repo issue a PR declares through the gate's fallback forms,
 * checked in the gate's order: closing body keyword, non-closing body
 * reference, title suffix, then branch name.
 *
 * @param body   the PR description, or null when the PR has none
 * @param title  the PR title
 * @param branch the PR's head branch name
 * @returns the issue number, or undefined when the PR declares none
 */
export function findDeclaredIssue(
  body: string | null,
  title: string,
  branch: string
): number | undefined {
  const text = body ?? '';

  const closing = text.match(CLOSING_BODY_REF);
  if (closing) return Number(closing[3]);

  const nonClosing = text.match(NON_CLOSING_BODY_REF);
  if (nonClosing) return Number(nonClosing[2]);

  const titleRef = title.match(TITLE_SUFFIX_REF);
  if (titleRef) return Number(titleRef[1]);

  for (const pattern of BRANCH_REFS) {
    const m = branch.match(pattern);
    if (m) return Number(m[m.length - 1]);
  }
  return undefined;
}
