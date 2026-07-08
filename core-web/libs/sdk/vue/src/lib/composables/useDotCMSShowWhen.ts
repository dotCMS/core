import { onMounted, readonly, ref, type Ref } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Composable that reports whether the current UVE mode matches the given mode.
 *
 * Useful for conditionally rendering content based on the editor mode (EDIT,
 * PREVIEW, LIVE). It resolves after mount (returns `false` during SSR), matching
 * the React SDK's `useDotCMSShowWhen` behavior.
 *
 * @example
 * ```ts
 * const showInEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
 * ```
 *
 * @param when the UVE mode to check against
 * @returns a readonly ref that is `true` when the current UVE mode matches
 */
export function useDotCMSShowWhen(when: UVE_MODE): Readonly<Ref<boolean>> {
    const show = ref(false);

    onMounted(() => {
        show.value = getUVEState()?.mode === when;
    });

    return readonly(show);
}
