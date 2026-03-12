import { Pipe, PipeTransform } from '@angular/core';

/**
 * Computes the nesting depth of a host by counting the non-empty
 * path segments in its {@code parentPath}.
 *
 * <p>dotCMS stores a host's location in the Identifier table using the
 * {@code parent_path} column.  The root-level value is {@code "/"}.
 * A first-level child host has a parent_path like {@code "/parent/"}, a
 * second-level child has {@code "/parent/child/"}, and so on.  The depth
 * is therefore the number of non-empty segments produced by splitting on
 * {@code "/"}.
 *
 * <h3>Examples</h3>
 * <pre>
 *  "/"                  → 0  (top-level / root host)
 *  "/parent/"           → 1  (one level deep)
 *  "/parent/child/"     → 2  (two levels deep)
 *  ""                   → 0  (treated as root)
 *  null / undefined     → 0  (treated as root)
 * </pre>
 *
 * @export
 * @function getHostDepth
 */
export function getHostDepth(parentPath: string | null | undefined): number {
    if (!parentPath) {
        return 0;
    }

    return parentPath.split('/').filter(Boolean).length;
}

/**
 * Angular pipe wrapper around {@link getHostDepth}.
 *
 * Usage in templates:
 * ```html
 * <div [style.padding-left.rem]="host.parentPath | dotHostDepth">
 * ```
 *
 * @export
 * @class DotHostDepthPipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'dotHostDepth',
    standalone: true,
    pure: true
})
export class DotHostDepthPipe implements PipeTransform {
    transform(parentPath: string | null | undefined): number {
        return getHostDepth(parentPath);
    }
}
