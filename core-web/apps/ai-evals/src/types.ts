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
}

export type TaskAssertion = HttpGetAssertion | CustomAssertion;

export interface CleanupStep {
    method: string;
    path: string;
    body?: unknown;
}

export interface TaskDefinition {
    id: string;
    category: string;
    description: string;
    difficulty: 'easy' | 'medium' | 'hard';
    runs: number;
    prompt: string;
    assertions: TaskAssertion[];
    cleanup?: CleanupStep[];
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
