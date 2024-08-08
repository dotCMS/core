import * as core from '@actions/core';
import fetch from 'node-fetch';
import { Octokit } from 'octokit';
import { OctokitResponse } from '@octokit/types';
import * as shelljs from 'shelljs';

interface Committer {
    name: string;
    email: string;
    date: string;
}

interface Commit {
    url: string;
    sha: string;
    commit: {
        url: string;
        author: Committer;
        committer: Committer;
        message: string;
    };
}

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
const projectRoot = core.getInput('project_root');
const branch = core.getInput('branch');
const initialSha = core.getInput('initial_sha');
const lastSha = core.getInput('last_sha');
const githubToken = core.getInput('github_token');
const octokit = new Octokit({
    request: {
        fetch: fetch
    },
    auth: githubToken
});
const gitLogSeparator = '|||||';

export const generateChangeLog = async () => {
    const initialCommit: OctokitResponse<Commit> = await getCommit(initialSha);
    if (initialCommit.status !== 200) {
        core.error('Initial commit not found, aborting');
        return;
    }

    const lastCommit: OctokitResponse<Commit> = await getCommit(lastSha);
    if (lastCommit.status !== 200) {
        core.error('Last commit not found, aborting');
        return;
    }

    const commits = getCommits(initialCommit.data.sha, lastCommit.data.sha);
    const issues = await getIssues(commits);
    const issuesFlat = issues.map((issue) => issue.number);

    core.setOutput('commits_json', JSON.stringify(commits, null, 0));
    core.setOutput('issues_json', JSON.stringify(issues, null, 0));
    core.setOutput('issues_flat', JSON.stringify(issuesFlat, null, 0));

    core.info(`Issues found:\n${JSON.stringify(issuesFlat, null, 2)}`);
};

const getCommit = async (ref: string): Promise<OctokitResponse<Commit>> => {
    return (await octokit.request('GET /repos/{owner}/{repo}/commits/{ref}', {
        owner,
        repo,
        ref,
        headers
    })) as OctokitResponse<Commit>;
};

const parseEntry = (
    lines: string[],
    minumLength: number,
    predicate: (value: string, index: number, array: string[]) => boolean
): string[] => {
    if (lines.length < minumLength) {
        return [];
    }

    const result = lines
        .slice(minumLength - 1)
        .filter((line) => line.trim().length > 0)
        .filter(predicate);

    return result.length > 0 ? result : [];
};

const parseBody = (lines: string[]): string | undefined => {
    return parseEntry(lines, 3, (line) => !/^(Co-authored-by|Refs):.*/g.test(line.trim()))
        .join('\n')
        .trim();
};

const parseRefs = (lines: string[]): string[] => {
    const refs: string[] = [];
    parseEntry(lines, 3, (line) => /^Refs:.*/g.test(line.trim())).forEach((line) => {
        refs.push(
            ...line
                .replace('Refs:', '')
                .trim()
                .split(',')
                .map((ref) => ref.trim())
                .filter((ref) => !refs.includes(ref))
                .map((ref) => (ref.startsWith('#') ? ref.substring(1) : ref))
        );
    });
    return refs;
};

const parseGitLogEntry = (entry: string): GitLogEntry => {
    const lines = entry.trim().split('\n');
    return {
        sha: lines[0],
        subject: lines[1],
        body: parseBody(lines),
        refs: parseRefs(lines)
    } as GitLogEntry;
};

const subjectFilter = (entry: GitLogEntry): boolean => {
    return /^(feat|fix|perf|refactor|docs|style|ci|chore|test)(\(.+\))?:.*$/g.test(entry.subject);
};

const getCommits = (fromSha: string, toSha: string): GitLogEntry[] => {
    const cmd = `git log ${branch} ${fromSha}..${toSha} --pretty="${gitLogSeparator}%n%H%n%s%n%b"`;
    core.info(`Executing command: ${cmd}`);
    const output =
        shelljs.exec(cmd, {
            silent: true,
            cwd: projectRoot
        })?.stdout || '';
    const filtered = output
        .split(gitLogSeparator)
        .map(parseGitLogEntry)
        .filter((e: GitLogEntry) => !!e.sha && !!e.refs)
        .filter(subjectFilter);
    if (filtered.length === 0) {
        core.info('No commits found, aborting');
    } else {
        core.info(`Filtered commits:\n${JSON.stringify(filtered, null, 2)}`);
    }

    return filtered;
};

const extractIssueNumbers = (refs: string[]): number[] => {
    core.info(`Extracting issues numbers from refs: ${refs}`);
    return refs.map(parseInt).filter((n: number | undefined) => !!n) as number[];
};

const fetchIssue = async (issueNumber: number): Promise<OctokitResponse<GithubIssue>> => {
    core.info(`Fetching issue ${issueNumber}`);
    return (await octokit.request('GET /repos/{owner}/{repo}/issues/{issueNumber}', {
        owner,
        repo,
        issueNumber,
        headers
    })) as OctokitResponse<GithubIssue>;
};

const getIssues = async (logEntries: GitLogEntry[]): Promise<GithubIssue[]> => {
    const issues: GithubIssue[] = [];

    for (const entry of logEntries) {
        core.info(`Getting issues for sha [${entry.sha}]`);
        const issueNumbers = extractIssueNumbers(entry.refs);
        for (const issueNumber of issueNumbers) {
            core.info(`Processing issue ${issueNumber} found in sha [${entry.sha}]`);
            let issue = issues.find((i) => i.number === issueNumber);
            if (!issue) {
                const found = await fetchIssue(issueNumber);
                if (found.status !== 200) {
                    core.warning(`Issue ${issueNumber} not found, skipping`);
                    continue;
                }

                issue = {
                    id: found.data.id,
                    number: found.data.number,
                    title: found.data.title,
                    body: found.data.body,
                    logEntries: []
                };
                issues.push(issue);
            }

            if (!issue.logEntries?.find((logEntry) => logEntry.sha === entry.sha)) {
                issue.logEntries?.push(entry);
            }
        }
    }

    return issues;
};
