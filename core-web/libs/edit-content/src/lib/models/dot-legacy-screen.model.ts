/** What the generic legacy-screen side panel loads (#38303). */
export interface DotLegacyScreenRequest {
    /** A same-origin admin URL. One that fails `isSameOriginRelativeUrl` renders nothing. */
    url: string;
    /** The iframe's accessible title. */
    title: string;
}

/** The `ng-event` CustomEvent detail the legacy screens dispatch on their own document. */
export interface DotLegacyScreenEvent {
    name: string;
    /** A map for `save-page`, a boolean (dirty) for `edit-contentlet-data-updated`. */
    payload?: Record<string, unknown> | boolean;
    data?: Record<string, unknown>;
}
