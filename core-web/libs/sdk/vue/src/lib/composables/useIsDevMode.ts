import { onMounted, ref, type Ref } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';

import { useDotCMSPageContext } from '../contexts/dotcms-page.context';

/**
 * @internal
 *
 * Composable that determines whether the current render is in "development"
 * mode — i.e. whether editor metadata (`data-dot-*` attributes, empty-state
 * placeholders, fallback components) should be emitted.
 *
 * Inside the UVE it follows the UVE state (dev when mode is EDIT); outside the
 * UVE it follows the renderer `mode` from the page context.
 *
 * @returns a ref that is `true` when in development/edit mode
 */
export function useIsDevMode(): Ref<boolean> {
    const { mode } = useDotCMSPageContext();
    const isDevMode = ref(mode === DEVELOPMENT_MODE);

    onMounted(() => {
        // Inside UVE we rely on the UVE state to determine development mode.
        if (getUVEState()?.mode) {
            isDevMode.value = getUVEState()?.mode === UVE_MODE.EDIT;

            return;
        }

        isDevMode.value = mode === DEVELOPMENT_MODE;
    });

    return isDevMode;
}
