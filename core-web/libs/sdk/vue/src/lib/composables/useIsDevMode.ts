import { computed, type ComputedRef, type MaybeRefOrGetter, toValue } from 'vue';

import { UVE_MODE, type DotCMSPageRendererMode } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';

/**
 * @internal
 *
 * Resolve whether we are rendering in "development" mode — i.e. whether editor
 * metadata (`data-dot-*` attributes, empty-state placeholders, fallback
 * components) should be emitted. Inside the UVE it follows the UVE state (dev
 * when mode is EDIT); otherwise it follows the renderer `mode`.
 */
export function resolveDevMode(mode: DotCMSPageRendererMode | undefined): boolean {
    const uveMode = getUVEState()?.mode;

    if (uveMode) {
        return uveMode === UVE_MODE.EDIT;
    }

    return mode === DEVELOPMENT_MODE;
}

/**
 * @internal
 *
 * Computed development-mode flag. `getUVEState()` and the renderer `mode` are
 * both available synchronously, so this resolves during setup — the editor
 * `data-dot-*` attributes are present on the first render (which the UVE relies
 * on to attach its tooling) with no per-instance `onMounted`.
 *
 * Called once at the layout root ({@link DotCMSLayoutBody}); the result is
 * shared with the whole tree via the page context.
 */
export function useIsDevMode(
    mode: MaybeRefOrGetter<DotCMSPageRendererMode | undefined>
): ComputedRef<boolean> {
    return computed(() => resolveDevMode(toValue(mode)));
}
