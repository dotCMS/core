import * as core from '@actions/core';
import fetch from 'node-fetch';
import { Octokit } from 'octokit';
import { OctokitResponse } from '@octokit/types';

interface LabelResponse {
    id: number;
    name: string;
    description: string;
    color: string;
}

const owner = 'dotcms';
const repo = 'core';
const headers = {
    'X-GitHub-Api-Version': '2022-11-28'
};
const issues = JSON.parse(core.getInput('issues_json')) as number[];
const labelsStr = core.getInput('labels');
const labels = labelsStr.split(',');
const operation = core.getInput('operation').toUpperCase();
const githubToken = core.getInput('github_token');
const octokit = new Octokit({
    request: {
        fetch: fetch
    },
    auth: githubToken
});

export const labelIssues = async () => {
    if (issues.length === 0) {
        core.warning('No issues found, aborting');
        return;
    }
    if (labels.length === 0) {
        core.warning('No labels found, aborting');
        return;
    }
    if (!['ADD', 'REMOVE'].includes(operation)) {
        core.info('No operation specified, aborting');
        return;
    }

    const affected: number[] = [];
    const ignored: number[] = [];

    for (const issue of issues) {
        core.info(`Processing labels ${labelsStr} for issue ${issue} with operation ${operation}`);
        if (operation === 'ADD') {
            const added = await labelIssue(issue);
            if (added.status === 200) {
                affected.push(issue);
            } else {
                ignored.push(issue);
            }
        } else if (operation === 'REMOVE') {
            for (const label of labels) {
                const removed = await unlabelIssue(issue, label);
                if (removed.status === 200) {
                    affected.push(issue);
                } else {
                    ignored.push(issue);
                }
            }
        }
    }

    const affectedOutput = JSON.stringify(affected, null, 0);
    const ignoredOutput = JSON.stringify(ignored, null, 0);
    core.info(`Affected issues: ${affectedOutput}`);
    core.info(`Ignored issues: ${ignoredOutput}`);
    core.setOutput('affected_issues', affectedOutput);
    core.setOutput('ignored_issues', ignoredOutput);
};

const labelIssue = async (issueNumber: number): Promise<OctokitResponse<LabelResponse[]>> => {
    return (await octokit.request('POST /repos/{owner}/{repo}/issues/{issueNumber}/labels', {
        owner,
        repo,
        issueNumber,
        labels,
        headers
    })) as OctokitResponse<LabelResponse[]>;
};

const unlabelIssue = async (
    issueNumber: number,
    label: string
): Promise<OctokitResponse<LabelResponse[]>> => {
    return (await octokit.request(
        'DELETE /repos/{owner}/{repo}/issues/{issueNumber}/labels/{label}',
        {
            owner,
            repo,
            issueNumber,
            label,
            headers
        }
    )) as OctokitResponse<LabelResponse[]>;
};
