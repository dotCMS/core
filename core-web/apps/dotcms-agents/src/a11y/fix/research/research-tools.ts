import { tool, type Tool } from 'ai';
import { z } from 'zod';

import {
    collectSourceRefs,
    errMsg,
    isFixableViolation,
    noop,
    saveMime,
    shortName
} from '../../shared/agent-utils';

import type { RenderSources } from '../../dotcms/dotcms-client';
import type { DotcmsGateway } from '../../dotcms/dotcms-gateway';

/**
 * Typed tools for the agentic research pass (PASS 2). The model uses these to
 * RESEARCH and FIX violations the deterministic pass (PASS 1) couldn't — read any
 * source, grep across them, edit, re-scan — the way the MCP-server agent did.
 *
 * Safe by construction: there is no publish/delete tool, so destructive ops
 * aren't expressible; every call also goes through the path-allowlisted sandbox
 * client (the wire backstop). The model can only read theme/container sources and
 * write the WORKING version.
 */

/** Cap on the chars of a single file returned to the model (context budget). */
const MAX_ASSET_CHARS = 20000;
/** Cap on rescan items / matching grep lines returned to the model. */
const MAX_RESCAN_ITEMS = 40;
const MAX_GREP_LINES = 10;

export interface ResearchToolsDeps {
    client: DotcmsGateway;
    page: { uri: string; hostId: string };
    /** The PREVIEW_MODE scan URL (working content, no editor chrome) for rescan. */
    previewUrl: string;
    /** Files edited so far (shared with PASS 1 caps / reporting). */
    editedPaths: Set<string>;
    /** Per-call source-content cache (avoid re-reading). */
    cache: Record<string, string>;
    onStep?: (phase: string, message: string) => void;
}

export function createResearchTools(deps: ResearchToolsDeps): Record<string, Tool> {
    const { client, page } = deps;
    const step = deps.onStep ?? noop;

    // Memoize _render-sources for the whole research pass (locate + grep reuse it).
    let sourcesPromise: Promise<RenderSources> | undefined;
    const locate = (): Promise<RenderSources> => {
        sourcesPromise ??= client.locate(page.uri, page.hostId);
        return sourcesPromise;
    };

    const readCached = async (path: string): Promise<string> => {
        if (deps.cache[path] === undefined) {
            deps.cache[path] = await client.read(path);
        }
        return deps.cache[path];
    };

    return {
        locateSources: tool({
            description:
                'List the source files that built this page (theme VTLs/CSS/SCSS + container content-type VTLs), as paths only. Start here to discover what to read.',
            inputSchema: z.object({}),
            execute: async () => {
                step('locate', 'Agent: locating sources');
                const refs = collectSourceRefs(await locate());
                return {
                    files: refs.map((r) => r.path),
                    count: refs.length
                };
            }
        }),

        readAsset: tool({
            description:
                'Read the raw text of one source file by its host-qualified path (e.g. //demo.dotcms.com/application/containers/default/activity.vtl). Use to inspect the markup/CSS behind a violation.',
            inputSchema: z.object({
                path: z.string().describe('host-qualified asset path from locateSources')
            }),
            execute: async ({ path }) => {
                step('read', `Agent: reading ${shortName(path)}`);
                try {
                    const content = await readCached(path);
                    // Guard huge files: return a head + length so the model isn't flooded.
                    if (content.length > MAX_ASSET_CHARS) {
                        return {
                            path,
                            length: content.length,
                            truncated: true,
                            head: content.slice(0, MAX_ASSET_CHARS),
                            note: 'File truncated; use grepAssets to find specific text/colors.'
                        };
                    }
                    return { path, length: content.length, content };
                } catch (e) {
                    return { path, error: errMsg(e) };
                }
            }
        }),

        grepAssets: tool({
            description:
                'Search the page source files for a literal string or value (e.g. a failing color "#3b82f6", a selector, an element). Returns matching files with the matching lines. Use to locate where a violation’s color/markup is defined without reading every file.',
            inputSchema: z.object({
                query: z.string().describe('literal substring to find (case-insensitive)'),
                paths: z
                    .array(z.string())
                    .optional()
                    .describe('limit to these paths; default = all located sources')
            }),
            execute: async ({ query, paths }) => {
                step('read', `Agent: grep "${query}"`);
                const refs = collectSourceRefs(await locate());
                const targets = paths?.length ? refs.filter((r) => paths.includes(r.path)) : refs;
                const q = query.toLowerCase();
                // Warm the content cache in parallel, then filter (CPU-only).
                // The catch returns undefined for unreadable files; type it so the
                // array is (string | undefined)[] rather than poisoning to any[].
                const contents = await Promise.all(
                    targets.map((ref): Promise<string | undefined> =>
                        readCached(ref.path).catch((): undefined => undefined)
                    )
                );
                const hits: { path: string; lines: string[] }[] = [];
                targets.forEach((ref, i) => {
                    const content = contents[i];
                    if (content === undefined) {
                        return;
                    }
                    const lines = content
                        .split('\n')
                        .map((line: string, idx: number) => ({ line, lineNo: idx + 1 }))
                        .filter((entry) => entry.line.toLowerCase().includes(q))
                        .map((entry) => `${entry.lineNo}: ${entry.line.trim()}`);
                    if (lines.length) {
                        hits.push({ path: ref.path, lines: lines.slice(0, MAX_GREP_LINES) });
                    }
                });
                return { query, matches: hits, fileCount: hits.length };
            }
        }),

        saveWorking: tool({
            description:
                'Save an edited file to its WORKING version (never published). Provide the FULL new file content. Make the smallest change that fixes the violation; preserve Velocity ($,#) and CSS syntax. Returns the persisted size.',
            inputSchema: z.object({
                path: z.string(),
                content: z.string().describe('the complete edited file content')
            }),
            execute: async ({ path, content }) => {
                step('fix', `Agent: saving ${shortName(path)}`);
                try {
                    const saved = await client.saveWorking(path, content, saveMime(path));
                    if (!saved || saved.fileSize <= 0) {
                        return { path, ok: false, error: 'save returned 0 bytes; not applied' };
                    }
                    deps.editedPaths.add(path);
                    deps.cache[path] = content;
                    return { path, ok: true, fileSize: saved.fileSize, identifier: saved.identifier };
                } catch (e) {
                    return { path, ok: false, error: errMsg(e) };
                }
            }
        }),

        rescan: tool({
            description:
                'Re-scan the working page to check whether your edits cleared violations. Returns the current violation count and the remaining contrast/markup issues. Call after a save to confirm a fix (or detect a regression).',
            inputSchema: z.object({}),
            execute: async () => {
                step('rescan', 'Agent: re-scanning');
                try {
                    const scan = await client.scan(deps.previewUrl);
                    const viols = scan.findings.items.filter(isFixableViolation);
                    return {
                        violations: viols.length,
                        items: viols
                            .slice(0, MAX_RESCAN_ITEMS)
                            .map((f) => ({ code: f.code, selector: f.selector }))
                    };
                } catch (e) {
                    return { error: errMsg(e) };
                }
            }
        })
    };
}
