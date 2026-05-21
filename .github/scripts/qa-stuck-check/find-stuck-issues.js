// Finds project items in QA/Done that have not seen any project-field change
// in STUCK_DAYS days on the dotCMS - Product Planning project.
//
// IMPORTANT: the metric is "ProjectV2Item.updatedAt is N+ days ago". The GitHub
// Projects v2 GraphQL API does not expose status-change history, so this is a
// proxy for "no QA progress". Any project-field edit (Priority, Iteration, an
// automation move, etc.) resets the clock. This intentionally undercounts
// rather than overcounts — the Slack message wording reflects that.
//
// Filters applied per project item:
//   - content is an Issue
//   - project Status is one of TARGET_STATUSES (case-insensitive)
//   - none of SKIP_LABELS are present
//   - item.updatedAt is at least STUCK_DAYS ago
//   - the issue has one or more "Team : <name>" labels that map to a team
//     with a slack_channel in .claude/triage-config.json. If the issue has
//     multiple matching team labels, it is reported under EACH team.
//
// Invoked from actions/github-script; gets { github, core }.
// Outputs (core.setOutput):
//   groups_json  JSON array of { team, slack_channel, stuck_days, issues: [...] }
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

// If fewer than this fraction of project items have a Status field value, we
// assume the GraphQL `fieldValues(first:N)` cap is hiding it and fail loudly.
const STATUS_PRESENCE_MIN_RATIO = 0.5;

module.exports = async ({ github, core }) => {
  const triageConfig = JSON.parse(
    fs.readFileSync(path.resolve(TRIAGE_CONFIG_PATH), 'utf8'),
  );
  const teamChannels = buildTeamChannelIndex(triageConfig);

  const items = await fetchAllProjectItems(github, core);
  core.info(`Fetched ${items.length} project items from #${PROJECT_NUMBER}`);

  assertStatusFieldVisible(items, core);

  const cutoff = new Date(Date.now() - STUCK_DAYS * 24 * 60 * 60 * 1000);
  const stuckByTeam = new Map();

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

    const matchedTeams = resolveTeamsFromLabels(labels, teamChannels);
    if (matchedTeams.length === 0) continue;

    const daysStuck = Math.floor(
      (Date.now() - itemUpdatedAt.getTime()) / (24 * 60 * 60 * 1000),
    );
    const issueRecord = {
      number: issue.number,
      title: issue.title,
      url: issue.url,
      status,
      labels,
      assignees: (issue.assignees?.nodes || []).map((a) => a.login),
      itemUpdatedAt: itemUpdatedAt.toISOString(),
      daysStuck,
    };

    for (const match of matchedTeams) {
      if (!stuckByTeam.has(match.team)) {
        stuckByTeam.set(match.team, { channel: match.channel, issues: [] });
      }
      stuckByTeam.get(match.team).issues.push(issueRecord);
    }
  }

  const grouped = Array.from(stuckByTeam.entries()).map(([team, v]) => ({
    team,
    slack_channel: v.channel,
    stuck_days: STUCK_DAYS,
    issues: v.issues.sort((a, b) => b.daysStuck - a.daysStuck),
  }));

  core.setOutput('groups_json', JSON.stringify(grouped));
  core.setOutput('has_results', grouped.length > 0 ? 'true' : 'false');
  core.setOutput('summary', buildSummary(grouped, STUCK_DAYS));
  core.info(
    `Found ${grouped.reduce((n, g) => n + g.issues.length, 0)} issue-team pairing(s) across ${grouped.length} team(s)`,
  );
};

function buildTeamChannelIndex(triageConfig) {
  const idx = {};
  for (const [team, cfg] of Object.entries(triageConfig.teams || {})) {
    if (cfg.slack_channel) {
      idx[team.toLowerCase()] = { team, channel: cfg.slack_channel };
    }
  }
  return idx;
}

function resolveTeamsFromLabels(labels, teamChannels) {
  const matches = [];
  const seen = new Set();
  for (const label of labels) {
    const lower = label.toLowerCase();
    if (!lower.startsWith(TEAM_LABEL_PREFIX)) continue;
    const hit = teamChannels[lower];
    if (!hit || seen.has(hit.team)) continue;
    seen.add(hit.team);
    matches.push(hit);
  }
  return matches;
}

function readStatus(item) {
  const node = (item.fieldValues?.nodes || []).find(
    (n) => n.field && n.field.name === STATUS_FIELD_NAME,
  );
  return node ? node.name : null;
}

function assertStatusFieldVisible(items, core) {
  if (items.length === 0) return;
  const withStatus = items.filter((i) => readStatus(i) !== null).length;
  const ratio = withStatus / items.length;
  if (ratio < STATUS_PRESENCE_MIN_RATIO) {
    const msg =
      `Only ${withStatus}/${items.length} project items expose a "${STATUS_FIELD_NAME}" field value. ` +
      'Either the field is named differently or it falls outside the GraphQL fieldValues(first:N) page. ' +
      'Bump the cap in the query or update STATUS_FIELD_NAME.';
    core.setFailed(msg);
    throw new Error(msg);
  }
  if (withStatus < items.length) {
    core.warning(
      `${items.length - withStatus} project item(s) had no readable "${STATUS_FIELD_NAME}" field value and were ignored.`,
    );
  }
}

function buildSummary(groups, stuckDays) {
  if (groups.length === 0) {
    return `No QA/Done issues with ${stuckDays}+ days of project inactivity.`;
  }
  const total = groups.reduce((n, g) => n + g.issues.length, 0);
  const lines = [
    `# QA/Done issues with no project activity for ${stuckDays}+ days`,
    '',
    `_Total: ${total} issue-team pairing(s) across ${groups.length} team(s). Metric: ProjectV2Item.updatedAt — any project-field edit resets the clock._`,
    '',
  ];
  for (const g of groups) {
    lines.push(`## ${g.team} → ${g.slack_channel} (${g.issues.length})`);
    for (const e of g.issues) {
      const assignees = e.assignees.length
        ? ` · assignees: ${e.assignees.join(', ')}`
        : '';
      lines.push(
        `- [#${e.number}](${e.url}) — ${e.title} · status: ${e.status} · last project update ${e.daysStuck}d ago${assignees}`,
      );
    }
    lines.push('');
  }
  return lines.join('\n');
}

async function fetchAllProjectItems(github, core) {
  const all = [];
  let cursor = null;
  do {
    const data = await github.graphql(PROJECT_ITEMS_QUERY, {
      org: PROJECT_OWNER,
      number: PROJECT_NUMBER,
      cursor,
    });
    const project = data?.organization?.projectV2;
    if (!project) {
      const msg =
        `Could not read projectV2 #${PROJECT_NUMBER} for org "${PROJECT_OWNER}". ` +
        'Either the project does not exist or the token lacks the read:project scope.';
      core.setFailed(msg);
      throw new Error(msg);
    }
    const items = project.items;
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
            fieldValues(first: 50) {
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
                assignees(first: 20) { nodes { login } }
                labels(first: 50) { nodes { name } }
              }
            }
          }
        }
      }
    }
  }
`;