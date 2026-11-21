import type { LanguageModel } from 'ai';

// ── Assertion results ─────────────────────────────────────────────────────────

export interface AssertionDetails {
    path?: string;
    status?: number;
    response_body?: unknown;
    expected?: unknown;
    actual?: unknown;
    error?: string;
}

export interface AssertionResult {
    passed: boolean;
    type: string;
    description: string;
    details: AssertionDetails;
}

// ── Per-run result ────────────────────────────────────────────────────────────

export interface RunResult {
    run_index: number;
    passed: boolean;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    steps: any[];
    text: string;
    finish_reason: string;
    input_tokens: number;
    output_tokens: number;
    duration_ms: number;
    assertions: AssertionResult[];
    /** Values the task's `setup` script returned for this run. */
    vars: TaskVars;
    error?: string;
}

// ── Task-level aggregation ────────────────────────────────────────────────────

export interface CostModelAssumptions {
    model_id: string;
    input_per_mtok: number;
    output_per_mtok: number;
}

export interface TaskResult {
    task_id: string;
    model_id: string;
    runs: RunResult[];
    passed: number;
    total: number;
    pass_rate: number;
    mean_duration_ms: number;
    mean_input_tokens: number;
    mean_output_tokens: number;
    estimated_cost_usd: number;
    cost_model_assumptions: CostModelAssumptions;
}

// ── YAML task schema ──────────────────────────────────────────────────────────

export interface HttpGetAssertion {
    type: 'http_get';
    path: string;
    expect: 'ok' | number;
}

export interface CustomAssertion {
    type: 'custom';
    name: string;
    script: string;
    /** Deadline for the script, for checks that poll. Default 10s, at most 120s. */
    timeout_ms?: number;
}

export type TaskAssertion = HttpGetAssertion | CustomAssertion;

export interface CleanupStep {
    method: string;
    path: string;
    body?: unknown;
}

/** A cleanup step written as JavaScript, for state a fixed path cannot name. */
export interface CleanupScript {
    script: string;
}

/**
 * Values a task's `setup` script returns, available as `{{name}}` in the prompt, in assertion
 * and cleanup paths, and as `vars` in every script.
 */
export type TaskVars = Record<string, string>;

export interface TaskDefinition {
    id: string;
    category: string;
    description: string;
    difficulty: 'easy' | 'medium' | 'hard';
    runs: number;
    prompt: string;
    /**
     * JavaScript run before each run (globals: `url`, `token`, `fetch`, `run`). Creates the
     * run's fixtures and returns the values the prompt and assertions refer to. A failure
     * skips the run's model step and records the run as failed.
     */
    setup?: string;
    /** Most model steps per run. Default 10. */
    max_steps?: number;
    assertions: TaskAssertion[];
    cleanup?: (CleanupStep | CleanupScript)[];
    tags?: string[];
    requires_fixture?: string;
}

// ── Harness options ───────────────────────────────────────────────────────────

export interface RunTaskOptions {
    task: TaskDefinition;
    model: LanguageModel;
    modelId: string;
    dotcmsUrl: string;
    authToken: string;
}
