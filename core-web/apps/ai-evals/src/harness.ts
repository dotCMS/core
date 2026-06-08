import { generateText, stepCountIs } from 'ai';

import type {
    AssertionResult,
    CleanupStep,
    CostModelAssumptions,
    CustomAssertion,
    HttpGetAssertion,
    RunResult,
    RunTaskOptions,
    TaskResult
} from './types';
import { makeTools } from './tools';

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

// ── HTTP GET assertion ────────────────────────────────────────────────────────

async function runHttpGetAssertion(
    assertion: HttpGetAssertion,
    dotcmsUrl: string,
    authToken: string
): Promise<AssertionResult> {
    const url = new URL(assertion.path, dotcmsUrl).toString();
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
            description: `GET ${assertion.path} → expect ${assertion.expect}`,
            details: { path: assertion.path, status, response_body: responseBody, expected }
        };
    } catch (err) {
        return {
            passed: false,
            type: 'http_get',
            description: `GET ${assertion.path} → expect ${assertion.expect}`,
            details: {
                path: assertion.path,
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

async function runCustomAssertion(
    assertion: CustomAssertion,
    run: RunResult,
    dotcmsUrl: string,
    authToken: string
): Promise<AssertionResult> {
    const ctx = { trace: run, url: dotcmsUrl, token: authToken };

    try {
        const fn = new Function(
            'trace',
            'url',
            'token',
            'fetch',
            `return (async () => { ${assertion.script} })()`
        );

        const timeout = new Promise<never>((_, reject) =>
            setTimeout(() => reject(new Error('Assertion timed out after 10s')), CUSTOM_TIMEOUT_MS)
        );

        const result = await Promise.race([
            fn(ctx.trace, ctx.url, ctx.token, globalThis.fetch) as Promise<unknown>,
            timeout
        ]);

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
    steps: CleanupStep[],
    dotcmsUrl: string,
    authToken: string
): Promise<void> {
    for (const step of steps) {
        const url = new URL(step.path, dotcmsUrl).toString();
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
                console.warn(
                    `  [cleanup] ${step.method} ${step.path} → ${response.status} (ignored)`
                );
            }
        } catch (err) {
            console.warn(
                `  [cleanup] ${step.method} ${step.path} → error: ${err instanceof Error ? err.message : String(err)} (ignored)`
            );
        }
    }
}

// ── Single run ────────────────────────────────────────────────────────────────

async function executeRun(index: number, options: RunTaskOptions): Promise<RunResult> {
    const { task, model, dotcmsUrl, authToken } = options;
    const start = Date.now();

    let partialRun: Omit<RunResult, 'assertions' | 'passed'> = {
        run_index: index,
        steps: [],
        text: '',
        finish_reason: 'error',
        input_tokens: 0,
        output_tokens: 0,
        duration_ms: 0
    };

    try {
        const tools = makeTools(dotcmsUrl, authToken);
        // nosemgrep: detect-vercelai -- internal LLM eval harness (ai-evals), not shipped runtime code; Vercel AI SDK usage is intentional
        const { text, steps, finishReason, usage } = await generateText({
            model,
            prompt: task.prompt,
            tools,
            stopWhen: stepCountIs(10)
        });

        partialRun = {
            run_index: index,
            steps,
            text,
            finish_reason: finishReason,
            input_tokens: usage.inputTokens ?? 0,
            output_tokens: usage.outputTokens ?? 0,
            duration_ms: Date.now() - start
        };
    } catch (err) {
        const run: RunResult = {
            ...partialRun,
            duration_ms: Date.now() - start,
            assertions: [],
            passed: false,
            error: err instanceof Error ? err.message : String(err)
        };
        await runCleanup(task.cleanup ?? [], dotcmsUrl, authToken);
        return run;
    }

    // Run assertions
    const assertions: AssertionResult[] = [];
    for (const assertion of task.assertions) {
        if (assertion.type === 'http_get') {
            assertions.push(await runHttpGetAssertion(assertion, dotcmsUrl, authToken));
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
    await runCleanup(task.cleanup ?? [], dotcmsUrl, authToken);

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
