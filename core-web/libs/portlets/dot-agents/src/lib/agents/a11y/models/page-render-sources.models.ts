/**
 * Types for the "working vs live" file diff view.
 *
 * The diff view answers: "which source files that compose this page differ
 * between the working and the published (live) version?" — i.e. what the agent
 * actually changed but hasn't published yet. It is driven by two backend calls:
 *
 *   1. GET /api/v1/page/_render-sources/{uri}   → the source files of a page
 *      (theme VTLs/CSS, container VTLs, widget VTLs). References only, no content.
 *   2. GET /api/v1/content/versions?identifier=&groupByLang=1  → every version of
 *      a file asset, with `working`/`live` flags and a version-specific
 *      `fileAssetVersion` path (`/dA/<inode>/fileAsset/<name>`) whose bytes are
 *      that exact version's content.
 *
 * The `_render-sources` shapes mirror the Java views in
 * `com.dotcms.rest.api.v1.page` (PageRenderSourcesView & friends).
 */

// ── /api/v1/page/_render-sources/{uri} response ─────────────────────────────

/** Where a container / widget / content-type's Velocity code lives. */
export type RenderSourceType = 'DB' | 'FILE' | 'CODE';

/** A file asset reference (theme file) — path, identifier, extension only. */
export interface FileRefView {
    /** Host-qualified path, e.g. `//demo.dotcms.com/application/themes/travel/header.vtl`. */
    path: string;
    /** File asset identifier. */
    identifier: string;
    /** Lowercased extension without the dot, e.g. `vtl`, `css`, `scss`. */
    extension: string;
}

/** One content type placed in a container. FILE containers also carry path + identifier. */
export interface ContentTypeEntryView {
    contentTypeVar: string;
    /** Host-qualified path to the VTL file (FILE containers only). */
    path?: string;
    /** File asset identifier of the VTL file (FILE containers only). */
    identifier?: string;
}

/** A container referenced by the page template (keyed by container ref in the map). */
export interface ContainerSourceView {
    /** `DB` or `FILE`. */
    source: RenderSourceType;
    contentTypes: ContentTypeEntryView[];
}

/** The page's theme: folder ref + every file under it (recursive). */
export interface ThemeSourceView {
    id: string;
    name: string;
    folderPath: string;
    files: FileRefView[];
}

/** A widget contentlet placed on the page. FILE widgets carry the VTL file ref. */
export interface WidgetSourceView {
    contentTypeVar: string;
    title: string;
    contentletId: string;
    contentletInode: string;
    /** `FILE` (path/identifier populated) or `CODE` (inline Velocity, no file). */
    source: 'FILE' | 'CODE';
    path?: string;
    identifier?: string;
}

/** Lightweight page reference. */
export interface PageSourceRefView {
    identifier: string;
    /** Host-qualified page URI, e.g. `//demo.dotcms.com/index`. */
    uri: string;
    languageId: number;
}

/** Top-level `_render-sources` response entity. */
export interface PageRenderSourcesView {
    page: PageSourceRefView;
    theme: ThemeSourceView;
    /** Keyed by container reference (UUID for DB, host-qualified path for FILE). */
    containers: Record<string, ContainerSourceView>;
    widgets: WidgetSourceView[];
}

// ── Flattened source file + diff models ─────────────────────────────────────

/** Which part of the page a source file belongs to — for grouping / labeling. */
export type PageSourceOrigin = 'theme' | 'container' | 'widget';

/**
 * A single source file of the page, flattened from the `_render-sources` tree
 * and de-duplicated by identifier. Only files backed by a real file asset
 * (with an `identifier`) appear — inline CODE widgets and DB containers have no
 * file to diff and are dropped upstream.
 */
export interface PageSourceFile {
    identifier: string;
    /** Host-qualified path from `_render-sources`. */
    path: string;
    /** Basename for display, e.g. `header.vtl`. */
    name: string;
    /** Lowercased extension without the dot, e.g. `vtl`, `css`. */
    extension: string;
    /** Where in the page this file comes from. */
    origin: PageSourceOrigin;
}

/**
 * A source file whose working and live versions have been resolved to text and
 * found to DIFFER. The diff view lists only these.
 */
export interface PageDiffFile extends PageSourceFile {
    /** Working (unpublished) version text — the "after". */
    working: string;
    /** Live (published) version text — the "before". Empty when the file has no live version yet. */
    live: string;
    /** Added line count (working vs live). */
    added: number;
    /** Removed line count (working vs live). */
    removed: number;
}
