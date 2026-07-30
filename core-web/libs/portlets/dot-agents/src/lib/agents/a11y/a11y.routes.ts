import { Route, UrlMatchResult, UrlSegment } from '@angular/router';

import { DotA11yDiffComponent } from './a11y-diff/a11y-diff.component';
import { DotA11yPickerComponent } from './a11y-picker/a11y-picker.component';
import { DotA11yRootComponent } from './a11y-root/a11y-root.component';
import { DotA11yRunComponent } from './a11y-run/a11y-run.component';

/**
 * Matches the diff route: any non-empty page path whose LAST segment is `diff`
 * (e.g. `blog/post/hello/diff`). Consumes every segment so the child component
 * can reconstruct the page URI (and drop the trailing `diff` marker). Returns
 * null (no match) for the bare picker and for run paths without the marker, so
 * ordering with {@link runMatcher} is unambiguous.
 */
export function diffMatcher(segments: UrlSegment[]): UrlMatchResult | null {
    if (segments.length >= 1 && segments[segments.length - 1].path === 'diff') {
        return { consumed: segments };
    }

    return null;
}

/**
 * Matches the run route: any non-empty page path (e.g. `blog/post/hello`). Must
 * be registered AFTER {@link diffMatcher} so a trailing `/diff` is claimed by the
 * diff route first. The empty path is handled by the picker route above, so this
 * only fires for real page paths.
 */
export function runMatcher(segments: UrlSegment[]): UrlMatchResult | null {
    return segments.length >= 1 ? { consumed: segments } : null;
}

/**
 * Accessibility Studio routes. The root component provides the store (so state
 * survives the picker↔run↔diff switch) and hosts a `router-outlet`; the child
 * routes pick the screen — and put the selected page's URI in the URL:
 *   - `''`            → the page picker
 *   - `<path>/diff`   → the working-vs-live file diff view (matched first)
 *   - `<path>`        → the run screen; captures the page path verbatim
 *                       (e.g. `/agents/a11y/blog/post/hello`) so runs are
 *                       deep-linkable and shareable with a human-readable URL.
 *
 * The diff matcher must come before the run matcher: both consume all segments,
 * but the diff one only matches when the last segment is `diff`.
 */
export const dotAccessibilityStudioRoutes: Route[] = [
    {
        path: '',
        component: DotA11yRootComponent,
        children: [
            { path: '', component: DotA11yPickerComponent },
            { matcher: diffMatcher, component: DotA11yDiffComponent },
            { matcher: runMatcher, component: DotA11yRunComponent }
        ]
    }
];
