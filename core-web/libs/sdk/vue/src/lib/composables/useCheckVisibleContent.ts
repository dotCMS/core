import { onMounted, ref, type Ref } from 'vue';

/**
 * @internal
 *
 * Composable that reports whether the referenced element rendered visible
 * content (measured height greater than zero).
 *
 * Used in development mode to give empty contentlets a minimum height so the
 * editor can still target them.
 *
 * @param elementRef a template ref to the element to measure
 * @returns a ref that is `true` when the element has a measurable height
 */
export function useCheckVisibleContent(elementRef: Ref<HTMLElement | null>): Ref<boolean> {
    const haveContent = ref(false);

    onMounted(() => {
        const height = elementRef.value?.getBoundingClientRect().height ?? 0;
        haveContent.value = height > 0;
    });

    return haveContent;
}
