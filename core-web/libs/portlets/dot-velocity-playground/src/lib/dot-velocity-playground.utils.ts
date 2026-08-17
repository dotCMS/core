import { HttpErrorResponse } from '@angular/common/http';

import {
    DotVelocityPlaygroundError,
    DotVelocityResponseContentType,
    VelocityError,
    VelocityErrorResponse,
    VelocityWarning
} from './models/dot-velocity-playground.models';

export const HISTORY_STORAGE_KEY = 'velocityPlayground';
export const SPLITTER_STORAGE_KEY = 'velocityPlayground.splitterRatio';
export const WRAP_STORAGE_KEY = 'velocityPlayground.wrap';

export const HISTORY_MAX_ENTRIES = 10;
export const HISTORY_LABEL_MAX_LENGTH = 60;
export const DEFAULT_SPLITTER_RATIO: readonly [number, number] = [50, 50];
export const JSON_PRETTY_PRINT_MAX_BYTES = 512_000;

export interface VelocityHelpExample {
    title: string;
    code: string;
    description?: string;
}

export interface VelocityDownloadParams {
    ext: 'txt' | 'json' | 'xml';
    mime: 'text/plain' | 'application/json' | 'application/xml';
}

/** Type guard: the raw value is an array of strings (suitable as history). */
export const isValidHistory = (value: unknown): value is string[] =>
    Array.isArray(value) && value.every((entry) => typeof entry === 'string');

/** Type guard: the raw value is a 2-tuple of finite numbers (splitter ratio). */
export const isValidRatio = (value: unknown): value is [number, number] =>
    Array.isArray(value) &&
    value.length === 2 &&
    value.every((n) => typeof n === 'number' && Number.isFinite(n));

/**
 * Insert `entry` at the head of `history`, trimming whitespace so values that
 * differ only by padding collapse to one, and capping the list at
 * HISTORY_MAX_ENTRIES. Returns `history` unchanged when the trimmed entry is
 * empty.
 */
export const dedupeAndCap = (history: string[], entry: string): string[] => {
    const trimmed = entry.trim();
    if (!trimmed) return history;
    const filtered = history.filter((item) => item !== trimmed);
    return [trimmed, ...filtered].slice(0, HISTORY_MAX_ENTRIES);
};

/**
 * Pretty-print a JSON response body with 2-space indentation. Falls back to
 * the raw body for non-JSON content types, malformed payloads, and bodies
 * larger than JSON_PRETTY_PRINT_MAX_BYTES (Monaco already struggles past
 * that size and the parse/stringify round-trip doubles memory).
 */
export const formatBody = (body: string, contentType: DotVelocityResponseContentType): string => {
    if (contentType !== 'json' || !body.trim()) return body;
    if (body.length > JSON_PRETTY_PRINT_MAX_BYTES) return body;
    try {
        return JSON.stringify(JSON.parse(body), null, 2);
    } catch {
        return body;
    }
};

/**
 * Build a compact, single-line label for a history entry. Collapses internal
 * whitespace, trims, truncates to HISTORY_LABEL_MAX_LENGTH characters with an
 * ellipsis, and falls back to `emptyFallback` when the entry is blank.
 */
export const formatHistoryLabel = (entry: string, emptyFallback: string): string => {
    const compact = entry.replace(/\s+/g, ' ').trim();
    if (compact.length > HISTORY_LABEL_MAX_LENGTH) {
        return `${compact.slice(0, HISTORY_LABEL_MAX_LENGTH)}…`;
    }
    return compact || emptyFallback;
};

/**
 * Map a response content type to the download extension + MIME pair used when
 * the user exports the output as a file.
 */
export const getDownloadParams = (
    contentType: DotVelocityResponseContentType
): VelocityDownloadParams => {
    if (contentType === 'json') return { ext: 'json', mime: 'application/json' };
    if (contentType === 'xml') return { ext: 'xml', mime: 'application/xml' };
    return { ext: 'txt', mime: 'text/plain' };
};

/** i18n key used when we can't extract any usable message from a failed run. */
export const UNKNOWN_ERROR_KEY = 'velocityPlayground.error.unknown';

/** Type guard: a single Velocity error object carrying at least a string `message`. */
const isVelocityErrorObject = (value: unknown): value is VelocityError =>
    typeof value === 'object' &&
    value !== null &&
    typeof (value as VelocityError).message === 'string';

/** Type guard: the structured `{ errors: VelocityError[] }` body from a `400`. */
const isVelocityErrorResponse = (value: unknown): value is VelocityErrorResponse =>
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as VelocityErrorResponse).errors) &&
    (value as VelocityErrorResponse).errors.length > 0 &&
    isVelocityErrorObject((value as VelocityErrorResponse).errors[0]);

/**
 * Coerce an `HttpErrorResponse.error` into a plain object. The service uses
 * `responseType: 'text'`, so a structured `400` arrives as a JSON *string* that
 * must be parsed; a defensive object branch covers interceptors that may have
 * already parsed it. Returns `null` for anything that isn't structured JSON.
 */
const coerceErrorBody = (raw: unknown): unknown => {
    if (typeof raw === 'object' && raw !== null) return raw;
    if (typeof raw === 'string') {
        const trimmed = raw.trim();
        if (!trimmed.startsWith('{')) return null;
        try {
            return JSON.parse(trimmed);
        } catch {
            return null;
        }
    }
    return null;
};

/**
 * Normalize a failed `POST /api/vtl/dynamic` run into a `DotVelocityPlaygroundError`.
 *
 * Recognizes the structured `400` contract (`{ errors: [{ message, errorType,
 * templateName, line, column }] }`) and returns its first error as `structured`.
 * Otherwise falls back to the raw text body, then the nested `error.message`,
 * then the top-level message, then an i18n key — always yielding a non-empty
 * `message` for the banner.
 *
 * `isVelocityError` is `true` only for the structured Velocity contract; callers
 * use it to keep VTL syntax/runtime errors inline and skip the global (modal)
 * error handler, which should stay reserved for infrastructure failures.
 */
export const parseVelocityError = (
    error: HttpErrorResponse | null | undefined
): { error: DotVelocityPlaygroundError; isVelocityError: boolean } => {
    const body = coerceErrorBody(error?.error);

    if (isVelocityErrorResponse(body)) {
        const first = body.errors[0];
        const warnings = Array.isArray(body.warnings) ? body.warnings : [];
        return {
            error: { message: first.message, structured: first, warnings },
            isVelocityError: true
        };
    }

    const rawText =
        typeof error?.error === 'string' && error.error.trim() ? error.error.trim() : null;
    const nestedMessage =
        isVelocityErrorObject(body) && body.message.trim() ? body.message.trim() : null;

    const message = rawText ?? nestedMessage ?? error?.message?.trim() ?? UNKNOWN_ERROR_KEY;

    return {
        error: { message: message || UNKNOWN_ERROR_KEY, structured: null, warnings: [] },
        isVelocityError: false
    };
};

/**
 * Parse the `X-Dot-Velocity-Warnings` response header (a JSON array of
 * `VelocityWarning`) sent on a successful run. Returns an empty array when the
 * header is absent, empty, or malformed — warnings are best-effort context and
 * must never break the success path.
 */
export const parseWarningsHeader = (raw: string | null | undefined): VelocityWarning[] => {
    if (!raw || !raw.trim()) return [];
    try {
        const parsed: unknown = JSON.parse(raw);
        return Array.isArray(parsed) ? (parsed as VelocityWarning[]) : [];
    } catch {
        return [];
    }
};

/**
 * Reduce a (possibly multi-line) error message to a single-line summary for the
 * banner. Velocity parse errors carry a long "Was expecting one of: …" dump on
 * subsequent lines — that belongs in the Monaco trace, not the banner. Returns
 * the first non-empty line, trimmed.
 */
export const firstLine = (message: string): string => {
    const match = message.split('\n').find((line) => line.trim().length > 0);
    return (match ?? message).trim();
};

/**
 * Render a normalized Velocity error as a plain-text, stack-trace-style block for
 * the read-only Monaco output pane. Shows the full engine output (the backend's
 * `detail` when present, otherwise the summary), the location, and any collected
 * warnings — everything the caller needs to fix the code, in one copyable block.
 *
 * `message` may be an i18n key for the unknown-error fallback; the caller passes
 * an already-resolved string via `resolvedMessage` so this stays pure/DOM-free.
 */
export const formatErrorTrace = (
    error: DotVelocityPlaygroundError,
    resolvedMessage: string
): string => {
    const lines: string[] = [];
    const detail = error.structured;

    // Prefer the full engine output (detail) over the one-line summary for the body.
    const body = detail?.detail?.trim() ? detail.detail.trim() : resolvedMessage;
    const header = detail?.errorType ? `${detail.errorType}: ${body}` : body;
    lines.push(header);

    if (detail) {
        if (detail.templateName) {
            lines.push(`    at template "${detail.templateName}"`);
        }
        if (detail.line !== undefined && detail.line !== null) {
            const col =
                detail.column !== undefined && detail.column !== null
                    ? `, column ${detail.column}`
                    : '';
            lines.push(`    at line ${detail.line}${col}`);
        }
    }

    const warningLines = formatWarnings(error.warnings);
    if (warningLines) {
        lines.push('', warningLines);
    }

    return lines.join('\n');
};

/**
 * Format a list of Velocity warnings as a plain-text block for the trace pane.
 * Returns an empty string when there are none.
 */
export const formatWarnings = (warnings: VelocityWarning[]): string => {
    if (!warnings.length) return '';
    const header = warnings.length === 1 ? '1 warning:' : `${warnings.length} warnings:`;
    const lines = warnings.map((w) => {
        const loc =
            w.line !== undefined && w.line !== null
                ? ` (line ${w.line}${w.column !== undefined && w.column !== null ? `, column ${w.column}` : ''})`
                : '';
        return `  - [${w.type}] ${w.message}${loc}`;
    });
    return [header, ...lines].join('\n');
};

/**
 * Static catalog of example snippets shown in the help popover. Titles and
 * descriptions are i18n keys resolved with DotMessagePipe at render time.
 */
export const VELOCITY_HELP_EXAMPLES: VelocityHelpExample[] = [
    {
        title: 'velocityPlayground.help.example.contentSnapshot',
        description: 'velocityPlayground.help.example.contentSnapshot.desc',
        code: '#set($types = ["htmlpageasset","webPageContent","FileAsset","persona","Vanityurl"])\nContent live on $host.hostname:\n#foreach($t in $types)\n  #set($n = $dotcontent.pull("+contentType:$t +live:true +conhost:$host.identifier", 1000, "modDate").size())\n  - $t: $n\n#end'
    },
    {
        title: 'velocityPlayground.help.example.pullPages',
        description: 'velocityPlayground.help.example.pullPages.desc',
        code: '#set($pages = $dotcontent.pull("+contentType:htmlpageasset +live:true +conhost:$host.identifier", 10, "modDate desc"))\nFound $pages.size() page(s):\n#foreach($page in $pages)\n  - $page.title  ($page.pageUrl)\n#end'
    },
    {
        title: 'velocityPlayground.help.example.transformToJsonApi',
        description: 'velocityPlayground.help.example.transformToJsonApi.desc',
        code: '#set($pages = $dotcontent.pull("+contentType:htmlpageasset +live:true +conhost:$host.identifier", 10, "modDate desc"))\n#set($items = [])\n#foreach($p in $pages)\n  #set($entry = {\n    "id":      $p.identifier,\n    "title":   $p.title,\n    "url":     $p.pageUrl,\n    "modDate": $date.format("yyyy-MM-dd\'T\'HH:mm:ssZ", $p.modDate)\n  })\n  $items.add($entry)\n#end\n$dotJSON.put("site", $host.hostname)\n$dotJSON.put("count", $items.size())\n$dotJSON.put("items", $items)'
    },
    {
        title: 'velocityPlayground.help.example.pullFiles',
        description: 'velocityPlayground.help.example.pullFiles.desc',
        code: '#set($files = $dotcontent.pull("+contentType:FileAsset +live:true +conhost:$host.identifier", 5, "modDate desc"))\n#foreach($f in $files)\n  - $f.fileName  ($f.fileSize bytes, $f.mimeType)\n#end'
    }
];
