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

export const fetchIssues = async (): Promise<number[]> => {
    if (!fetchOperation) {
        core.warning('No fetch operation provided, aborting');
        return [];
    }

    if (!fetchValue) {
        core.warning('No fetch value provided, aborting');
        return [];
    }

    core.info(`Fetching issues for ${fetchOperation} with value: ${fetchValue}`);
    let issues: OctokitResponse<GithubIssue[]> | undefined;
    switch (fetchOperation) {
        case 'WITH_LABELS':
            issues = await getIssuesWithLabels(fetchValue);
            break;
        default:
            core.warning(`Unknown fetch operation: ${fetchOperation}`);
            return [];
    }

    if (issues.status !== 200) {
        core.warning(`Failed to fetch issues: ${issues.status}`);
        return [];
    }

    core.info(`Found ${issues.data.length} issues`);
    return issues.data.map((issue) => issue.number);
};

const getIssuesWithLabels = async (labels: string): Promise<OctokitResponse<GithubIssue[]>> => {
    const issues: OctokitResponse<GithubIssue[]> = await octokit.request(
        'GET /repos/{owner}/{repo}/issues?labels={labels}',
        {
            owner,
            repo,
            headers,
            labels
        }
    );
    return issues;
};
