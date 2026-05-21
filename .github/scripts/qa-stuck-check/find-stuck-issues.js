// Finds issues stuck in QA/Done on the dotCMS - Product Planning project.
// An issue is "stuck" when:
//   - Its project Status is one of TARGET_STATUSES (QA, Done)
//   - It does NOT carry any SKIP_LABELS (QA : Passed, QA : Not Needed)
//   - The ProjectV2Item has not changed in at least STUCK_DAYS days
//   - It carries a "Team : <name>" label that maps to a team configured with
//     a slack_channel in .claude/triage-config.json
//
// Groups results by team for downstream Slack posting.
//
// Invoked from actions/github-script; gets { github, core, ... }.
// Outputs (core.setOutput):
//   groups_json  JSON array of { team, slack_channel, issues: [...] }
//   has_results  'true' | 'false'
//   summary      markdown summary for the job log

const fs = require('fs');
const path = require('path');

const PROJECT_OWNER = process.env.PROJECT_OWNER || 'dotCMS';
const PROJECT_NUMBER = parseInt(process.env.PROJECT_NUMBER || '7', 10);
const STATUS_FIELD_NAME = process.env.STATUS_FIELD_NAME || 'Status';
const TARGET_STATUSES = (process.env.TARGET_STATUSES || 'QA,Done')
  .split(',')
  .map((s) => s.trim().toLowerCase());
const SKIP_LABELS = (process.env.SKIP_LABELS || 'QA : Passed,QA : Not Needed')
  .split(',')
  .map((s) => s.trim().toLowerCase());
const TEAM_LABEL_PREFIX = (process.env.TEAM_LABEL_PREFIX || 'Team : ')
  .toLowerCase();
const STUCK_DAYS = parseInt(process.env.STUCK_DAYS || '3', 10);
const TRIAGE_CONFIG_PATH =
  process.env.TRIAGE_CONFIG_PATH || '.claude/triage-config.json';

module.exports = async ({ github, core }) => {
  const triageConfig = JSON.parse(
    fs.readFileSync(path.resolve(TRIAGE_CONFIG_PATH), 'utf8'),
  );
  const teamChannels = buildTeamChannelIndex(triageConfig);

  const items = await fetchAllProjectItems(github);
  core.info(`Fetched ${items.length} project items from #${PROJECT_NUMBER}`);

  const cutoff = new Date(Date.now() - STUCK_DAYS * 24 * 60 * 60 * 1000);
  const stuck = [];

  for (const item of items) {
    const issue = item.content;
    if (!issue || issue.__typename !== 'Issue') continue;

    const status = readStatus(item);
    if (!status || !TARGET_STATUSES.includes(status.toLowerCase())) continue;

    const labels = (issue.labels?.nodes || []).map((l) => l.name);
    const labelsLower = labels.map((l) => l.toLowerCase());
    if (labelsLower.some((l) => SKIP_LABELS.includes(l))) continue;

    const itemUpdatedAt = item.updatedAt ? new Date(item.updatedAt) : null;
    if (!itemUpdatedAt || itemUpdatedAt > cutoff) continue;

    const team = resolveTeamFromLabels(labels, teamChannels);
    if (!team) continue;

    const daysStuck = Math.floor(
      (Date.now() - itemUpdatedAt.getTime()) / (24 * 60 * 60 * 1000),
    );

    stuck.push({
      team,
      issue: {
        number: issue.number,
        title: issue.title,
        url: issue.url,
        status,
        labels,
        assignees: (issue.assignees?.nodes || []).map((a) => a.login),
        itemUpdatedAt: itemUpdatedAt.toISOString(),
        daysStuck,
      },
    });
  }

  const grouped = groupByTeam(stuck, teamChannels);
  core.setOutput('groups_json', JSON.stringify(grouped));
  core.setOutput('has_results', grouped.length > 0 ? 'true' : 'false');
  core.setOutput('summary', buildSummary(grouped, STUCK_DAYS));
  core.info(
    `Found ${stuck.length} stuck issue(s) across ${grouped.length} team(s)`,
  );
};

function buildTeamChannelIndex(triageConfig) {
  const idx = {};
  for (const [team, cfg] of Object.entries(triageConfig.teams || {})) {
    if (cfg.slack_channel) idx[team.toLowerCase()] = { team, channel: cfg.slack_channel };
  }
  return idx;
}

function resolveTeamFromLabels(labels, teamChannels) {
  for (const label of labels) {
    const lower = label.toLowerCase();
    if (!lower.startsWith(TEAM_LABEL_PREFIX)) continue;
    const hit = teamChannels[lower];
    if (hit) return hit.team;
  }
  return null;
}

function readStatus(item) {
  const node = (item.fieldValues?.nodes || []).find(
    (n) => n.field && n.field.name === STATUS_FIELD_NAME,
  );
  return node ? node.name : null;
}

function groupByTeam(stuck, teamChannels) {
  const byTeam = {};
  for (const entry of stuck) {
    if (!byTeam[entry.team]) byTeam[entry.team] = [];
    byTeam[entry.team].push(entry.issue);
  }
  return Object.entries(byTeam).map(([team, issues]) => ({
    team,
    slack_channel: teamChannels[team.toLowerCase()].channel,
    issues: issues.sort((a, b) => b.daysStuck - a.daysStuck),
  }));
}

function buildSummary(groups, stuckDays) {
  if (groups.length === 0) {
    return `No issues stuck in QA for ${stuckDays}+ days.`;
  }
  const lines = [`# QA-stuck issues (${stuckDays}+ days since last project update)`, ''];
  for (const g of groups) {
    lines.push(`## ${g.team} → ${g.slack_channel} (${g.issues.length})`);
    for (const e of g.issues) {
      const assignees = e.assignees.length
        ? ` · assignees: ${e.assignees.map((a) => '@' + a).join(', ')}`
        : '';
      lines.push(
        `- [#${e.number}](${e.url}) — ${e.title} · status: ${e.status} · ${e.daysStuck}d stuck${assignees}`,
      );
    }
    lines.push('');
  }
  return lines.join('\n');
}

async function fetchAllProjectItems(github) {
  const all = [];
  let cursor = null;
  do {
    const data = await github.graphql(PROJECT_ITEMS_QUERY, {
      org: PROJECT_OWNER,
      number: PROJECT_NUMBER,
      cursor,
    });
    const items = data.organization.projectV2.items;
    all.push(...items.nodes);
    cursor = items.pageInfo.hasNextPage ? items.pageInfo.endCursor : null;
  } while (cursor);
  return all;
}

const PROJECT_ITEMS_QUERY = `
  query($org: String!, $number: Int!, $cursor: String) {
    organization(login: $org) {
      projectV2(number: $number) {
        items(first: 50, after: $cursor) {
          pageInfo { hasNextPage endCursor }
          nodes {
            id
            updatedAt
            fieldValues(first: 20) {
              nodes {
                __typename
                ... on ProjectV2ItemFieldSingleSelectValue {
                  name
                  field { ... on ProjectV2SingleSelectField { name } }
                }
              }
            }
            content {
              __typename
              ... on Issue {
                number
                title
                url
                state
                assignees(first: 10) { nodes { login } }
                labels(first: 30) { nodes { name } }
              }
            }
          }
        }
      }
    }
  }
`;