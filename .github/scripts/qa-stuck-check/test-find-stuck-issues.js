// Smoke tests for find-stuck-issues.js. Stubs `github.graphql` and asserts
// what gets flagged / grouped under each scenario.
//
// Usage: node .github/scripts/qa-stuck-check/test-find-stuck-issues.js

const assert = require('assert');
const path = require('path');

const SCRIPT_PATH = path.resolve(
  __dirname,
  './find-stuck-issues.js',
);

const now = Date.now();
const daysAgo = (n) => new Date(now - n * 24 * 60 * 60 * 1000).toISOString();

function mkItem({ id, updatedAt, status, number, title, labels, assignees, omitStatusField, labelsHasNextPage, state }) {
  const fieldValues = omitStatusField
    ? { nodes: [] }
    : {
        nodes: [
          {
            __typename: 'ProjectV2ItemFieldSingleSelectValue',
            name: status,
            field: { name: 'Status' },
          },
        ],
      };
  return {
    id,
    updatedAt,
    fieldValues,
    content: {
      __typename: 'Issue',
      number,
      title,
      url: `https://github.com/dotCMS/core/issues/${number}`,
      state: state || (status === 'Done' ? 'CLOSED' : 'OPEN'),
      assignees: { nodes: (assignees || []).map((login) => ({ login })) },
      labels: {
        pageInfo: { hasNextPage: !!labelsHasNextPage },
        nodes: (labels || []).map((name) => ({ name })),
      },
    },
  };
}

function freshModule() {
  // find-stuck-issues.js reads env vars at module load, so re-require it after
  // resetting envs in each test.
  delete require.cache[SCRIPT_PATH];
  return require(SCRIPT_PATH);
}

function makeFakes(items) {
  const captured = {};
  let failed = null;
  const warnings = [];
  const fakeCore = {
    info: (m) => console.log('[info]', m),
    warning: (m) => {
      warnings.push(m);
      console.log('[warn]', m);
    },
    setFailed: (m) => {
      failed = m;
      console.log('[failed]', m);
    },
    setOutput: (k, v) => {
      captured[k] = v;
    },
  };
  const fakeGithub = {
    graphql: async () => ({
      organization: {
        projectV2: {
          items: {
            pageInfo: { hasNextPage: false, endCursor: null },
            nodes: items,
          },
        },
      },
    }),
  };
  return { fakeCore, fakeGithub, captured, warnings, get failed() { return failed; } };
}

async function testMainScenario() {
  console.log('\n=== testMainScenario ===');
  process.env.PROJECT_OWNER = 'dotCMS';
  process.env.PROJECT_NUMBER = '7';
  process.env.STUCK_DAYS = '3';
  process.env.TRIAGE_CONFIG_PATH = '.claude/triage-config.json';

  const run = freshModule();
  const items = [
    mkItem({ id: 'a', updatedAt: daysAgo(5), status: 'QA', number: 100, title: 'Stuck in QA - Scout', labels: ['Team : Scout', 'priority:high'], assignees: ['nollymar'] }),
    mkItem({ id: 'b', updatedAt: daysAgo(10), status: 'Done', number: 101, title: 'Already passed QA', labels: ['Team : Falcon', 'QA : Passed'] }),
    mkItem({ id: 'c', updatedAt: daysAgo(7), status: 'In Progress', number: 102, title: 'Not in QA yet', labels: ['Team : Falcon'] }),
    mkItem({ id: 'd', updatedAt: daysAgo(1), status: 'QA', number: 103, title: 'Too fresh', labels: ['Team : Maintenance'] }),
    mkItem({ id: 'e', updatedAt: daysAgo(20), status: 'Done', number: 104, title: 'No team label', labels: [] }),
    mkItem({ id: 'f', updatedAt: daysAgo(4), status: 'Done', number: 105, title: 'Done but no QA - Modernization', labels: ['Team : Modernization'], assignees: ['hmoreras'] }),
    mkItem({ id: 'g', updatedAt: daysAgo(8), status: 'Done', number: 106, title: 'No QA needed', labels: ['Team : Scout', 'QA : Not Needed'] }),
    mkItem({ id: 'h', updatedAt: daysAgo(6), status: 'QA', number: 107, title: 'Older Scout QA', labels: ['Team : Scout'], assignees: ['rjvelazco'] }),
  ];
  const { fakeCore, fakeGithub, captured } = makeFakes(items);

  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  const byTeam = Object.fromEntries(groups.map((g) => [g.team, g]));

  assert.strictEqual(captured.has_results, 'true');
  assert.ok(byTeam['Team : Scout'], 'Scout group present');
  assert.strictEqual(byTeam['Team : Scout'].issues.length, 2);
  assert.strictEqual(byTeam['Team : Scout'].slack_channel, 'CQNF9PCFQ');
  assert.strictEqual(byTeam['Team : Scout'].stuck_days, 3);
  assert.strictEqual(byTeam['Team : Modernization'].issues.length, 1);
  assert.strictEqual(byTeam['Team : Modernization'].slack_channel, 'C09LGH4HNR5');
  assert.ok(!byTeam['Team : Falcon']);
  assert.ok(!byTeam['Team : Maintenance']);
  assert.strictEqual(byTeam['Team : Scout'].issues[0].number, 107);

  console.log('OK');
}

async function testEmptyProject() {
  console.log('\n=== testEmptyProject ===');
  const run = freshModule();
  const { fakeCore, fakeGithub, captured } = makeFakes([]);
  await run({ github: fakeGithub, core: fakeCore });

  assert.strictEqual(captured.has_results, 'false');
  assert.strictEqual(JSON.parse(captured.groups_json).length, 0);
  assert.match(captured.summary, /No QA\/Done issues/);
  console.log('OK');
}

async function testMultipleTeamLabels() {
  console.log('\n=== testMultipleTeamLabels ===');
  const run = freshModule();
  const items = [
    mkItem({ id: 'm', updatedAt: daysAgo(7), status: 'QA', number: 200, title: 'Cross-team issue', labels: ['Team : Scout', 'Team : Falcon'], assignees: [] }),
  ];
  const { fakeCore, fakeGithub, captured } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  const byTeam = Object.fromEntries(groups.map((g) => [g.team, g]));

  assert.strictEqual(groups.length, 2, 'reported to both teams');
  assert.strictEqual(byTeam['Team : Scout'].issues[0].number, 200);
  assert.strictEqual(byTeam['Team : Falcon'].issues[0].number, 200);
  console.log('OK');
}

async function testMissingStatusFailsLoudly() {
  console.log('\n=== testMissingStatusFailsLoudly ===');
  const run = freshModule();
  // 4 of 5 items have no Status -> ratio < 0.5 -> setFailed + throw
  const items = [
    mkItem({ id: '1', updatedAt: daysAgo(5), omitStatusField: true, number: 300, title: 'no status', labels: ['Team : Scout'] }),
    mkItem({ id: '2', updatedAt: daysAgo(5), omitStatusField: true, number: 301, title: 'no status', labels: ['Team : Scout'] }),
    mkItem({ id: '3', updatedAt: daysAgo(5), omitStatusField: true, number: 302, title: 'no status', labels: ['Team : Scout'] }),
    mkItem({ id: '4', updatedAt: daysAgo(5), omitStatusField: true, number: 303, title: 'no status', labels: ['Team : Scout'] }),
    mkItem({ id: '5', updatedAt: daysAgo(5), status: 'QA', number: 304, title: 'has status', labels: ['Team : Scout'] }),
  ];
  const fakes = makeFakes(items);

  await assert.rejects(
    () => run({ github: fakes.fakeGithub, core: fakes.fakeCore }),
    /falls outside the GraphQL fieldValues/,
  );
  assert.match(fakes.failed, /Only 1\/5/);
  console.log('OK');
}

async function testSomeMissingStatusWarnsButContinues() {
  console.log('\n=== testSomeMissingStatusWarnsButContinues ===');
  const run = freshModule();
  // 1 of 3 missing -> ratio 2/3 > 0.5 -> warn but continue
  const items = [
    mkItem({ id: '1', updatedAt: daysAgo(5), omitStatusField: true, number: 400, title: 'no status', labels: ['Team : Scout'] }),
    mkItem({ id: '2', updatedAt: daysAgo(5), status: 'QA', number: 401, title: 'qa', labels: ['Team : Scout'] }),
    mkItem({ id: '3', updatedAt: daysAgo(5), status: 'Done', number: 402, title: 'done', labels: ['Team : Modernization'] }),
  ];
  const { fakeCore, fakeGithub, captured, warnings } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  assert.strictEqual(JSON.parse(captured.groups_json).length, 2);
  assert.ok(warnings.some((w) => /had no readable/.test(w)));
  console.log('OK');
}

async function testNullProjectFailsLoudly() {
  console.log('\n=== testNullProjectFailsLoudly ===');
  const run = freshModule();
  let failed = null;
  const fakeCore = {
    info: () => {},
    warning: () => {},
    setFailed: (m) => {
      failed = m;
    },
    setOutput: () => {},
  };
  const fakeGithub = {
    graphql: async () => ({ organization: { projectV2: null } }),
  };

  await assert.rejects(
    () => run({ github: fakeGithub, core: fakeCore }),
    /Could not read projectV2/,
  );
  assert.match(failed, /Could not read projectV2/);
  console.log('OK');
}

async function testTeamWithoutSlackChannelIgnored() {
  console.log('\n=== testTeamWithoutSlackChannelIgnored ===');
  // Use a temp triage config without slack_channel for a team to confirm we
  // silently ignore issues that only carry that team's label.
  const fs = require('fs');
  const os = require('os');
  const tmp = path.join(os.tmpdir(), `triage-${Date.now()}.json`);
  fs.writeFileSync(
    tmp,
    JSON.stringify({
      teams: {
        'Team : Scout': { slack_channel: 'CQNF9PCFQ', members: [], areas: [] },
        'Team : Ghost': { members: [], areas: [] }, // no slack_channel
      },
    }),
  );
  process.env.TRIAGE_CONFIG_PATH = tmp;

  const run = freshModule();
  const items = [
    mkItem({ id: 'g', updatedAt: daysAgo(7), status: 'QA', number: 500, title: 'ghost-only', labels: ['Team : Ghost'] }),
    mkItem({ id: 's', updatedAt: daysAgo(7), status: 'QA', number: 501, title: 'scout', labels: ['Team : Scout'] }),
  ];
  const { fakeCore, fakeGithub, captured } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  assert.strictEqual(groups.length, 1);
  assert.strictEqual(groups[0].team, 'Team : Scout');

  fs.unlinkSync(tmp);
  process.env.TRIAGE_CONFIG_PATH = '.claude/triage-config.json';
  console.log('OK');
}

async function testInvalidStuckDaysFailsLoudly() {
  console.log('\n=== testInvalidStuckDaysFailsLoudly ===');
  // Note: '' falls back to the JS-level default '3' via `process.env.STUCK_DAYS || '3'`,
  // which mirrors the schedule trigger where inputs.stuck_days is unset.
  for (const bad of ['abc', '0', '-1']) {
    process.env.STUCK_DAYS = bad;
    process.env.TRIAGE_CONFIG_PATH = '.claude/triage-config.json';
    const run = freshModule();
    const { fakeCore, fakeGithub } = makeFakes([]);
    await assert.rejects(
      () => run({ github: fakeGithub, core: fakeCore }),
      /STUCK_DAYS must be a finite integer/,
      `expected throw for STUCK_DAYS="${bad}"`,
    );
  }
  process.env.STUCK_DAYS = '3';
  console.log('OK');
}

async function testNonIssueContentDoesNotTripRatio() {
  console.log('\n=== testNonIssueContentDoesNotTripRatio ===');
  process.env.STUCK_DAYS = '3';
  const run = freshModule();
  // 1 Issue with Status + 4 non-Issue items missing Status. Before the fix
  // this tripped the ratio guard (1/5 < 0.5); after the fix the ratio is
  // computed only over Issue-typed items (1/1 = 1.0).
  const issueItem = mkItem({ id: 'ok', updatedAt: daysAgo(7), status: 'QA', number: 800, title: 'real issue', labels: ['Team : Scout'] });
  const draftLike = (id, n) => ({
    id,
    updatedAt: daysAgo(7),
    fieldValues: { nodes: [] },
    content: { __typename: 'DraftIssue', title: `draft ${n}` },
  });
  const items = [issueItem, draftLike('d1', 1), draftLike('d2', 2), draftLike('d3', 3), draftLike('d4', 4)];
  const { fakeCore, fakeGithub, captured } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  assert.strictEqual(groups.length, 1, 'Issue passes; non-Issue items ignored');
  assert.strictEqual(groups[0].issues[0].number, 800);
  console.log('OK');
}

async function testClosedQaIssueIsSkipped() {
  console.log('\n=== testClosedQaIssueIsSkipped ===');
  process.env.STUCK_DAYS = '3';
  const run = freshModule();
  const items = [
    // SKIP: CLOSED but still parked in Status=QA (board-cleanup case)
    mkItem({ id: 'cqa', updatedAt: daysAgo(7), status: 'QA', state: 'CLOSED', number: 700, title: 'Closed but in QA column', labels: ['Team : Scout'] }),
    // KEEP: CLOSED + Done is the expected combo
    mkItem({ id: 'cd', updatedAt: daysAgo(7), status: 'Done', state: 'CLOSED', number: 701, title: 'Closed and Done', labels: ['Team : Scout'] }),
    // KEEP: OPEN + QA is the normal case
    mkItem({ id: 'oq', updatedAt: daysAgo(7), status: 'QA', state: 'OPEN', number: 702, title: 'Open QA', labels: ['Team : Scout'] }),
  ];
  const { fakeCore, fakeGithub, captured } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  assert.strictEqual(groups.length, 1);
  const numbers = groups[0].issues.map((i) => i.number).sort();
  assert.deepStrictEqual(numbers, [701, 702], 'closed-QA dropped, others kept');
  console.log('OK');
}

async function testLabelsOverflowSkipsIssue() {
  console.log('\n=== testLabelsOverflowSkipsIssue ===');
  process.env.STUCK_DAYS = '3';
  const run = freshModule();
  // Two stale, team-labeled issues. The first reports labelsHasNextPage=true,
  // so it must be skipped (we can't trust its skip-label gate). The second
  // must come through.
  const items = [
    mkItem({ id: 'overflow', updatedAt: daysAgo(7), status: 'QA', number: 600, title: 'Too many labels', labels: ['Team : Scout'], assignees: [], labelsHasNextPage: true }),
    mkItem({ id: 'ok', updatedAt: daysAgo(7), status: 'QA', number: 601, title: 'Normal', labels: ['Team : Scout'], assignees: [] }),
  ];
  const { fakeCore, fakeGithub, captured, warnings } = makeFakes(items);
  await run({ github: fakeGithub, core: fakeCore });

  const groups = JSON.parse(captured.groups_json);
  assert.strictEqual(groups.length, 1);
  assert.strictEqual(groups[0].issues.length, 1);
  assert.strictEqual(groups[0].issues[0].number, 601);
  assert.ok(
    warnings.some((w) => /Issue #600 has more labels/.test(w)),
    'expected warning about issue #600',
  );
  console.log('OK');
}

(async () => {
  await testMainScenario();
  await testEmptyProject();
  await testMultipleTeamLabels();
  await testMissingStatusFailsLoudly();
  await testSomeMissingStatusWarnsButContinues();
  await testNullProjectFailsLoudly();
  await testTeamWithoutSlackChannelIgnored();
  await testInvalidStuckDaysFailsLoudly();
  await testNonIssueContentDoesNotTripRatio();
  await testClosedQaIssueIsSkipped();
  await testLabelsOverflowSkipsIssue();
  console.log('\nAll tests passed.');
})().catch((e) => {
  console.error('\nTest failure:', e);
  process.exit(1);
});