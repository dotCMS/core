import { computed, onMounted, ref, toValue, type ComputedRef, type MaybeRefOrGetter } from 'vue';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Composable that reports whether the current UVE mode matches the given mode.
 *
 * Useful for conditionally rendering content based on the editor mode (EDIT,
 * PREVIEW, LIVE). It resolves after mount (returns `false` during SSR), matching
 * the React SDK's `useDotCMSShowWhen` behavior. The `when` argument may be a
 * plain value, a `ref`, or a getter — a reactive value is re-evaluated when it
 * changes.
 *
 * @example
 * ```ts
 * const showInEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
 * ```
 *
 * @param when the UVE mode to check against
 * @returns a readonly computed that is `true` when the current UVE mode matches
 */
export function useDotCMSShowWhen(
    when: MaybeRefOrGetter<UVE_MODE>
): Readonly<ComputedRef<boolean>> {
    // `getUVEState()` reads `window`, so we resolve it after mount and keep the
    // result `false` during SSR / the initial render.
    const mounted = ref(false);
    onMounted(() => {
        mounted.value = true;
    });

    return computed(() => (mounted.value ? getUVEState()?.mode === toValue(when) : false));
}
