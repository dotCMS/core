import * as core from '@actions/core';
import fetch from 'node-fetch';
import { Octokit } from 'octokit';
import { OctokitResponse } from '@octokit/types';

interface GitLogEntry {
    sha: string;
    subject: string;
    body?: string;
    refs: string[];
}

interface GithubIssue {
    id: number;
    number: number;
    title: string;
    body?: string;
    logEntries?: GitLogEntry[];
}

const owner = 'dotcms';
const repo = 'core';
const headers = {
    'X-GitHub-Api-Version': '2022-11-28'
};
const fetchOperation = core.getInput('fetch_operation');
const fetchValue = core.getInput('fetch_value');
const githubToken = core.getInput('github_token');
const octokit = new Octokit({
    request: {
        fetch: fetch
    },
    auth: githubToken
});
const perPage = 30;

export const fetchIssues = async (): Promise<number[]> => {
    if (!fetchOperation) {
        core.warning('No fetch operation provided, aborting');
        return [];
    }

    if (!fetchValue) {
        core.warning('No fetch value provided, aborting');
        return [];
    }

    core.info(`Fetching issues for ${fetchOperation} with value: [${fetchValue}]`);
    let issues: GithubIssue[] | undefined;
    switch (fetchOperation) {
        case 'WITH_LABELS':
            issues = await groupIssues(fetchValue, getIssuesWithLabels);
            break;
        default:
            core.warning(`Unknown fetch operation: ${fetchOperation}`);
            return [];
    }

    core.info(`Found ${issues.length} issues`);
    return issues.map((issue) => issue.number);
};

const getIssuesWithLabels = async (
    labels: string,
    page: number
): Promise<OctokitResponse<GithubIssue[]>> => {
    core.info(
        `Sending request as: 'GET /repos/${owner}/${repo}/issues?labels=${labels}&per_page=${perPage}&page=${page}'`
    );
    const issues: OctokitResponse<GithubIssue[]> = await octokit.request(
        'GET /repos/{owner}/{repo}/issues?labels={labels}&per_page={perPage}&page={page}',
        {
            owner,
            repo,
            headers,
            labels,
            perPage,
            page
        }
    );
    return issues;
};

const groupIssues = async (
    value: string,
    fetchFn: (value: string, page: number) => Promise<OctokitResponse<GithubIssue[]>>
): Promise<GithubIssue[]> => {
    const allIssues: GithubIssue[] = [];
    let issues: OctokitResponse<GithubIssue[]>;
    let page = 1;
    do {
        issues = await fetchFn(value, page++);

        if (issues.status !== 200) {
            core.warning(`Failed to fetch issues: ${issues.status}`);
            break;
        }

        const data = issues.data as GithubIssue[];
        core.info(`Fetched ${data.length} issues`);
        if (data.length > 0) {
            core.info(`Fetched ${data.map((issue) => issue.number)} issues`);
            allIssues.push(...data);
        }
    } while (issues.data.length === perPage);

    return allIssues;
};
