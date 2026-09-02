/**
 * QA status computation.
 *
 * Strict aggregation rules across a PR's linked issues:
 *   - Any issue carrying `QA : Failed`  → failed
 *   - Else every issue carries `QA : Passed` or `QA : Not Needed` → passed
 *   - Else → missing
 *
 * Issues that are actually PR references (GitHub returns `pull_request` on
 * `issues.get`) or 404s are ignored when computing the verdict. If filtering
 * leaves zero usable issues, the PR is treated as `missing` rather than
 * `unlinked` — the author *did* declare a link, it just doesn't resolve.
 */

import { LinkedIssueInfo, PRDetails, PRQAResult, QAStatus } from './types';
import { classifyExclusion } from './exclusions';

const LABEL_FAILED = 'qa : failed';
const LABEL_PASSED = 'qa : passed';
const LABEL_NOT_NEEDED = 'qa : not needed';

function issueVerdict(issue: LinkedIssueInfo): LinkedIssueInfo['verdict'] {
  if (issue.isPullRequest || issue.notFound) return 'ignored';
  const labelsLower = issue.labels.map((l) => l.toLowerCase());
  if (labelsLower.includes(LABEL_FAILED)) return 'failed';
  if (labelsLower.includes(LABEL_PASSED)) return 'passed';
  if (labelsLower.includes(LABEL_NOT_NEEDED)) return 'not-needed';
  return 'none';
}

function aggregateStatus(verdicts: LinkedIssueInfo['verdict'][]): QAStatus {
  const usable = verdicts.filter((v) => v !== 'ignored');
  if (usable.length === 0) return 'missing';
  if (usable.some((v) => v === 'failed')) return 'failed';
  if (usable.every((v) => v === 'passed' || v === 'not-needed')) return 'passed';
  return 'missing';
}

export function computePRQA(
  pr: PRDetails,
  issueInfos: Map<number, LinkedIssueInfo>
): PRQAResult {
  const base = {
    pr: pr.number,
    title: pr.title,
    url: pr.url,
    author: pr.author,
    labels: pr.labels,
    externalRefs: pr.externalRefs,
  };

  const exclusion = classifyExclusion(pr);
  if (exclusion.excluded) {
    return {
      ...base,
      status: 'excluded',
      exclusionReason: exclusion.reason,
      linkedIssues: [],
    };
  }

  if (pr.linkedIssues.length === 0) {
    // No same-repo links. If there's an external (cross-repo) closing reference,
    // surface as `external` — we can't read QA labels from another repo.
    if (pr.externalRefs.length > 0) {
      return { ...base, status: 'external', linkedIssues: [] };
    }
    return { ...base, status: 'unlinked', linkedIssues: [] };
  }

  const resolved: LinkedIssueInfo[] = pr.linkedIssues.map((n) => {
    const info = issueInfos.get(n);
    if (!info) {
      return {
        number: n,
        url: '',
        isPullRequest: false,
        notFound: true,
        labels: [],
        verdict: 'ignored',
      };
    }
    return { ...info, verdict: issueVerdict(info) };
  });

  const status = aggregateStatus(resolved.map((r) => r.verdict));
  return { ...base, status, linkedIssues: resolved };
}
