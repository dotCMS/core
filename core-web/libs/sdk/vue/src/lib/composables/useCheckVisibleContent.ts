import { onMounted, ref, type Ref } from 'vue';

/**
 * @internal
 *
 * Composable that reports whether an element rendered visible content (measured
 * height greater than zero). Used in development mode to give empty contentlets
 * a minimum height so the editor can still target them.
 *
 * Takes a getter for the element (rather than a template ref) so the caller can
 * resolve it from the component instance's root element and avoid attaching a
 * template ref to a wrapper vnode — which Vue may hoist, triggering a warning.
 *
 * @param getElement returns the element to measure (called on mount)
 * @returns a ref that is `true` when the element has a measurable height
 */
export function useCheckVisibleContent(getElement: () => Element | null | undefined): Ref<boolean> {
    const haveContent = ref(false);

    onMounted(() => {
        const height = getElement()?.getBoundingClientRect().height ?? 0;
        haveContent.value = height > 0;
    });

    return haveContent;
}
