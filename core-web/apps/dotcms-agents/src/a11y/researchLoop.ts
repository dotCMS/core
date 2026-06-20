import { generateText, stepCountIs, type LanguageModel } from 'ai';

import { createResearchTools, type ResearchToolsDeps } from './tools';
import { defaultModel } from './triage';

import type { ScanFinding } from './dotcms-client';

/**
 * PASS 2 — agentic research loop. For violations the deterministic pass couldn't
 * fix, give the model the typed research tools (locate/read/grep/save/rescan) and
 * let it work the way the MCP-server agent did: discover the source, edit it,
 * confirm with a re-scan. Safe by construction — no publish/delete tool exists,
 * and every call rides the path-allowlisted sandbox.
 */

const RESEARCH_SYSTEM = `You are an accessibility fixer working on a dotCMS page. You are given a list of unresolved a11y violations and a set of tools to inspect and edit the page's source files.

SKIP IMMEDIATELY — do NOT research or try to fix these (they are not in any editable source; spending tool calls on them is wasted):
- Editor chrome buttons: elements with data-dot-object="edit-content" / "edit-container" / "add". These are the dotCMS editor's inline UI, in no template. (But data-dot-object="contentlet" / "container" are NOT chrome — see below, they tell you the source.)
- "incomplete"/needs-review contrast where text sits over a background IMAGE or gradient (axe could not measure it). Never "fix" these by changing colors — they need a human design decision.
Note these as skipped in your summary and move on.

ATTRIBUTION HINT — dotCMS tags rendered elements with their source. If a violation (or its ancestor) carries data-dot-object="contentlet" with data-dot-type="X" (e.g. "Banner", "Activity"), the source is that content type's container VTL — match data-dot-type to a container content-type VTL from locateSources (e.g. Banner → banner.vtl, Activity → activity.vtl) and edit there. This is more reliable than grepping.

Loop for each REMAINING violation:
1. Use locateSources to see the page's source files.
2. Use grepAssets / readAsset to find the EXACT source (VTL template, container VTL, or CSS/SCSS) that produces the offending markup or color. The scanner gives you the element's HTML and selector — match it to a file. Filenames are not reliable; verify by reading.
3. Make the SMALLEST edit that fixes the violation, then saveWorking the FULL edited file.
4. After editing, call rescan to confirm the violation count dropped. If it went UP, your edit made things worse — revert it (saveWorking the original content).

Rules:
- NEVER invent a publish/delete operation — you can only read and save WORKING versions.
- Preserve Velocity ($, #, #if/#set/#foreach) and CSS syntax exactly. A broken template is worse than the violation.
- For contrast: nudge the EXISTING color to clear WCAG AA (4.5:1 normal, 3:1 large). Do not invent a new brand color.
- Do NOT fabricate semantic content. Generic alt/aria text that merely passes the scanner but is meaningless is NOT a fix — skip it.
- Container colors are often set in the container VTL via a Velocity variable (e.g. #set($btnBg = "#1D4ED8")) inside a <style> block — grep the VTLs for the failing color.
- Work through the violations you were given; skip any you genuinely cannot attribute to a source file. Be concise and efficient with tool calls.

When done, briefly summarize which files you edited and which violations you fixed vs skipped.`;

export interface ResearchResult {
    /** Files the agent saved (working) during the loop. */
    editedPaths: string[];
    /** The model's final summary text. */
    summary: string;
    /** Violation count before/after the research pass (from rescans), if known. */
    before?: number;
    after?: number;
    steps: number;
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

export async function runResearch(input: RunResearchInput): Promise<ResearchResult> {
    const { violations, deps } = input;
    const model = input.model ?? defaultModel();
    const maxSteps = input.maxSteps ?? 40;
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
        summary: result.text,
        steps: result.steps?.length ?? 0
    };
}
