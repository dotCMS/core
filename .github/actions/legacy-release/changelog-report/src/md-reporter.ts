import * as core from '@actions/core';

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
    url?: string;
    commits?: string;
}

interface ShowableField {
    field: string;
    label: string;
    width: number;
}

const issues = JSON.parse(core.getInput('issues_json') || '[]') as GithubIssue[];
const tag = core.getInput('tag');
const colSeparator = '|';
const lineChar = '-';
const repoUrl = 'https://github.com/dotCMS/core/issues';
const showableFields: ShowableField[] = [
    {
        field: 'number',
        label: 'Issue',
        width: 5
    },
    {
        field: 'title',
        label: 'Title',
        width: 5
    },
    {
        field: 'url',
        label: 'Link',
        width: 4
    },
    {
        field: 'logEntries',
        label: 'Commits',
        width: 7
    }
];

export const report = () => {
    const issueNumbers = issues.map((issue) => issue.number);
    core.info(`Generating markdown report from changelog issues: ${JSON.stringify(issueNumbers)}`);

    enrichIssues();
    calculateWidths();
    showReport();
};

const enrichIssues = () => {
    return issues.forEach((issue) => {
        issue.url = `${repoUrl}/${issue.number}`;
        issue.commits = issue.logEntries?.map(formatEntry).join('\n');
    });
};

const formatEntry = (entry: GitLogEntry) => {
    return `- *${entry.sha}*: ${entry.subject.trim()}`;
};

const calculateWidths = () => {
    for (const issue of issues) {
        for (const field of showableFields) {
            switch (field.field) {
                case 'number': {
                    if (issue.number.toString().length > field.width) {
                        field.width = issue.number;
                    }
                    break;
                }
                case 'title': {
                    const value = issue.title.trim();
                    if (value.length > field.width) {
                        field.width = value.length;
                    }
                    break;
                }
                case 'url': {
                    const value = issue.title.trim();
                    if (value.length > field.width) {
                        field.width = value.length;
                    }
                    break;
                }
                case 'logEntries': {
                    const value = issue.commits?.trim();
                    const subjects = value?.split('\n');
                    for (const subject of subjects || []) {
                        if (subject.length + 2 > field.width) {
                            field.width = subject.length + 2;
                        }
                    }
                }
            }
        }
    }
};

const showReport = () => {
    print(`# Changelog for tag ${tag}`);
    print('\n');
    showHeaders();
    showIssues();
};

const showHeaders = () => {
    let headers = ``;
    for (const field of showableFields) {
        headers += `${colSeparator} ${buildHeader(field, ' ')} `;
    }
    headers += `${colSeparator}`;
    print(headers);

    let line = ``;
    for (const field of showableFields) {
        line += `${colSeparator}${repeatLine(field)}`;
    }
    line += `${colSeparator}`;
    print(line);
};

const showIssues = () => {
    for (const issue of issues) {
        let issueLine = ``;
        for (const field of showableFields) {
            switch (field.field) {
                case 'number': {
                    issueLine += `${colSeparator} ${buildText(
                        issue.number.toString(),
                        ' ',
                        field.width
                    )} `;
                    break;
                }
                case 'title': {
                    issueLine += `${colSeparator} ${buildText(
                        issue.title.trim(),
                        ' ',
                        field.width
                    )} `;
                    break;
                }
                case 'url': {
                    issueLine += `${colSeparator} ${buildText(issue.url || '', ' ', field.width)} `;
                    break;
                }
                case 'logEntries': {
                    issueLine += `${colSeparator} ${buildText(
                        issue.commits || '',
                        ' ',
                        field.width
                    )} `;
                    break;
                }
            }
        }
        issueLine += colSeparator;
        print(issueLine);
    }
};

const print = (text?: string) => {
    core.info(text || '');
};

const buildText = (text: string, repeat: string, times: number): string => {
    return `${text}${repeat.repeat(Math.abs(times - text.length))}`;
};

const buildHeader = (field: ShowableField, repeat: string): string => {
    return `${field.label}${repeat.repeat(Math.abs(field.width - field.label.length))}`;
};

const repeatLine = (field: ShowableField): string => {
    return `${lineChar.repeat(field.width + 2)}`;
};
