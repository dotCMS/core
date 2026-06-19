import { anthropic } from '@ai-sdk/anthropic';
import { generateText, Output, type LanguageModel } from 'ai';
import { z } from 'zod';

import type { ScanFinding, SourceRef } from './dotcms-client';

/**
 * The two LLM judgment calls in the (B) deterministic-skeleton loop. Everything
 * else (sequencing, guards, caps, report assembly) is plain code in runFix.ts;
 * the model is scoped to exactly the two things that need it:
 *   1. triage + attribution — classify a violation and pick the source file
 *   2. minimal-diff generation — rewrite that file to clear the violation
 *
 * The model is injected (default Anthropic Sonnet) so it's mockable in tests and
 * the provider stays swappable per plan §3 (Azure/Bedrock/dotAI later).
 *
 * Cost note: triage classification + minimal diffs do NOT need a frontier model.
 * Opus cost ~$12 for a single page in testing; Sonnet is the default. Override
 * per-deploy with A11Y_AGENT_MODEL (any Anthropic model id) without code changes.
 */

export const DEFAULT_MODEL = process.env.A11Y_AGENT_MODEL ?? 'claude-sonnet-4-6';

export function defaultModel(): LanguageModel {
    return anthropic(DEFAULT_MODEL);
}

// ── 1. Triage + attribution ─────────────────────────────────────────────────

export const TriageDecisionSchema = z.object({
    // Whether this violation is fixable in source within v1 scope.
    fixability: z.enum(['vtl', 'css', 'report-only']),
    // The host-qualified path of the file to edit (must be one of the candidates).
    // Null when report-only or attribution is not provable.
    targetPath: z.string().nullable(),
    // Attribution evidence gate (plan §5/§9): the model must confirm the chosen
    // file actually contains the offending markup/selector before any edit.
    // If false, runFix reports instead of guess-editing.
    evidenceFound: z.boolean(),
    // Short human-facing rationale for the report.
    reason: z.string()
});

export type TriageDecision = z.infer<typeof TriageDecisionSchema>;

export interface TriageInput {
    finding: ScanFinding;
    /** Candidate source refs from /_render-sources (theme + container VTLs). */
    candidates: SourceRef[];
    /** The already-read contents of each candidate, keyed by path. */
    fileContents: Record<string, string>;
    /** When true, CSS violations are forced to report-only (per-run opt-out, §3). */
    skipCss: boolean;
}

const TRIAGE_SYSTEM = `You triage a single web-accessibility violation for a dotCMS page and attribute it to the exact source file.

Rules:
- Choose fixability: "vtl" (markup issue in a template/container .vtl), "css" (contrast or a stylesheet rule), or "report-only" (content-field text, third-party, JS-injected, or attribution not provable).
- targetPath MUST be one of the provided candidate paths, or null for report-only.
- evidenceFound is the gate: set it true ONLY if the chosen file's contents clearly contain the offending markup or selector. If you cannot point to it in the file, set evidenceFound=false (we will report, not guess-edit).
- Never fabricate semantic content. Generic alt/aria text that merely passes a scanner but is meaningless is NOT a fix — prefer report-only.
- Be conservative: when unsure, report-only.`;

export async function triageViolation(
    input: TriageInput,
    model: LanguageModel = defaultModel()
): Promise<TriageDecision> {
    const candidatesBlock = input.candidates
        .map((c) => {
            const body = input.fileContents[c.path] ?? '(not read)';
            return `### ${c.path} (identifier ${c.identifier})\n\`\`\`\n${body}\n\`\`\``;
        })
        .join('\n\n');

    // The candidate-files block is identical for every violation in a run, so we
    // mark it ephemeral-cacheable: Anthropic prompt caching makes the repeated
    // (and large — full CSS/VTL) prefix ~10x cheaper after the first call. The
    // per-violation details go in a separate, uncached user message.
    const filesContext = `Candidate source files for the page being fixed:\n\n${candidatesBlock}`;

    const violationPrompt = `Violation (axe rule "${input.finding.code}", impact ${
        input.finding.runnerExtras?.impact ?? 'unknown'
    }):
- message: ${input.finding.message}
- selector: ${input.finding.selector}
- offending HTML: ${input.finding.context}

skipCss is ${input.skipCss ? 'TRUE — treat CSS/contrast issues as report-only' : 'false'}.

Decide fixability, the target file (a candidate path or null), whether the offending markup is actually present in that file (evidenceFound), and a short reason.`;

    const { output } = await generateText({
        model,
        output: Output.object({ schema: TriageDecisionSchema }),
        system: TRIAGE_SYSTEM,
        messages: [
            {
                role: 'user',
                content: [
                    {
                        type: 'text',
                        text: filesContext,
                        providerOptions: { anthropic: { cacheControl: { type: 'ephemeral' } } }
                    }
                ]
            },
            { role: 'user', content: violationPrompt }
        ]
    });

    // Deterministic guard: honor skipCss even if the model ignored it.
    if (input.skipCss && output.fixability === 'css') {
        return {
            fixability: 'report-only',
            targetPath: null,
            evidenceFound: false,
            reason: 'CSS fix skipped per run option (skipCss); contrast reported instead.'
        };
    }
    return output;
}

// ── 2. Minimal-diff generation ───────────────────────────────────────────────

export const FixOutputSchema = z.object({
    // The full edited file content (we PUT the whole file to /save).
    newContent: z.string(),
    // A short unified-diff-ish summary of what changed, for the report.
    diff: z.string(),
    // The model's confidence it is a real, minimal, non-breaking fix.
    applied: z.boolean(),
    reason: z.string()
});

export type FixOutput = z.infer<typeof FixOutputSchema>;

export interface FixInput {
    finding: ScanFinding;
    targetPath: string;
    originalContent: string;
    fixability: 'vtl' | 'css';
}

const FIX_SYSTEM = `You produce a minimal, correct source edit that clears one accessibility violation in a dotCMS .vtl or .css file.

Rules:
- Return the FULL edited file content in newContent (it is saved wholesale).
- Make the SMALLEST change that fixes the violation. Do not reformat, reorder, or touch unrelated code.
- Preserve Velocity syntax ($, #, #if/#foreach, #set) exactly — a broken template is worse than the violation.
- For contrast: nudge the EXISTING color to clear WCAG AA (4.5:1 normal, 3:1 large). Do not invent a new brand color. If the threshold can't be met without a design decision, set applied=false and explain.
- Never fabricate semantic content (e.g. meaningless alt text). If a correct fix needs human-authored content, set applied=false.
- diff: a short summary of the change (a few lines), not the whole file.`;

export async function generateFix(
    input: FixInput,
    model: LanguageModel = defaultModel()
): Promise<FixOutput> {
    const prompt = `File: ${input.targetPath} (${input.fixability})

Violation (axe rule "${input.finding.code}"):
- message: ${input.finding.message}
- selector: ${input.finding.selector}
- offending HTML: ${input.finding.context}

Current file content:
\`\`\`
${input.originalContent}
\`\`\`

Produce the minimal edited file that clears this violation.`;

    const { output } = await generateText({
        model,
        output: Output.object({ schema: FixOutputSchema }),
        system: FIX_SYSTEM,
        prompt
    });
    return output;
}
