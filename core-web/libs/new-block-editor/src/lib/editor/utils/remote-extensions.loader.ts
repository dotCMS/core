import type { AnyExtension } from '@tiptap/core';

import type { Action, DotCMSContentTypeField, RemoteCustomExtensions } from '@dotcms/dotcms-models';

const EMPTY: RemoteCustomExtensions = { extensions: [] };

export interface RemoteExtensionsResolved {
    extensions: AnyExtension[];
    actions: Action[];
}

/**
 * Reads the `customBlocks` field variable as a {@link RemoteCustomExtensions} payload.
 * Returns `{ extensions: [] }` for missing, malformed, or schema-mismatched input
 * (the legacy contract — fields without customBlocks must work). The legacy block
 * editor validates `{url, actions[].{command, menuLabel, icon}}` only; we mirror
 * that exactly so existing customer configs keep loading. For allowed-blocks
 * compatibility, customers must also set `actions[].name` to the TipTap node name
 * exported by the remote bundle (for example `customGallery`).
 */
export function parseCustomBlocksField(
    field: DotCMSContentTypeField | undefined
): RemoteCustomExtensions {
    const raw = field?.fieldVariables?.find((v) => v.key === 'customBlocks')?.value;
    if (!raw) return EMPTY;

    let parsed: unknown;
    try {
        parsed = JSON.parse(raw);
    } catch (err) {
        console.warn('[remote-extension] customBlocks JSON parse failed', err);
        return EMPTY;
    }

    if (!isRemoteCustomExtensions(parsed)) {
        console.warn('[remote-extension] customBlocks did not match expected schema', parsed);
        return EMPTY;
    }

    return parsed;
}

/**
 * Dynamically imports each remote extension URL and reduces the resolved modules
 * into a flat list of TipTap extensions plus the declared menu actions.
 *
 * Failed URL loads are logged and dropped so a single bad host does not prevent
 * the editor from booting (mirrors {@link Promise.allSettled} semantics from the
 * legacy implementation).
 *
 * The `webpackIgnore` magic comment tells webpack not to try to bundle the URL.
 * Vite/esbuild leave dynamic-import string expressions alone by default, but the
 * comment is harmless in either bundler so we keep it for cross-bundler safety.
 */
export async function loadRemoteExtensions(
    parsed: RemoteCustomExtensions
): Promise<RemoteExtensionsResolved> {
    const actions: Action[] = parsed.extensions.flatMap((ext) => ext.actions ?? []);

    const settled = await Promise.allSettled(
        parsed.extensions.map((ext) => import(/* webpackIgnore: true */ ext.url))
    );

    const merged: Record<string, unknown> = settled.reduce<Record<string, unknown>>(
        (acc, result, index) => {
            if (result.status !== 'fulfilled') {
                console.warn(
                    `[remote-extension] failed to load ${parsed.extensions[index].url}`,
                    result.reason
                );
                return acc;
            }
            return { ...acc, ...(result.value as object) };
        },
        {}
    );

    const extensions = Object.values(merged) as AnyExtension[];
    const registeredNodeNames = new Set(
        extensions
            .map((extension) => extension?.name)
            .filter((name): name is string => typeof name === 'string' && name.length > 0)
    );

    actions.forEach((action) => {
        const name = (action as Partial<Action>).name?.trim();

        if (!name) {
            console.warn('[remote-extension] customBlocks action.name is required for remote blocks');
            return;
        }

        if (!registeredNodeNames.has(name)) {
            console.warn(
                `[remote-extension] declared action.name "${name}" did not match any loaded node`
            );
        }
    });

    return {
        extensions,
        actions
    };
}

function isAction(value: unknown): value is Action {
    if (!value || typeof value !== 'object') return false;
    const v = value as Record<string, unknown>;
    return (
        typeof v['command'] === 'string' &&
        typeof v['menuLabel'] === 'string' &&
        typeof v['icon'] === 'string'
    );
}

function isRemoteCustomExtensions(value: unknown): value is RemoteCustomExtensions {
    if (!value || typeof value !== 'object') return false;
    const candidate = (value as { extensions?: unknown }).extensions;
    if (!Array.isArray(candidate)) return false;

    return candidate.every((ext) => {
        if (!ext || typeof ext !== 'object') return false;
        const e = ext as Record<string, unknown>;
        if (typeof e['url'] !== 'string') return false;
        if (e['actions'] === undefined) return true;
        return Array.isArray(e['actions']) && e['actions'].every(isAction);
    });
}
