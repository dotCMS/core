import { HttpError, type DotCMSRuntime } from '@dotcms/ai/runtime';

import { errorMessage } from './runtime';

/** The default variant when the caller does not name one. */
export const DEFAULT_VARIANT = 'DEFAULT';
/** The default language id when the caller does not name one. */
export const DEFAULT_LANGUAGE_ID = 1;

/** How a slot's contentlet list is combined with the ids the caller supplies. */
export type PlaceOp = 'append' | 'set' | 'remove';

/** Whether the tool merges into the page's current content or replaces it wholesale. */
export type PlaceMode = 'merge' | 'replace';

/**
 * How the caller points at a slot. Either the 1-based index into the page's real slots (in layout
 * order), or a container reference. `instance` (the slot uuid) is optional ONLY when the named
 * container occupies exactly one slot on the page; otherwise it is required to disambiguate.
 */
export type SlotAddress = number | { container: string; instance?: string };

/** One slot assignment in the multi-slot (`slots[]`) form. */
export interface SlotAssignment {
    slot: SlotAddress;
    contentlets: string[];
    op?: PlaceOp;
}

export interface PagePlaceContentOptions {
    dotcms: DotCMSRuntime;
    /** Page URL path (e.g. "/about-us") or a page identifier (UUID). */
    path: string;
    /**
     * One or more slot assignments applied in a single atomic write. Placing content in one slot is
     * just an array of one: `[{ slot, contentlets }]`.
     */
    slots: SlotAssignment[];
    /** Variant name. Default "DEFAULT". */
    variantName?: string;
    /** Language id. Default 1. */
    languageId?: number;
    /**
     * "merge" (default) reads the page's current content and applies the ops on top, preserving every
     * untouched slot. "replace" treats the supplied slots as the complete desired map — every other
     * slot on the page is cleared. Both still validate slot existence and return a before/after diff.
     */
    mode?: PlaceMode;
}

/** The before/after picture of one slot the write touched or preserved. */
export interface SlotResult {
    /** Container key as the layout addresses it (path for file containers, id for db containers). */
    identifier: string;
    /** Slot instance uuid. */
    uuid: string;
    /** Contentlet ids in the slot before the write. */
    before: string[];
    /** Contentlet ids in the slot after the write. */
    after: string[];
    /** True when before/after differ. */
    changed: boolean;
}

export interface PagePlaceContentManifest {
    /** Identifier of the page whose content was written. */
    pageId: string;
    /** The page's url path. */
    url: string;
    /** Variant the write targeted. */
    variantName: string;
    /** Language id the write targeted. */
    languageId: number;
    /** The effective mode ("merge" or "replace"). */
    mode: PlaceMode;
    /** Per-slot before/after for every slot on the page (touched and untouched). */
    slots: SlotResult[];
    /** Actionable notices: content loss, net-loss conflict, etc. Empty on a clean happy path. */
    warnings: string[];
}

/** A slot discovered on the page: its layout addressing + the content currently in it. */
interface PageSlot {
    /** Container key as the layout addresses it — this is what the POST body's `identifier` must be. */
    identifier: string;
    /** Slot instance uuid, as the layout addresses it — the POST body's `uuid`. */
    uuid: string;
    /** Contentlet identifiers currently in this slot, in order. */
    contentlets: string[];
}

/**
 * Place content into a page's container slots without wiping the rest of the page.
 *
 * `POST /api/v1/page/{pageId}/content` is a FULL replacement of the page's container-to-contentlet
 * map: any slot omitted from the body is emptied. A caller that means "add one contentlet to one
 * slot" but sends only that slot silently clears every other slot. This wrapper absorbs that trap:
 * it reads the page's current content first, applies the requested op(s) to the addressed slot(s)
 * only, then POSTs the COMPLETE map back so untouched slots survive.
 *
 * It also removes the discovery friction: the caller addresses a slot by index or container name
 * and we resolve it to the exact `identifier`+`uuid` the endpoint needs — sourced from the page's
 * real layout, so a typo fails clearly instead of silently doing nothing. The returned manifest
 * gives a before/after diff per slot and flags any slot that lost content.
 */
export async function placeContent(
    options: PagePlaceContentOptions
): Promise<PagePlaceContentManifest> {
    const mode: PlaceMode = options.mode ?? 'merge';
    const variantName = options.variantName ?? DEFAULT_VARIANT;
    const languageId = options.languageId ?? DEFAULT_LANGUAGE_ID;
    const assignments = validateAssignments(options.slots);

    // Read the page's current content. This is the whole point of merge mode, but replace mode needs
    // it too — to validate the addressed slots exist and to build the before/after diff.
    const { pageId, url, slots } = await loadPageSlots(
        options.dotcms,
        options.path,
        languageId,
        variantName
    );

    // Index the real slots two ways so a caller can address either by 1-based layout order or by
    // container (+optional instance uuid). Resolution validates existence and disambiguates.
    const resolved = assignments.map((assignment) => ({
        assignment,
        target: resolveSlot(assignment.slot, slots)
    }));

    // Start from the page's current map. In merge mode we mutate the addressed slots in place; in
    // replace mode we start empty (every slot cleared) and set only what the caller specifies.
    const desired = new Map<string, string[]>();
    for (const slot of slots) {
        desired.set(
            slotKey(slot.identifier, slot.uuid),
            mode === 'replace' ? [] : [...slot.contentlets]
        );
    }

    for (const { assignment, target } of resolved) {
        const key = slotKey(target.identifier, target.uuid);
        const current = desired.get(key) ?? [];
        desired.set(key, applyOp(current, assignment.contentlets, assignment.op ?? 'append'));
    }

    // The POST body is the FULL array — every slot on the page, with its desired contents.
    const body = slots.map((slot) => ({
        identifier: slot.identifier,
        uuid: slot.uuid,
        contentletsId: desired.get(slotKey(slot.identifier, slot.uuid)) ?? []
    }));

    await postContent(options.dotcms, pageId, body, variantName, languageId);

    // Build the diff and warnings from before (page's current) vs after (what we sent).
    const warnings: string[] = [];
    const slotResults: SlotResult[] = slots.map((slot) => {
        const after = desired.get(slotKey(slot.identifier, slot.uuid)) ?? [];
        const before = slot.contentlets;
        const lost = before.filter((id) => !after.includes(id));
        if (lost.length > 0) {
            warnings.push(
                `Slot ${slot.identifier} [uuid ${slot.uuid}] lost ${lost.length} contentlet(s): ` +
                    `${lost.join(', ')}.`
            );
        }
        return {
            identifier: slot.identifier,
            uuid: slot.uuid,
            before,
            after,
            changed: !sameOrder(before, after)
        };
    });

    return { pageId, url, variantName, languageId, mode, slots: slotResults, warnings };
}

/**
 * `slots` is required and must be non-empty. The tool schema also guards this, but the lib is
 * called directly from tests, so it validates too.
 */
function validateAssignments(slots: SlotAssignment[] | undefined): SlotAssignment[] {
    if (!slots || slots.length === 0) {
        throw new Error('`slots` is required and must contain at least one slot assignment.');
    }
    return slots;
}

/** Apply an op to a slot's current contentlet list, preserving order and de-duplicating. */
function applyOp(current: string[], incoming: string[], op: PlaceOp): string[] {
    switch (op) {
        case 'set':
            return dedupe(incoming);
        case 'remove': {
            const remove = new Set(incoming);
            return current.filter((id) => !remove.has(id));
        }
        case 'append':
        default:
            return dedupe([...current, ...incoming]);
    }
}

function dedupe(ids: string[]): string[] {
    const seen = new Set<string>();
    const out: string[] = [];
    for (const id of ids) {
        if (id && !seen.has(id)) {
            seen.add(id);
            out.push(id);
        }
    }
    return out;
}

function sameOrder(a: string[], b: string[]): boolean {
    return a.length === b.length && a.every((v, i) => v === b[i]);
}

/** A stable key for a slot: a container can appear multiple times, so the uuid is part of it. */
function slotKey(identifier: string, uuid: string): string {
    return `${identifier} ${uuid}`;
}

/**
 * Resolve a caller-supplied slot address to a concrete slot on the page. Fails with the list of
 * valid slots when the address does not match, and (for a container address with no instance) with
 * the list of instances when the container occupies more than one slot.
 */
function resolveSlot(address: SlotAddress, slots: PageSlot[]): PageSlot {
    if (typeof address === 'number') {
        const index = address - 1; // 1-based, in layout order.
        if (!Number.isInteger(address) || index < 0 || index >= slots.length) {
            throw new Error(
                `Slot index ${address} is out of range. The page has ${slots.length} slot(s): ` +
                    `${describeSlots(slots)}.`
            );
        }
        return slots[index];
    }

    const wanted = address.container.trim();
    // Rank-filtered, like `findContainer`: keep only the slots matching at the BEST rank, so
    // an exact/path-boundary hit on `.../default/` is never diluted by a loose substring hit on
    // `.../default-banner/`. Without this, naming one container could report "appears in 2
    // slots, pass slot.instance" about two DIFFERENT containers.
    const bestRank = slots.reduce(
        (rank, slot) => Math.max(rank, containerMatchRank(slot.identifier, wanted)),
        MATCH_NONE
    );
    const matches =
        bestRank === MATCH_NONE
            ? []
            : slots.filter((slot) => containerMatchRank(slot.identifier, wanted) === bestRank);

    if (matches.length === 0) {
        throw new Error(
            `No slot on the page uses container "${address.container}". Available slots: ` +
                `${describeSlots(slots)}.`
        );
    }

    if (address.instance !== undefined) {
        const exact = matches.find((slot) => slot.uuid === String(address.instance));
        if (!exact) {
            const instances = matches.map((slot) => `'${slot.uuid}'`).join(', ');
            throw new Error(
                `Container "${address.container}" has no slot with instance '${address.instance}'. ` +
                    `Available instances: ${instances}.`
            );
        }
        return exact;
    }

    if (matches.length > 1) {
        const instances = matches.map((slot) => `'${slot.uuid}'`).join(', ');
        throw new Error(
            `Container "${address.container}" appears in ${matches.length} slots ` +
                `(instances: ${instances}). Pass slot.instance to disambiguate.`
        );
    }

    return matches[0];
}

/**
 * How well a layout container key matches what the caller asked for. Higher is better;
 * `NONE` means no match at all.
 *
 * Ranked rather than boolean, because the previous "either string contains the other" test
 * made resolution ORDER-DEPENDENT and therefore non-deterministic from the caller's side: a
 * layout holding both `.../containers/default/` and `.../containers/default-banner/` would
 * resolve a request for `.../default/` to whichever appeared FIRST in the object. Object keys
 * iterate in insertion order, so which container won depended on layout authoring order — and
 * in `merge` mode the tool would read the banner's contentlet list and write the merged result
 * back under it, putting content in the wrong container and potentially replacing the banner's
 * own contents.
 */
const MATCH_NONE = 0;
/** One string contains the other anywhere — the loosest, most ambiguous signal. */
const MATCH_SUBSTRING = 1;
/** The key ends with the wanted value on a `/` boundary, e.g. `.../containers/default/`. */
const MATCH_PATH_SUFFIX = 2;
/** Byte-identical (case-insensitively). */
const MATCH_EXACT = 3;

function containerMatchRank(identifier: string, wanted: string): number {
    const key = identifier.toLowerCase();
    const want = wanted.toLowerCase();

    if (key === want) {
        return MATCH_EXACT;
    }

    // Compare on `/`-delimited boundaries so `default` cannot match `default-banner`. Both
    // sides are normalised for a trailing slash first, since container paths carry one and
    // callers routinely omit it.
    const keyTrimmed = key.replace(/\/+$/, '');
    const wantTrimmed = want.replace(/\/+$/, '');
    if (keyTrimmed === wantTrimmed || keyTrimmed.endsWith(`/${wantTrimmed}`)) {
        return MATCH_PATH_SUFFIX;
    }

    if (key.includes(want) || want.includes(key)) {
        return MATCH_SUBSTRING;
    }

    return MATCH_NONE;
}

/**
 * The single best-matching key, or a hard error when the choice is genuinely ambiguous.
 *
 * Failing loudly with the candidates beats silently picking one: writing to the wrong
 * container is not recoverable by the caller, whereas an error naming both candidates tells
 * them exactly what to disambiguate with.
 */
function bestContainerKey(keys: string[], wanted: string): string | undefined {
    let bestRank = MATCH_NONE;
    let best: string[] = [];

    for (const key of keys) {
        const rank = containerMatchRank(key, wanted);
        if (rank === MATCH_NONE || rank < bestRank) {
            continue;
        }
        if (rank > bestRank) {
            bestRank = rank;
            best = [key];
        } else {
            best.push(key);
        }
    }

    if (best.length === 0) {
        return undefined;
    }

    if (best.length > 1) {
        throw new Error(
            `Container "${wanted}" is ambiguous — it matches ${best.length} containers on this ` +
                `page equally well: ${best.join(', ')}. Pass the full container path or id to ` +
                `disambiguate; guessing here could write content into the wrong container.`
        );
    }

    return best[0];
}

function describeSlots(slots: PageSlot[]): string {
    if (slots.length === 0) return '(none)';
    return slots.map((slot, i) => `#${i + 1} ${slot.identifier} [uuid ${slot.uuid}]`).join('; ');
}

interface PageJsonResponse {
    entity?: {
        page?: { identifier?: string; pageURI?: string; pageUrl?: string; url?: string };
        containers?: Record<string, RawContainer>;
        layout?: { body?: { rows?: LayoutRow[] } };
    };
}

interface RawContainer {
    contentlets?: Record<string, Array<{ identifier?: string; inode?: string }>>;
}

interface LayoutRow {
    columns?: Array<{ containers?: Array<{ identifier?: string; uuid?: string }> }>;
}

/**
 * Fetch the page's current content and flatten it into the ordered list of slots we operate on.
 *
 * The slot order and the authoritative identifier+uuid come from `layout.body.rows[].columns[]
 * .containers[]` — this is exactly what the endpoint expects back. The current contentlet ids come
 * from `containers[identifier].contentlets[uuid]`, matched tolerantly because the layout key and the
 * containers-map key can differ (shorty vs full id, host-relative vs host-qualified path) and the
 * contentlets map is historically keyed as either "1" or "uuid-1".
 */
async function loadPageSlots(
    dotcms: DotCMSRuntime,
    path: string,
    languageId: number,
    variantName: string
): Promise<{ pageId: string; url: string; slots: PageSlot[] }> {
    const uri = path.trim().startsWith('/') ? path.trim() : `/${path.trim()}`;
    // Read the SAME variant the write targets. Omitting `variantName` here reads DEFAULT,
    // so in `merge` mode on a non-DEFAULT variant the "before" slot map and the
    // untouched-slot preservation would be computed from DEFAULT and then written into the
    // target variant — clobbering its real contents and reporting a bogus before/after diff.
    const response = (await dotcms.request({
        path: `/api/v1/page/json${uri}`,
        query: { variantName, language_id: languageId }
    })) as PageJsonResponse;

    const entity = response.entity;
    if (!entity || !entity.page) {
        throw new Error(
            `Page "${path}" was not found (no page at this url for language ${languageId}).`
        );
    }

    const pageId = entity.page.identifier;
    if (!pageId) {
        throw new Error(`Page "${path}" resolved but has no identifier.`);
    }
    const url = entity.page.pageURI ?? entity.page.pageUrl ?? entity.page.url ?? uri;

    const containers = entity.containers ?? {};
    const rows = entity.layout?.body?.rows ?? [];

    const slots: PageSlot[] = [];
    for (const row of rows) {
        for (const column of row.columns ?? []) {
            for (const container of column.containers ?? []) {
                const identifier = container.identifier;
                const uuid = container.uuid;
                if (!identifier || !uuid) {
                    continue;
                }
                slots.push({
                    identifier,
                    uuid,
                    contentlets: currentContentlets(containers, identifier, uuid)
                });
            }
        }
    }

    return { pageId, url, slots };
}

/**
 * Read the contentlet ids currently in a slot from the containers map. The layout `identifier` may
 * not be byte-identical to the containers-map key, and the per-slot key may be "uuid" or "uuid-N",
 * so both lookups are tolerant.
 */
function currentContentlets(
    containers: Record<string, RawContainer>,
    identifier: string,
    uuid: string
): string[] {
    const raw = findContainer(containers, identifier);
    if (!raw?.contentlets) {
        return [];
    }

    const map = raw.contentlets;
    const list =
        map[uuid] ??
        map[`uuid-${uuid}`] ??
        map[stripUuidPrefix(uuid)] ??
        // Last resort: a key that ends with the uuid (covers other prefixings).
        map[Object.keys(map).find((k) => stripUuidPrefix(k) === stripUuidPrefix(uuid)) ?? ''];

    if (!Array.isArray(list)) {
        return [];
    }
    return list.map((c) => c.identifier).filter((id): id is string => Boolean(id));
}

function stripUuidPrefix(uuid: string): string {
    return uuid.startsWith('uuid-') ? uuid.slice('uuid-'.length) : uuid;
}

function findContainer(
    containers: Record<string, RawContainer>,
    identifier: string
): RawContainer | undefined {
    if (containers[identifier]) {
        return containers[identifier];
    }
    const key = bestContainerKey(Object.keys(containers), identifier);

    return key ? containers[key] : undefined;
}

/**
 * POST the full container map, translating the two documented non-200 outcomes into actionable
 * messages: 409 is the net-loss conflict (someone else changed the page, or the write would
 * remove too much), and a 400 usually means a contentlet's type is not allowed in its container.
 *
 * Both branches are keyed on `HttpError.status`, not on the text of the message. The 409 used to
 * be sniffed with `/\b409\b|net content loss|conflict/i`, which both over-matched (any response
 * body happening to contain "409" or "conflict") and under-matched (a real 409 whose body says
 * neither). `cause` is threaded through so the original typed error — and with it `code` and
 * `status` — still reaches the tool boundary instead of being flattened away here.
 */
async function postContent(
    dotcms: DotCMSRuntime,
    pageId: string,
    body: Array<{ identifier: string; uuid: string; contentletsId: string[] }>,
    variantName: string,
    languageId: number
): Promise<void> {
    try {
        await dotcms.request({
            method: 'POST',
            path: `/api/v1/page/${encodeURIComponent(pageId)}/content`,
            query: { variantName, language_id: languageId },
            body
        });
    } catch (error) {
        const message = errorMessage(error);

        if (error instanceof HttpError && error.status === 409) {
            throw withCause(
                'Page content save was rejected as a net-loss conflict (the change would remove more ' +
                    'content than allowed, or the page changed underneath this write). Re-read the page ' +
                    `and retry. Original error: ${message}`,
                error
            );
        }

        // The 400 branch the docblock has always promised but never had. This is the single
        // most common failure of this tool in a placement loop, and without naming the cause
        // the model cannot tell that the fix is a different container or a different content
        // type — so it retries the identical call and fails identically.
        if (error instanceof HttpError && error.status === 400) {
            throw withCause(
                'Page content save was rejected (HTTP 400). The usual cause is a contentlet whose ' +
                    'CONTENT TYPE is not permitted in the container it was placed in — check the ' +
                    "container's allowed content types and either place a permitted type or choose " +
                    'a different container. Retrying this same call unchanged will fail the same ' +
                    `way. Original error: ${message}`,
                error
            );
        }

        throw withCause(`Failed to save page content: ${message}`, error);
    }
}

/**
 * An `Error` carrying the original as `cause`.
 *
 * Written by assignment rather than `new Error(msg, { cause })` because that constructor
 * overload needs the ES2022 lib and this project targets lower. Threading the cause matters:
 * without it the typed `HttpError` — and with it `code` and `status` — is flattened to a
 * message here and can never reach the tool boundary that reports `retryable`.
 */
function withCause(message: string, cause: unknown): Error {
    const error = new Error(message);
    (error as Error & { cause?: unknown }).cause = cause;

    return error;
}
