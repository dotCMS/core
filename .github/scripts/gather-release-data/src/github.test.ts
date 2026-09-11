import { BODY_CHAR_LIMIT, fetchPRDetails } from './github';
import type { Octokit } from '@octokit/rest';

describe('fetchPRDetails', () => {
  function mockPR(over: Record<string, unknown> = {}) {
    return {
      number: 37196,
      title: 'fix(edit-content): pick the asset picker per host',
      body: 'Closes: #37132',
      labels: { nodes: [{ name: 'Type : Defect' }] },
      closingIssuesReferences: {
        nodes: [{ number: 37132, repository: { nameWithOwner: 'dotCMS/core' } }],
      },
      ...over,
    };
  }

  it('reads linked issues from closingIssuesReferences, not the body (#35763)', async () => {
    // "Closes: #N" — the form CLAUDE.md mandates — never matched the old regex.
    const graphql = jest.fn(async () => ({ repository: { pr37196: mockPR() } }));
    const octokit = { graphql } as unknown as Octokit;

    const details = await fetchPRDetails(octokit, 'dotCMS', 'core', [37196]);
    expect(details.get(37196)?.linkedIssues).toEqual([37132]);
    expect(details.get(37196)?.labels).toEqual(['Type : Defect']);
  });

  it('drops cross-repo refs — the changelog links this repo only', async () => {
    const graphql = jest.fn(async () => ({
      repository: {
        pr37196: mockPR({
          closingIssuesReferences: {
            nodes: [
              { number: 37132, repository: { nameWithOwner: 'dotCMS/core' } },
              { number: 673, repository: { nameWithOwner: 'dotCMS/private-issues' } },
            ],
          },
        }),
      },
    }));
    const octokit = { graphql } as unknown as Octokit;

    const details = await fetchPRDetails(octokit, 'dotCMS', 'core', [37196]);
    expect(details.get(37196)?.linkedIssues).toEqual([37132]);
  });

  it('truncates bodies to keep the assembled prompt under MAX_ARG_STRLEN', async () => {
    const graphql = jest.fn(async () => ({
      repository: { pr37196: mockPR({ body: 'x'.repeat(50_000) }) },
    }));
    const octokit = { graphql } as unknown as Octokit;

    const details = await fetchPRDetails(octokit, 'dotCMS', 'core', [37196]);
    expect(details.get(37196)?.body).toHaveLength(BODY_CHAR_LIMIT);
  });

  it('warns about PRs the query could not resolve, and keeps the rest', async () => {
    const warn = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);
    const graphql = jest.fn(async () => ({
      repository: { pr37196: mockPR(), pr99999: null },
    }));
    const octokit = { graphql } as unknown as Octokit;

    const details = await fetchPRDetails(octokit, 'dotCMS', 'core', [37196, 99999]);
    expect([...details.keys()]).toEqual([37196]);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('#99999'));
    warn.mockRestore();
  });

  it('handles a PR with no body and no labels', async () => {
    const graphql = jest.fn(async () => ({
      repository: {
        pr37196: mockPR({
          body: null,
          labels: null,
          closingIssuesReferences: { nodes: [] },
        }),
      },
    }));
    const octokit = { graphql } as unknown as Octokit;

    const details = await fetchPRDetails(octokit, 'dotCMS', 'core', [37196]);
    expect(details.get(37196)).toMatchObject({ body: '', labels: [], linkedIssues: [] });
  });
});
