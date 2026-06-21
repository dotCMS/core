import { generateText, stepCountIs, type LanguageModel } from 'ai';

import { RESEARCH_SYSTEM } from './research-prompt';
import { createResearchTools, type ResearchToolsDeps } from './research-tools';

import { defaultModel } from '../model';

import type { ScanFinding } from '../../dotcms/dotcms-client';

/**
 * PASS 2 — agentic research loop. For violations the deterministic pass couldn't
 * fix, give the model the typed research tools (locate/read/grep/save/rescan) and
 * let it work the way the MCP-server agent did: discover the source, edit it,
 * confirm with a re-scan. Safe by construction — no publish/delete tool exists,
 * and every call rides the path-allowlisted sandbox. The system prompt lives in
 * research-prompt.ts.
 */

export interface ResearchResult {
    /** Files the agent saved (working) during the loop. */
    editedPaths: string[];
    /** The model's final summary text. */
    summary: string;
}

export interface RunResearchInput {
    violations: ScanFinding[];
    deps: ResearchToolsDeps;
    model?: LanguageModel;
    maxSteps?: number;
}

/** Format the unresolved violations compactly for the prompt. */
function violationsBlock(violations: ScanFinding[]): string {
    return violations
        .map((v, i) => {
            const d = v.data;
            const colors = d?.fgColor ? ` [fg=${d.fgColor} bg=${d.bgColor} ratio=${d.contrastRatio} need ${d.expectedContrastRatio}]` : '';
            return `${i + 1}. ${v.code}${colors}\n   selector: ${v.selector}\n   html: ${v.context.slice(0, 200)}`;
        })
        .join('\n\n');
}

/** Default tool-use step budget for the research pass. */
export const DEFAULT_RESEARCH_MAX_STEPS = 40;

export async function runResearch(input: RunResearchInput): Promise<ResearchResult> {
    const { violations, deps } = input;
    const model = input.model ?? defaultModel();
    const maxSteps = input.maxSteps ?? DEFAULT_RESEARCH_MAX_STEPS;
    const tools = createResearchTools(deps);

    const prompt = `These ${violations.length} accessibility violations remain unfixed. Research the page source and fix the ones you can:\n\n${violationsBlock(violations)}`;

    const result = await generateText({
        model,
        tools,
        stopWhen: stepCountIs(maxSteps),
        system: RESEARCH_SYSTEM,
        prompt
    });

    return {
        editedPaths: [...deps.editedPaths],
        summary: result.text
    };
}
