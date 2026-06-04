import 'dotenv/config';

import path from 'node:path';

import { anthropic } from '@ai-sdk/anthropic';

import { runTask } from './harness';
import { loadTask, loadTasksFromDir } from './loader';

// ── Env validation ────────────────────────────────────────────────────────────

function requireEnv(name: string): string {
    const v = process.env[name];
    if (!v) {
        console.error(`Missing required env var: ${name}`);
        process.exit(1);
    }
    return v;
}

const DOTCMS_URL = requireEnv('DOTCMS_URL');
const AUTH_TOKEN = requireEnv('AUTH_TOKEN');
requireEnv('ANTHROPIC_API_KEY');

// ── Runner ────────────────────────────────────────────────────────────────────

const MODEL_ID = 'claude-sonnet-4-5-20250929';
const model = anthropic(MODEL_ID);

async function main() {
    // CLI: optionally pass a task file or directory, otherwise run all tasks/
    const args = process.argv.slice(2);
    const verbose = args.includes('--verbose') || !!process.env['VERBOSE'];
    const positional = args.filter((a) => !a.startsWith('--'));
    const tasksDir = path.resolve(__dirname, '../tasks');

    const tasks = positional[0] ? [loadTask(positional[0])] : loadTasksFromDir(tasksDir);

    if (tasks.length === 0) {
        console.error(`No tasks found. Add .yaml files to apps/ai-evals/tasks/`);
        process.exit(1);
    }

    console.log(`\n=== dotCMS ai-evals ===`);
    console.log(`Model  : ${MODEL_ID}`);
    console.log(`Target : ${DOTCMS_URL}`);
    console.log(`Tasks  : ${tasks.length}\n`);

    const results = [];
    for (const task of tasks) {
        console.log(`\n── ${task.id} (${task.runs} runs) ──`);
        console.log(`   ${task.description}`);

        const result = await runTask(
            { task, model, modelId: MODEL_ID, dotcmsUrl: DOTCMS_URL, authToken: AUTH_TOKEN },
            verbose
        );
        results.push(result);

        const pct = (result.pass_rate * 100).toFixed(0);
        console.log(
            `\n   Result: ${result.passed}/${result.total} (${pct}%) | $${result.estimated_cost_usd.toFixed(4)}`
        );
    }

    // Summary
    console.log('\n\n=== Summary ===');
    let anyFailed = false;
    for (const r of results) {
        const pct = (r.pass_rate * 100).toFixed(0);
        const status = r.pass_rate === 1 ? 'PASS' : 'FAIL';
        console.log(`[${status}] ${r.task_id}: ${r.passed}/${r.total} (${pct}%)`);
        if (r.pass_rate < 1) anyFailed = true;
    }

    if (anyFailed) process.exit(1);
}

main().catch((e) => {
    console.error('Fatal:', e);
    process.exit(1);
});
