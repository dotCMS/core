import { onMounted, ref, type Ref } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';

import { useDotCMSPageContext } from '../contexts/dotcms-page.context';

/**
 * @internal
 *
 * Resolve whether we are rendering in "development" mode — i.e. whether editor
 * metadata (`data-dot-*` attributes, empty-state placeholders, fallback
 * components) should be emitted. Inside the UVE it follows the UVE state (dev
 * when mode is EDIT); otherwise it follows the renderer `mode` from context.
 */
function resolveDevMode(mode: string | undefined): boolean {
    const uveMode = getUVEState()?.mode;

    if (uveMode) {
        return uveMode === UVE_MODE.EDIT;
    }

    return mode === DEVELOPMENT_MODE;
}

/**
 * @internal
 *
 * Composable exposing the development-mode flag as a ref.
 *
 * It resolves **synchronously during setup** (so the editor `data-dot-*`
 * attributes are present on the first render, which the UVE relies on to attach
 * its tooling), and re-checks on mount to cover any environment where `window`
 * / the UVE state only becomes available after the component mounts.
 *
 * @returns a ref that is `true` when in development/edit mode
 */
export function useIsDevMode(): Ref<boolean> {
    const ctx = useDotCMSPageContext();
    const isDevMode = ref(resolveDevMode(ctx.value.mode));

    onMounted(() => {
        isDevMode.value = resolveDevMode(ctx.value.mode);
    });

    return isDevMode;
}
