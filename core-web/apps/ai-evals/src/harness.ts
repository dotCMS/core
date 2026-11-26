import { generateText, stepCountIs } from 'ai';

import { makeTools } from './tools';

import type {
    AssertionResult,
    CleanupScript,
    CleanupStep,
    CostModelAssumptions,
    CustomAssertion,
    HttpGetAssertion,
    RunResult,
    RunTaskOptions,
    TaskResult,
    TaskVars
} from './types';

// ── Pricing (claude-sonnet-4-5, $/MTok) ──────────────────────────────────────

const COST_ASSUMPTIONS: CostModelAssumptions = {
    model_id: 'claude-sonnet-4-5-20250929',
    input_per_mtok: 3.0,
    output_per_mtok: 15.0
};

function estimateCost(inputTokens: number, outputTokens: number): number {
    return (
        (inputTokens / 1_000_000) * COST_ASSUMPTIONS.input_per_mtok +
        (outputTokens / 1_000_000) * COST_ASSUMPTIONS.output_per_mtok
    );
}

// ── Task scripts and {{vars}} ─────────────────────────────────────────────────

/** Replaces `{{name}}` with the run's value. An unknown name is left as written. */
export function interpolate(text: string, vars: TaskVars): string {
    return text.replace(/\{\{\s*([A-Za-z0-9_]+)\s*\}\}/g, (match, name: string) =>
        name in vars ? vars[name] : match
    );
}

interface ScriptGlobals {
    trace?: RunResult;
    url: string;
    token: string;
    vars: TaskVars;
    run: number;
}

/** Runs a task's inline JavaScript (setup, custom assertion, cleanup script) with a deadline. */
async function runScript(
    script: string,
    globals: ScriptGlobals,
    timeoutMs: number
): Promise<unknown> {
    const fn = new Function(
        'trace',
        'url',
        'token',
        'fetch',
        'vars',
        'run',
        `return (async () => { ${script} })()`
    );

    let timer: ReturnType<typeof setTimeout> | undefined;
    const timeout = new Promise<never>((_, reject) => {
        timer = setTimeout(
            () => reject(new Error(`Script timed out after ${timeoutMs / 1000}s`)),
            timeoutMs
        );
    });

    try {
        return await Promise.race([
            fn(
                globals.trace,
                globals.url,
                globals.token,
                globalThis.fetch,
                globals.vars,
                globals.run
            ) as Promise<unknown>,
            timeout
        ]);
    } finally {
        clearTimeout(timer);
    }
}

const SETUP_TIMEOUT_MS = 60_000;
const CLEANUP_SCRIPT_TIMEOUT_MS = 60_000;

/** Runs the task's `setup` and returns its values as strings. Throws when setup fails. */
async function runSetup(
    script: string,
    run: number,
    dotcmsUrl: string,
    authToken: string
): Promise<TaskVars> {
    // Values the script assigns to `vars` as it goes are kept if it throws later, so cleanup can
    // still remove what it created before failing.
    const assigned: Record<string, unknown> = {};
    const toVars = (values: Record<string, unknown>): TaskVars =>
        Object.fromEntries(Object.entries(values).map(([k, v]) => [k, String(v)]));
    let result: unknown;
    try {
        result = await runScript(
            script,
            { url: dotcmsUrl, token: authToken, vars: assigned as TaskVars, run },
            SETUP_TIMEOUT_MS
        );
    } catch (err) {
        throw new SetupError(err, toVars(assigned));
    }
    if (result === undefined || result === null) return toVars(assigned);
    if (typeof result !== 'object' || Array.isArray(result)) {
        throw new SetupError(
            new Error('setup must return an object of values (or nothing)'),
            toVars(assigned)
        );
    }
    return toVars({ ...assigned, ...(result as Record<string, unknown>) });
}

/** A failed setup, with the values it had assigned to `vars` before failing. */
class SetupError extends Error {
    constructor(
        readonly original: unknown,
        readonly vars: TaskVars
    ) {
        super(original instanceof Error ? original.message : String(original));
    }
}

// ── HTTP GET assertion ────────────────────────────────────────────────────────

async function runHttpGetAssertion(
    assertion: HttpGetAssertion,
    vars: TaskVars,
    dotcmsUrl: string,
    authToken: string
): Promise<AssertionResult> {
    const path = interpolate(assertion.path, vars);
    const url = new URL(path, dotcmsUrl).toString();
    const expected = assertion.expect === 'ok' ? 200 : assertion.expect;
    let status: number | undefined;
    let responseBody: unknown;

    try {
        const response = await fetch(url, {
            headers: { Authorization: `Bearer ${authToken}`, Accept: 'application/json' }
        });
        status = response.status;
        try {
            responseBody = await response.json();
        } catch {
            responseBody = await response.text().catch(() => null);
        }

        const passed = assertion.expect === 'ok' ? response.ok : status === expected;

        return {
            passed,
            type: 'http_get',
            description: `GET ${path} → expect ${assertion.expect}`,
            details: { path, status, response_body: responseBody, expected }
        };
    } catch (err) {
        return {
            passed: false,
            type: 'http_get',
            description: `GET ${path} → expect ${assertion.expect}`,
            details: {
                path,
                status,
                response_body: responseBody,
                expected,
                error: err instanceof Error ? err.message : String(err)
            }
        };
    }
}

// ── Custom assertion ──────────────────────────────────────────────────────────

const CUSTOM_TIMEOUT_MS = 10_000;
const MAX_CUSTOM_TIMEOUT_MS = 120_000;

const DEFAULT_MAX_STEPS = 10;

async function runCustomAssertion(
    assertion: CustomAssertion,
    run: RunResult,
    dotcmsUrl: string,
    authToken: string
): Promise<AssertionResult> {
    try {
        const result = await runScript(
            assertion.script,
            { trace: run, url: dotcmsUrl, token: authToken, vars: run.vars, run: run.run_index },
            Math.min(assertion.timeout_ms ?? CUSTOM_TIMEOUT_MS, MAX_CUSTOM_TIMEOUT_MS)
        );

        return {
            passed: !!result,
            type: 'custom',
            description: assertion.name,
            details: { actual: result }
        };
    } catch (err) {
        return {
            passed: false,
            type: 'custom',
            description: assertion.name,
            details: { error: err instanceof Error ? err.message : String(err) }
        };
    }
}

// ── Cleanup ───────────────────────────────────────────────────────────────────

async function runCleanup(
    steps: (CleanupStep | CleanupScript)[],
    vars: TaskVars,
    run: number,
    dotcmsUrl: string,
    authToken: string
): Promise<void> {
    for (const step of steps) {
        if ('script' in step) {
            try {
                await runScript(
                    step.script,
                    { url: dotcmsUrl, token: authToken, vars, run },
                    CLEANUP_SCRIPT_TIMEOUT_MS
                );
            } catch (err) {
                console.warn(
                    `  [cleanup] script → error: ${err instanceof Error ? err.message : String(err)} (ignored)`
                );
            }
            continue;
        }

        const path = interpolate(step.path, vars);
        const url = new URL(path, dotcmsUrl).toString();
        try {
            const response = await fetch(url, {
                method: step.method.toUpperCase(),
                headers: {
                    Authorization: `Bearer ${authToken}`,
                    Accept: 'application/json',
                    ...(step.body ? { 'Content-Type': 'application/json' } : {})
                },
                ...(step.body ? { body: JSON.stringify(step.body) } : {})
            });
            if (!response.ok) {
                console.warn(`  [cleanup] ${step.method} ${path} → ${response.status} (ignored)`);
            }
        } catch (err) {
            console.warn(
                `  [cleanup] ${step.method} ${path} → error: ${err instanceof Error ? err.message : String(err)} (ignored)`
            );
        }
    }
}

// ── Single run ────────────────────────────────────────────────────────────────

async function executeRun(index: number, options: RunTaskOptions): Promise<RunResult> {
    const { task, model, dotcmsUrl, authToken } = options;
    const start = Date.now();
    const cleanupSteps = task.cleanup ?? [];

    let vars: TaskVars = {};
    let partialRun: Omit<RunResult, 'assertions' | 'passed'> = {
        run_index: index,
        steps: [],
        text: '',
        finish_reason: 'error',
        input_tokens: 0,
        output_tokens: 0,
        duration_ms: 0,
        vars
    };

    const failed = async (stage: string, err: unknown): Promise<RunResult> => {
        const run: RunResult = {
            ...partialRun,
            duration_ms: Date.now() - start,
            assertions: [],
            passed: false,
            error: `${stage}: ${err instanceof Error ? err.message : String(err)}`
        };
        await runCleanup(cleanupSteps, vars, index, dotcmsUrl, authToken);
        return run;
    };

    if (task.setup) {
        try {
            vars = await runSetup(task.setup, index, dotcmsUrl, authToken);
            partialRun = { ...partialRun, vars };
        } catch (err) {
            if (err instanceof SetupError) {
                vars = err.vars;
                partialRun = { ...partialRun, vars };
            }
            return failed('setup', err);
        }
    }

    try {
        const tools = makeTools(dotcmsUrl, authToken);
        // nosemgrep: detect-vercelai -- internal LLM eval harness (ai-evals), not shipped runtime code; Vercel AI SDK usage is intentional
        const { text, steps, finishReason, usage } = await generateText({
            model,
            prompt: interpolate(task.prompt, vars),
            tools,
            stopWhen: stepCountIs(task.max_steps ?? DEFAULT_MAX_STEPS)
        });

        partialRun = {
            run_index: index,
            steps,
            text,
            finish_reason: finishReason,
            input_tokens: usage.inputTokens ?? 0,
            output_tokens: usage.outputTokens ?? 0,
            duration_ms: Date.now() - start,
            vars
        };
    } catch (err) {
        return failed('model', err);
    }

    // Run assertions
    const assertions: AssertionResult[] = [];
    for (const assertion of task.assertions) {
        if (assertion.type === 'http_get') {
            assertions.push(await runHttpGetAssertion(assertion, vars, dotcmsUrl, authToken));
        } else if (assertion.type === 'custom') {
            assertions.push(
                await runCustomAssertion(
                    assertion,
                    { ...partialRun, assertions: [], passed: false },
                    dotcmsUrl,
                    authToken
                )
            );
        }
    }

    // Best-effort cleanup
    await runCleanup(cleanupSteps, vars, index, dotcmsUrl, authToken);

    const passed = assertions.every((a) => a.passed);
    return { ...partialRun, assertions, passed };
}

// ── Task runner ───────────────────────────────────────────────────────────────

export async function runTask(options: RunTaskOptions, verbose = false): Promise<TaskResult> {
    const { task, modelId } = options;
    const runs: RunResult[] = [];

    for (let i = 0; i < task.runs; i++) {
        console.log(`  run ${i + 1}/${task.runs}...`);
        const run = await executeRun(i + 1, options);
        runs.push(run);

        for (const [j, step] of run.steps.entries()) {
            const calls =
                step.toolCalls?.map((c: { toolName: string }) => c.toolName).join(', ') ?? '—';
            console.log(`    step ${j + 1}: [${calls}]`);

            if (verbose) {
                for (const result of step.toolResults ?? []) {
                    const input = JSON.stringify(result.input).slice(0, 200);
                    const output = String(result.output).slice(0, 500);
                    console.log(`      [${result.toolName}] input : ${input}`);
                    console.log(`      [${result.toolName}] output: ${output}`);
                }
            }
        }

        for (const a of run.assertions) {
            console.log(`    [${a.passed ? 'PASS' : 'FAIL'}] ${a.description}`);
            if (!a.passed && a.details.error) {
                console.log(`           error: ${a.details.error}`);
            }
        }

        if (run.error) console.log(`    [ERROR] ${run.error}`);
    }

    const passed = runs.filter((r) => r.passed).length;
    const totalInput = runs.reduce((s, r) => s + r.input_tokens, 0);
    const totalOutput = runs.reduce((s, r) => s + r.output_tokens, 0);
    const totalDuration = runs.reduce((s, r) => s + r.duration_ms, 0);

    return {
        task_id: task.id,
        model_id: modelId,
        runs,
        passed,
        total: runs.length,
        pass_rate: passed / runs.length,
        mean_duration_ms: totalDuration / runs.length,
        mean_input_tokens: totalInput / runs.length,
        mean_output_tokens: totalOutput / runs.length,
        estimated_cost_usd: estimateCost(totalInput, totalOutput),
        cost_model_assumptions: COST_ASSUMPTIONS
    };
}
