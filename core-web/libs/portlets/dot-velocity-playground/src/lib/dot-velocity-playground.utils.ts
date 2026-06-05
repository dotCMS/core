import { DotVelocityResponseContentType } from './models/dot-velocity-playground.models';

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

/**
 * Read a JSON value from localStorage with a typed fallback. Returns the
 * fallback in non-browser environments, when the key is missing, or when the
 * payload can't be parsed.
 */
export const readJson = <T>(key: string, fallback: T): T => {
    if (typeof window === 'undefined') return fallback;
    try {
        const raw = window.localStorage.getItem(key);
        if (raw == null) return fallback;
        return JSON.parse(raw) as T;
    } catch {
        return fallback;
    }
};

/** Persist a JSON-serializable value to localStorage; silently noop on quota / private mode. */
export const writeJson = (key: string, value: unknown): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
        // Storage may be unavailable (quota, private mode) — ignore
    }
};

/** Remove a localStorage entry; silently noop when storage is unavailable. */
export const removeKey = (key: string): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.removeItem(key);
    } catch {
        // ignore
    }
};

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
