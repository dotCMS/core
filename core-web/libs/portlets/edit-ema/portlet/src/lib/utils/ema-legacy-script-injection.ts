/**
 * @module ema-legacy-script-injection
 *
 * @deprecated This module is a temporary workaround and will be removed once
 * EMA (Edit Mode Anywhere) headless editing is fully sunset.
 *
 * ## Why this exists
 *
 * PR #34927 moved the `dot-uve.js` editor script injection from the Angular
 * frontend to the backend, embedding it directly in `entity.page.rendered`.
 * PR #34995 then removed the frontend injection code.
 *
 * This broke EMA-based customers (e.g. Freshdesk #36029) because EMA runs
 * headless — the iframe loads the customer's own frontend (Next.js, etc.)
 * and does **not** consume the backend-rendered HTML that now contains the
 * script. As a result, the UVE editor controls stopped appearing in edit mode.
 *
 * ## How it works
 *
 * When the backend feature flag `FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION` is
 * set to `true`, the Angular editor re-injects the `dot-uve.js` script tag
 * into VTL page content before writing it to the iframe — restoring the
 * pre-PR #34995 behavior.
 *
 * ## Removal plan
 *
 * Once all EMA customers have migrated to the Universal Visual Editor (UVE),
 * delete this file and remove:
 * - `FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION` from `FeaturedFlags` enum
 * - The flag entry from `UVE_FEATURE_FLAGS` in `consts.ts`
 * - `$isEmaLegacyScriptInjectionEnabled` from `withPageContext.ts`
 * - The conditional call in `EditEmaEditorComponent.injectCodeToVTL`
 *
 * @see {@link https://github.com/dotCMS/core/pull/34927 PR #34927 — backend script injection}
 * @see {@link https://github.com/dotCMS/core/pull/34995 PR #34995 — frontend injection removal}
 */

/**
 * Path to the UVE editor script served by the dotCMS backend.
 *
 * @deprecated Gated behind `FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION`.
 */
export const SDK_EDITOR_SCRIPT_SOURCE = '/ext/uve/dot-uve.js';

/**
 * Injects the `dot-uve.js` editor script tag into the rendered HTML content.
 *
 * If a `</body>` tag exists, the script is inserted just before it.
 * For advanced templates that may lack a `<body>`, the script is appended
 * at the end of the content.
 *
 * @deprecated Gated behind `FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION`.
 * @param rendered - The HTML string to inject the script into.
 * @returns The HTML string with the editor script tag added.
 */
export function addEditorPageScript(rendered = ''): string {
    const scriptString = `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`;
    const bodyExists = rendered.includes('</body>');

    if (!bodyExists) {
        return rendered + scriptString;
    }

    return rendered.replace('</body>', scriptString + '</body>');
}
