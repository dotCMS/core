// Resolves every dotCMS/core issue related to a merged pull request, for the
// post-merge test-plan automation.
//
// The issue set is the UNION of three sources with no precedence between them
// (rock spec §5). Every candidate is then validated against the API and dropped
// if it does not resolve to a real issue.
//
//   1. Branch name  — consecutive leading numeric tokens, with or without an
//                     `issue-` prefix:  ^(issue-)?\d+(-\d+)*-
//                     Tokens below MIN_ISSUE_NUMBER are discarded, so date-like
//                     prefixes (`2024-01-15-...`) yield nothing.
//                     `issue-37085-bouncycastle-185` -> [37085]   (185 is a
//                     library version, not an issue). `36937-36938-roles-api`
//                     -> [36937, 36938].
//                     NOTE: an author-prefixed branch (`nicobytes/36950-...`)
//                     yields nothing here, matching both spec §5.1 and the
//                     existing issue_comp_link-pr-to-issue.yml behaviour.
//   2. PR body      — ONLY references inside a `This PR fixes` statement.
//                     Accepts `#123` and `[#123](.../issues/123)`, multiple per
//                     statement. Bare `#123` elsewhere, `Related:`, and
//                     Fixes/Closes/Resolves are deliberately NOT interpreted.
//   3. Development  — GitHub's Development relationship: issues set to close on
//                     merge PLUS issues merely connected (spec §5.3 wants every
//                     linked issue, not only the closers).
//
// Invoked from actions/github-script; gets { github, core }.
// Inputs (env):
//   PR_NUMBER   required
//   PR_BRANCH   required
//   PR_BODY     optional — pass the freshly re-fetched body, not the event
//               payload: issue_comp_link-pr-to-issue.yml PATCHes the body on
//               the same pull_request.closed event and races this workflow.
// Outputs (core.setOutput):
//   issues_json  JSON array of validated issue numbers, ascending
//   issues_csv   same, comma-separated (marker format)
//   has_issues   'true' | 'false'
//   summary      markdown summary for the job log

/**
 * Consecutive leading numeric tokens of a branch name.
 *
 * Verified against real dotCMS/core branches — each of these is a trap worth keeping in mind
 * before "simplifying" this regex:
 *   issue-31904-Key-Value-Field-Preserve-order        -> [31904]
 *   issue-12345-43565-testing                         -> [12345, 43565]  (agreed multi-issue form)
 *   36937-36938-roles-api-role-membership             -> [36937, 36938]  (bare numeric prefix)
 *   37109-users-empty-roles-remove-all                -> [37109]
 *   issue-37085-bouncycastle-185                      -> [37085]  185 is a LIBRARY VERSION
 *   issue-37085-samlbundle-26.08.21                   -> [37085]  dotted version, not issues
 *   nicobytes/36950-remove-dead-core-web-libs-impl    -> []       author prefix: by design, the
 *                                                                 Development source recovers it
 *   issue-xmllint-apt-hang                            -> []       no number at all
 *   2024-01-15-hotfix-something                       -> []       date prefix: components are all
 *                                                                 below MIN_ISSUE_NUMBER, and a
 *                                                                 bare year/month/day must never be
 *                                                                 mistaken for an issue reference
 * Only the run of numbers at the very start counts; anything after a non-numeric token is data,
 * not an issue reference. Every survivor is still validated against the API before it is used.
 */

// dotCMS/core passed issue #10000 in October 2016, while every component of a date prefix
// (`2024-01-15-...`) is ≤ 9999 by construction. Without this floor the regex above turns a
// date-prefixed branch into candidates [2024, 1, 15] — and validate() cannot save us, because
// old low-numbered issues like core#15 really do exist, so a plan would land on an unrelated
// decade-old ticket. The floor applies to the `issue-`-prefixed form too on purpose: explicit
// or not, `issue-671-...` is exactly how PR #37167 pointed at private-issues#671, and no real
// dotCMS/core work references anything below the floor anymore.
const MIN_ISSUE_NUMBER = 10000;

function issuesFromBranch(branch) {
  if (!branch) return [];
  const m = /^(?:issue-)?(\d+(?:-\d+)*)-/.exec(branch.trim());
  if (!m) return [];
  return m[1].split('-').map(Number).filter((n) => n >= MIN_ISSUE_NUMBER);
}

/**
 * Issue references inside `This PR fixes` statements only, scoped to one repository.
 *
 * Scoped to that one statement on purpose (rock spec §5.2): `Fixes #N`, `Closes #N`, `Resolves #N`,
 * a bare `#N`, and `Related: #N` are all ignored here.
 *
 * Worth knowing: the `This PR fixes` line is often written by issue_comp_link-pr-to-issue.yml, not
 * by a human, and that workflow only ever appends ONE issue derived from the branch. Real example —
 * PR #37077's body reads `Closes #36937, closes #36938. #36939` but its bot line names only #36937.
 * This source therefore under-reports multi-issue PRs by design; the union with Development is what
 * makes the set complete.
 *
 * A reference can name another repository, and a bare number lifted out of `dotCMS/private-issues#671`
 * would be looked up here as core#671 — the same collision the branch source is guarded against.
 * So qualified `owner/repo#N` references and issue URLs are honoured only when they point at the
 * target repo, and are stripped before bare `#N` scanning so their numbers cannot leak through.
 *
 * Matching is line-scoped so a statement cannot swallow issue numbers from the following line.
 */
function issuesFromBody(body, target) {
  if (!body) return { numbers: [], foreign: [] };
  const numbers = [];
  const foreign = [];
  const stmt = /this\s+pr\s+fixes\b[^\n]*/gi;
  let s;
  while ((s = stmt.exec(body)) !== null) {
    let text = s[0];

    // Issue URLs — keep only this repo's.
    for (const m of text.matchAll(/github\.com\/([\w.-]+\/[\w.-]+)\/issues\/(\d+)/gi)) {
      if (m[1].toLowerCase() === target.toLowerCase()) numbers.push(Number(m[2]));
      else foreign.push({ number: Number(m[2]), repo: m[1] });
    }
    text = text.replace(/github\.com\/[\w.-]+\/[\w.-]+\/issues\/\d+/gi, ' ');

    // Qualified owner/repo#N — keep only this repo's, then remove so the bare scan cannot see them.
    for (const m of text.matchAll(/([\w.-]+\/[\w.-]+)#(\d+)/g)) {
      if (m[1].toLowerCase() === target.toLowerCase()) numbers.push(Number(m[2]));
      else foreign.push({ number: Number(m[2]), repo: m[1] });
    }
    text = text.replace(/[\w.-]+\/[\w.-]+#\d+/g, ' ');

    // Whatever bare `#N` remains is unqualified, so it means this repo.
    for (const m of text.matchAll(/#(\d+)/g)) numbers.push(Number(m[1]));
  }
  return { numbers, foreign };
}

/**
 * Issues linked through GitHub's Development panel (closing + merely connected).
 *
 * Development links are NOT repo-scoped: dotCMS routinely links a core PR to an issue in another
 * repository — `dotCMS/private-issues` for embargoed security work, for example. The GraphQL nodes
 * therefore carry `repository.nameWithOwner`, and discarding it is dangerous: a bare number gets
 * looked up in dotCMS/core, where an unrelated issue may happen to occupy it. Real case — PR #37167
 * links `dotCMS/private-issues#671`, while `dotCMS/core#671` is a 2012 pull request.
 *
 * Returns same-repo issues in `numbers`, and everything else in `foreign` so the caller can both
 * report it and use it to disqualify identical numbers arriving from the branch or body.
 */
async function issuesFromDevelopment(github, owner, repo, prNumber) {
  const target = `${owner}/${repo}`;
  const query = `
    query($owner:String!, $repo:String!, $pr:Int!) {
      repository(owner:$owner, name:$repo) {
        pullRequest(number:$pr) {
          closingIssuesReferences(first: 50) {
            nodes { number repository { nameWithOwner } }
          }
          timelineItems(first: 100, itemTypes: [CONNECTED_EVENT, DISCONNECTED_EVENT]) {
            nodes {
              __typename
              ... on ConnectedEvent    { subject { ... on Issue { number repository { nameWithOwner } } } }
              ... on DisconnectedEvent { subject { ... on Issue { number repository { nameWithOwner } } } }
            }
          }
        }
      }
    }`;
  let data;
  try {
    data = await github.graphql(query, { owner, repo, pr: prNumber });
  } catch (err) {
    // A Development lookup failure must not sink the whole run; the other two
    // sources still stand.
    return { numbers: [], foreign: [], error: err.message };
  }
  const pr = data?.repository?.pullRequest;
  if (!pr) return { numbers: [], foreign: [], error: 'pull request not found' };

  const linked = [];
  for (const n of pr.closingIssuesReferences?.nodes || []) {
    if (n?.number) linked.push({ number: n.number, repo: n.repository?.nameWithOwner });
  }

  // Connected minus disconnected — a link that was later removed does not count.
  // Keyed by repo#number so an unlink in one repo cannot cancel a link in another.
  const disconnected = new Set();
  const connected = [];
  for (const n of pr.timelineItems?.nodes || []) {
    const num = n?.subject?.number;
    if (!num) continue;
    const nwo = n.subject.repository?.nameWithOwner;
    const key = `${nwo}#${num}`;
    if (n.__typename === 'ConnectedEvent') connected.push({ number: num, repo: nwo, key });
    else if (n.__typename === 'DisconnectedEvent') disconnected.add(key);
  }
  linked.push(...connected.filter((c) => !disconnected.has(c.key)));

  const numbers = linked.filter((l) => l.repo === target).map((l) => l.number);
  const foreign = linked.filter((l) => l.repo && l.repo !== target);
  return { numbers, foreign };
}

/**
 * Keeps only candidates that resolve to a real ISSUE in this repo.
 * Drops 404s and numbers that are actually pull requests.
 */
async function validate(github, owner, repo, candidates) {
  const kept = [];
  const dropped = [];
  for (const number of candidates) {
    try {
      const { data } = await github.rest.issues.get({ owner, repo, issue_number: number });
      if (data.pull_request) dropped.push({ number, reason: 'is a pull request, not an issue' });
      else kept.push(number);
    } catch (err) {
      dropped.push({ number, reason: err.status === 404 ? 'does not exist' : `lookup failed (${err.status || err.message})` });
    }
  }
  return { kept, dropped };
}

module.exports = async ({ github, core }) => {
  const prNumber = parseInt(process.env.PR_NUMBER, 10);
  const branch = process.env.PR_BRANCH || '';
  const body = process.env.PR_BODY || '';
  const [owner, repo] = (process.env.GITHUB_REPOSITORY || 'dotCMS/core').split('/');

  if (!Number.isInteger(prNumber)) throw new Error('PR_NUMBER is required');

  const fromBranch = issuesFromBranch(branch);
  const bodyRes = issuesFromBody(body, `${owner}/${repo}`);
  const fromBody = bodyRes.numbers;
  const dev = await issuesFromDevelopment(github, owner, repo, prNumber);

  // A branch or body reference carries no repository, so `issue-671-...` is indistinguishable from
  // core#671. When Development attributes that exact number to a DIFFERENT repo, believe
  // Development: the reference is to that repo's issue, and a same-numbered item here is a
  // coincidence. Without this, PR #37167 (`issue-671-...` -> dotCMS/private-issues#671) is one
  // unlucky number away from posting a security test plan onto an unrelated 2012 ticket.
  const foreignByNumber = new Map(
    [...dev.foreign, ...bodyRes.foreign].map((f) => [f.number, f.repo]),
  );

  const union = [...new Set([...fromBranch, ...fromBody, ...dev.numbers])]
    .filter((n) => n !== prNumber)
    .sort((a, b) => a - b);

  const candidates = union.filter((n) => !foreignByNumber.has(n));
  const crossRepo = union
    .filter((n) => foreignByNumber.has(n))
    .map((n) => ({ number: n, reason: `belongs to ${foreignByNumber.get(n)}, not ${owner}/${repo}` }));

  const { kept, dropped: invalid } = await validate(github, owner, repo, candidates);
  const dropped = [...crossRepo, ...invalid];

  const provenance = (n) => [
    fromBranch.includes(n) ? 'branch' : null,
    fromBody.includes(n) ? 'body' : null,
    dev.numbers.includes(n) ? 'development' : null,
  ].filter(Boolean).join(', ');

  // A single source can legitimately yield the same number twice — a markdown
  // issue link matches both `#123` and `/issues/123`, and Development can list
  // an issue as both closing and connected. Dedupe for display; the union
  // already dedupes for real.
  const show = (nums) => (nums.length ? [...new Set(nums)].map((n) => `#${n}`).join(', ') : '_none_');

  const lines = [
    `**Related-issue discovery for PR #${prNumber}**`,
    '',
    `- Branch \`${branch}\` → ${show(fromBranch)}`,
    `- \`This PR fixes\` → ${show(fromBody)}`,
    `- Development → ${show(dev.numbers)}${dev.error ? ` _(lookup failed: ${dev.error})_` : ''}`,
    ...(dev.foreign.length || bodyRes.foreign.length
      ? [`- References outside \`${owner}/${repo}\` (ignored) → ${[...dev.foreign, ...bodyRes.foreign].map((f) => `\`${f.repo}#${f.number}\``).join(', ')}`]
      : []),
    '',
  ];
  if (kept.length) {
    lines.push('| Issue | Found via |', '|---|---|');
    for (const n of kept) lines.push(`| #${n} | ${provenance(n)} |`);
  } else {
    lines.push('_No valid related issue found — no test plan will be generated._');
  }
  if (dropped.length) {
    lines.push('', '**Dropped candidates**', '');
    for (const d of dropped) lines.push(`- #${d.number} — ${d.reason}`);
  }
  const summary = lines.join('\n');

  core.setOutput('issues_json', JSON.stringify(kept));
  core.setOutput('issues_csv', kept.join(','));
  core.setOutput('has_issues', kept.length ? 'true' : 'false');
  core.setOutput('summary', summary);
  core.info(summary);

  return kept;
};

