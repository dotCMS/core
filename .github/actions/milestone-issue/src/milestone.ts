import * as core from '@actions/core';
import * as fs from 'fs';
import fetch from 'node-fetch';
import * as path from 'path';
import { Octokit } from 'octokit';
import { OctokitResponse } from '@octokit/types';

enum MilestoneOperation {
    ADD,
    REMOVE,
    NONE
}

interface Team {
    id: string;
    name: string;
    label: string;
    project_url: string;
    members: string[];
    current_milestone: string;
}

interface MilestoneRule {
    field: string;
    matching_values: string[];
    non_matching_values: string[];
    team: string;
    operation?: MilestoneOperation;
}

interface Event {
    action: string;
    issue: Issue;
    label: Label;
}

interface Issue {
    number: number;
    title?: string | number;
    body?: string;
    html_url: string;
    id: number;
    labels: Label[];
    state: string;
    url: string;
    milestone: string | number | null;
}

interface Label {
    color: string;
    default: boolean;
    description: string;
    id: number;
    name: string;
    node_id: string;
    url: string;
}

interface Milestone {
    id: number;
    number: number;
    state: string;
    title: string;
}

const token = core.getInput('token');
const owner = 'dotcms';
const repo = 'core';
const headers = {
    'X-GitHub-Api-Version': '2022-11-28'
};
const octokit = new Octokit({
    request: {
        fetch: fetch
    },
    auth: token
});

export const assignMilestone = async (
    confDir: string,
    teamsStr: string,
    field: string,
    eventStr: string
) => {
    const teams: Team[] = (JSON.parse(readFromFile(confDir, 'teams.json'))?.teams || []) as Team[];
    const event: Event = JSON.parse(eventStr);
    console.log(`Processing event:\n${JSON.stringify(event, null, 2)}`);
    const issueLabels = event.issue.labels.map((label) => label.name);

    const resolvedTeams: Team[] = (
        (JSON.parse(teamsStr) as string[])
            .map((t) => t.trim().toLowerCase())
            .map((t) => teams.find((team) => team.id === t)) as Team[]
    ).filter((team) => issueLabels.some((label) => label === team.label));
    if (resolvedTeams.length === 0) {
        throw new Error('No teams provided');
    }
    if (resolvedTeams.length > 1) {
        throw new Error('Issue has more than one team label');
    }

    const resolvedTeam = resolvedTeams[0];
    console.log(`Resolved team:\n${JSON.stringify(resolvedTeam, null, 2)}`);

    const eventValue = getEventValue(event, field);
    const milestoneRules: MilestoneRule[] = (
        (JSON.parse(readFromFile(confDir, 'milestone-issue.json'))?.milestone_rules ||
            []) as MilestoneRule[]
    )
        .filter((rule) => rule.field === field)
        .filter(
            (rule) =>
                rule.matching_values.some((value) => value === eventValue) ||
                rule.non_matching_values.some((value) => value === eventValue)
        );
    if (milestoneRules.length === 0) {
        throw new Error(
            `No milestone rules were resolved for field ${field} and event value ${eventValue}`
        );
    }
    console.log(`Resolved milestone rules:\n${JSON.stringify(milestoneRules, null, 2)}`);

    const resolvedRule: MilestoneRule | undefined = resolveRule(
        event,
        field,
        resolvedTeam,
        milestoneRules
    );

    await handleMilestone(resolvedRule, event.issue, resolvedTeam);
};

const readFromFile = (configDir: string, file: string): string => {
    const confFile = path.join(configDir, file);
    console.log(`Reading config file from [${confFile}]`);

    return fs.readFileSync(confFile, {
        encoding: 'utf8',
        flag: 'r'
    });
};

const getEventValue = (event: Event, field: string): string | undefined => {
    switch (field) {
        case 'LABEL':
            return event.label.name;
        case 'PROJECT_STATUS':
            return undefined;
        default:
            return undefined;
    }
};

const resolveRule = (
    event: Event,
    field: string,
    team: Team,
    milestoneRules: MilestoneRule[]
): MilestoneRule | undefined => {
    const rules = milestoneRules.filter((rule) => rule.team === team.id);
    if (rules.length === 0) {
        return undefined;
    }

    switch (field) {
        case 'LABEL':
            return resolveMilestoneByLabel(event, rules);
        case 'PROJECT_STATUS':
            return resolveMilestoneByStatus(/*event, rules*/);
        default:
            return undefined;
    }
};

const hasAllValues = (values: string[], labels: string[]): boolean => {
    return values.every((value) => labels.includes(value));
};

const resolveMilestoneByLabel = (
    event: Event,
    rules: MilestoneRule[]
): MilestoneRule | undefined => {
    const issueLabels = event.issue.labels.map((label) => label.name);
    console.log(`Evaluating issue labels: [${issueLabels}]`);
    const addFilter = rules
        .filter((rule) => rule.matching_values.some((value) => issueLabels.includes(value)))
        .filter((rule) => !hasAllValues(rule.non_matching_values, issueLabels));
    if (addFilter.length > 0) {
        const toAdd = addFilter[0];
        toAdd.operation = MilestoneOperation.ADD;
        return toAdd;
    }

    const removeFilter = rules
        .filter((rule) => rule.non_matching_values.some((label) => issueLabels.includes(label)))
        .filter((rule) => !hasAllValues(rule.matching_values, issueLabels));
    if (removeFilter.length > 0) {
        const toRemove = removeFilter[0];
        toRemove.operation = MilestoneOperation.REMOVE;
        return toRemove;
    }

    return undefined;
};

const resolveMilestoneByStatus = () /*event: Event,
  team: Team,
  milestoneRules: MilestoneRule[]*/
: MilestoneRule | undefined => {
    return undefined;
};

const handleMilestone = async (rule: MilestoneRule | undefined, issue: Issue, team: Team) => {
    if (!rule) {
        console.log('Cannot resolve rule, aborting');
        return;
    }

    console.log(`Handling resolved milestone rule:\n${JSON.stringify(rule, null, 2)}`);

    const milestone: string | undefined =
        rule.operation === MilestoneOperation.ADD ? team.current_milestone : undefined;
    await updateIssue(issue.number, milestone);
};

const getMilestones = async (): Promise<OctokitResponse<Milestone[]>> => {
    console.log('Getting milestones');
    return await octokit.request('GET /repos/{owner}/{repo}/milestones', {
        owner,
        repo,
        headers
    });
};

const getMilestone = async (milestoneNumber: number): Promise<OctokitResponse<Milestone>> => {
    console.log(`Getting milestone [${milestoneNumber}]`);
    return await octokit.request('GET /repos/{owner}/{repo}/milestones/{milestone_number}', {
        owner,
        repo,
        milestone_number: milestoneNumber,
        headers
    });
};

const createMilestone = async (milestone: string): Promise<OctokitResponse<Milestone>> => {
    const milestones = await getMilestones();

    const found = milestones.data.find((m) => m.title === milestone);
    if (found) {
        console.log(`Milestone ${milestone} already exists:\n${JSON.stringify(found)}`);
        return getMilestone(found.number);
    }

    console.log(`Creating milestone: [${milestone}]`);
    const created = await octokit.request('POST /repos/{owner}/{repo}/milestones', {
        owner,
        repo,
        title: milestone,
        headers
    });
    console.log(`Milestone:\n${JSON.stringify(created, null, 2)}`);

    return created as OctokitResponse<Milestone>;
};

const updateIssue = async (
    issueNumber: number,
    milestone: string | undefined
): Promise<OctokitResponse<Issue>> => {
    console.log(`Updating issue #${issueNumber} with milestone [${milestone}]`);

    const created = milestone ? await createMilestone(milestone) : undefined;
    const milestoneId = created && created.status === 201 ? created.data.number : undefined;

    const updated: OctokitResponse<Issue> = (await octokit.request(
        'PATCH /repos/{owner}/{repo}/issues/{issue_number}',
        {
            owner,
            repo,
            issue_number: issueNumber,
            milestone: milestoneId,
            headers
        }
    )) as OctokitResponse<Issue>;
    console.log(`Updated issue:\n${JSON.stringify(updated, null, 2)}`);

    return updated;
};
