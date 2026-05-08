/**
 * localStorage key the dotCMS admin shell reads to render a "back to content" breadcrumb
 * after the user navigates from this editor to a content-editor screen. Mirrors the
 * legacy bubble menu's behaviour exactly so existing return-nav UX keeps working.
 */
const RELATIONSHIP_RETURN_KEY = 'dotcms.relationships.relationshipReturnValue';

/** Strips any " - …" suffix from the parent document title (used as the breadcrumb title). */
function readParentTitle(): string {
    const raw = window.parent
        ? window.parent.document?.title?.split(' - ')?.[0]
        : document?.title?.split(' - ')?.[0];
    return raw || '';
}

/**
 * Writes the breadcrumb the destination content editor uses to render its "back" link.
 * `blockEditorBackUrl` is the current page so the user returns to the same place; the
 * legacy editor used a regex-rewritten variant of the parent URL when it was the target,
 * but in the new editor world `window.location.href` is the canonical answer.
 */
export function writeRelationshipReturnBreadcrumb(inode: string): void {
    try {
        localStorage.setItem(
            RELATIONSHIP_RETURN_KEY,
            JSON.stringify({
                title: readParentTitle(),
                blockEditorBackUrl: window.location.href,
                inode
            })
        );
    } catch {
        // localStorage can fail in private browsing or quota-exceeded; the breadcrumb is
        // a UX nicety, not a correctness requirement, so swallow and keep navigating.
    }
}
