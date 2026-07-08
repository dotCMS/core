import { onMounted, ref, type Ref } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Reports whether the app is currently rendered inside the UVE in edit mode.
 * Resolves after mount, so it is `false` during the initial (server-safe) render
 * and updates once the UVE state is available.
 */
export function useIsEditMode(): Ref<boolean> {
    const isEditMode = ref(false);

    onMounted(() => {
        isEditMode.value = getUVEState()?.mode === UVE_MODE.EDIT;
    });

    return isEditMode;
}
