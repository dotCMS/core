// Smoke test for find-stuck-issues.js. Runs the script with a stubbed
// `github.graphql` and asserts which issues are flagged.
// Usage: node .github/scripts/qa-stuck-check/test-find-stuck-issues.js

const path = require('path');
const fs = require('fs');
const assert = require('assert');

process.env.PROJECT_OWNER = 'dotCMS';
process.env.PROJECT_NUMBER = '7';
process.env.STUCK_DAYS = '3';
process.env.TRIAGE_CONFIG_PATH = '.claude/triage-config.json';

const run = require('./find-stuck-issues.js');

const now = Date.now();
const daysAgo = (n) => new Date(now - n * 24 * 60 * 60 * 1000).toISOString();

const project = {
  organization: {
    projectV2: {
      items: {
        pageInfo: { hasNextPage: false, endCursor: null },
        nodes: [
          // KEEP: QA, no QA labels, 5d stale, Team:Scout
          mkItem({
            id: 'a',
            updatedAt: daysAgo(5),
            status: 'QA',
            number: 100,
            title: 'Stuck in QA - Scout',
            labels: ['Team : Scout', 'priority:high'],
            assignees: ['nollymar'],
          }),
          // SKIP: has QA : Passed
          mkItem({
            id: 'b',
            updatedAt: daysAgo(10),
            status: 'Done',
            number: 101,
            title: 'Already passed QA',
            labels: ['Team : Falcon', 'QA : Passed'],
            assignees: [],
          }),
          // SKIP: status is In Progress
          mkItem({
            id: 'c',
            updatedAt: daysAgo(7),
            status: 'In Progress',
            number: 102,
            title: 'Not in QA yet',
            labels: ['Team : Falcon'],
            assignees: [],
          }),
          // SKIP: only 1 day stale
          mkItem({
            id: 'd',
            updatedAt: daysAgo(1),
            status: 'QA',
            number: 103,
            title: 'Too fresh',
            labels: ['Team : Maintenance'],
            assignees: [],
          }),
          // SKIP: no Team label that matches config
          mkItem({
            id: 'e',
            updatedAt: daysAgo(20),
            status: 'Done',
            number: 104,
            title: 'No team label',
            labels: [],
            assignees: [],
          }),
          // KEEP: Done, 4d stale, Team:Modernization
          mkItem({
            id: 'f',
            updatedAt: daysAgo(4),
            status: 'Done',
            number: 105,
            title: 'Done but no QA verification - Modernization',
            labels: ['Team : Modernization'],
            assignees: ['hmoreras'],
          }),
          // SKIP: QA : Not Needed
          mkItem({
            id: 'g',
            updatedAt: daysAgo(8),
            status: 'Done',
            number: 106,
            title: 'No QA needed',
            labels: ['Team : Scout', 'QA : Not Needed'],
            assignees: [],
          }),
          // KEEP: QA, 6d stale, Team:Scout (so Scout gets 2 issues)
          mkItem({
            id: 'h',
            updatedAt: daysAgo(6),
            status: 'QA',
            number: 107,
            title: 'Older Scout QA',
            labels: ['Team : Scout'],
            assignees: ['rjvelazco'],
          }),
        ],
      },
    },
  },
};

function mkItem({ id, updatedAt, status, number, title, labels, assignees }) {
  return {
    id,
    updatedAt,
    fieldValues: {
      nodes: [
        {
          __typename: 'ProjectV2ItemFieldSingleSelectValue',
          name: status,
          field: { name: 'Status' },
        },
      ],
    },
    content: {
      __typename: 'Issue',
      number,
      title,
      url: `https://github.com/dotCMS/core/issues/${number}`,
      state: status === 'Done' ? 'CLOSED' : 'OPEN',
      assignees: { nodes: assignees.map((login) => ({ login })) },
      labels: { nodes: labels.map((name) => ({ name })) },
    },
  };
}

const captured = {};
const fakeCore = {
  info: (m) => console.log('[info]', m),
  setOutput: (k, v) => {
    captured[k] = v;
  },
};
const fakeGithub = {
  graphql: async () => project,
};

(async () => {
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  console.log('\n--- groups ---');
  console.log(JSON.stringify(groups, null, 2));
  console.log('\n--- summary ---');
  console.log(captured.summary);

  assert.strictEqual(captured.has_results, 'true', 'expected results');
  const byTeam = Object.fromEntries(groups.map((g) => [g.team, g]));

  assert.ok(byTeam['Team : Scout'], 'Scout group present');
  assert.strictEqual(
    byTeam['Team : Scout'].issues.length,
    2,
    'Scout has 2 issues',
  );
  assert.strictEqual(byTeam['Team : Scout'].slack_channel, 'CQNF9PCFQ');

  assert.ok(byTeam['Team : Modernization'], 'Modernization group present');
  assert.strictEqual(byTeam['Team : Modernization'].issues.length, 1);
  assert.strictEqual(
    byTeam['Team : Modernization'].slack_channel,
    'C09LGH4HNR5',
  );

  assert.ok(!byTeam['Team : Falcon'], 'Falcon excluded (skipped + not stuck)');
  assert.ok(
    !byTeam['Team : Maintenance'],
    'Maintenance excluded (too fresh)',
  );

  // Scout's most-stuck issue should be #107 (6d) sorted before #100 (5d)
  assert.strictEqual(byTeam['Team : Scout'].issues[0].number, 107);

  console.log('\nAll assertions passed.');
})().catch((e) => {
  console.error(e);
  process.exit(1);
});