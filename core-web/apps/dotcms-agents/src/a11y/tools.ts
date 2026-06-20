import { tool, type Tool } from 'ai';
import { z } from 'zod';

import type { DotcmsClient, RenderSources, SourceRef } from './dotcms-client';

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

export interface ResearchToolsDeps {
    client: DotcmsClient;
    page: { uri: string; hostId: string };
    /** The two scan URLs (live, editMode) for rescan. */
    editModeUrl: string;
    /** Files edited so far (shared with PASS 1 caps / reporting). */
    editedPaths: Set<string>;
    /** Per-call source-content cache (avoid re-reading). */
    cache: Record<string, string>;
    onStep?: (phase: string, message: string) => void;
}

/** Collect the editable source refs (theme files + container VTLs) for grep/locate. */
function editableRefs(sources: RenderSources): SourceRef[] {
    const refs: SourceRef[] = (sources.theme?.files ?? []).filter((f) => {
        const ext = (f.extension ?? '').toLowerCase();
        return ['vtl', 'css', 'scss', 'sass', 'dotsass', 'less'].includes(ext);
    });
    for (const c of Object.values(sources.containers ?? {})) {
        for (const ct of c.contentTypes ?? []) {
            if (ct.path) {
                refs.push({ identifier: ct.identifier, path: ct.path });
            }
        }
    }
    const seen = new Set<string>();
    return refs.filter((r) => (seen.has(r.path) ? false : (seen.add(r.path), true)));
}

const STYLESHEET_EXT = ['.css', '.scss', '.sass', '.dotsass', '.less'];
const saveMime = (p: string) =>
    STYLESHEET_EXT.some((e) => p.toLowerCase().endsWith(e)) ? 'text/css' : 'text/plain';

export function createResearchTools(deps: ResearchToolsDeps): Record<string, Tool> {
    const { client, page } = deps;
    const step = deps.onStep ?? (() => undefined);

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
                const sources = await client.locate(page.uri, page.hostId);
                const refs = editableRefs(sources);
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
                step('read', `Agent: reading ${path.split('/').pop()}`);
                try {
                    const content = await readCached(path);
                    // Guard huge files: return a head + length so the model isn't flooded.
                    if (content.length > 20000) {
                        return {
                            path,
                            length: content.length,
                            truncated: true,
                            head: content.slice(0, 20000),
                            note: 'File truncated; use grepAssets to find specific text/colors.'
                        };
                    }
                    return { path, length: content.length, content };
                } catch (e) {
                    return { path, error: e instanceof Error ? e.message : String(e) };
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
                const sources = await client.locate(page.uri, page.hostId);
                const refs = editableRefs(sources);
                const targets = paths?.length ? refs.filter((r) => paths.includes(r.path)) : refs;
                const q = query.toLowerCase();
                const hits: { path: string; lines: string[] }[] = [];
                for (const ref of targets) {
                    let content: string;
                    try {
                        content = await readCached(ref.path);
                    } catch {
                        continue;
                    }
                    const lines = content
                        .split('\n')
                        .map((l, i) => ({ l, i: i + 1 }))
                        .filter((x) => x.l.toLowerCase().includes(q))
                        .map((x) => `${x.i}: ${x.l.trim()}`);
                    if (lines.length) {
                        hits.push({ path: ref.path, lines: lines.slice(0, 10) });
                    }
                }
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
                step('fix', `Agent: saving ${path.split('/').pop()}`);
                try {
                    const saved = await client.saveWorking(path, content, saveMime(path));
                    if (!saved || saved.fileSize <= 0) {
                        return { path, ok: false, error: 'save returned 0 bytes; not applied' };
                    }
                    deps.editedPaths.add(path);
                    deps.cache[path] = content;
                    return { path, ok: true, fileSize: saved.fileSize, identifier: saved.identifier };
                } catch (e) {
                    return { path, ok: false, error: e instanceof Error ? e.message : String(e) };
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
                    const scan = await client.scan(deps.editModeUrl);
                    const viols = scan.findings.items.filter(
                        (f) =>
                            (f.resultType === 'violation' || f.type === 'error') &&
                            // exclude only genuine editor chrome (edit/add buttons), NOT
                            // contentlet/container wrappers (those are attribution metadata)
                            !/data-dot-object="(edit-content|edit-container|add)"/.test(
                                f.context ?? ''
                            )
                    );
                    return {
                        violations: viols.length,
                        items: viols
                            .slice(0, 40)
                            .map((f) => ({ code: f.code, selector: f.selector }))
                    };
                } catch (e) {
                    return { error: e instanceof Error ? e.message : String(e) };
                }
            }
        })
    };
}
