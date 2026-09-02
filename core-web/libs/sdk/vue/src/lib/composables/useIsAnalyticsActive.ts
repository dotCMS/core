import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue';

import { ANALYTICS_READY_EVENT, isDotAnalyticsActive } from '@dotcms/uve/internal';

/**
 * @internal
 *
 * Composable that tracks whether dotCMS Analytics is active on the page.
 *
 * Analytics may initialize after the page renders, so in addition to reading the
 * current state on mount we subscribe to the `dotcms:analytics:ready` window
 * event and re-evaluate when it fires. Used in live mode to decide whether the
 * minimal contentlet attributes Analytics needs should be kept.
 *
 * @returns a ref that is `true` when Analytics is active
 */
export function useIsAnalyticsActive(): Ref<boolean> {
    const isActive = ref(false);

    const onReady = () => {
        isActive.value = isDotAnalyticsActive();
    };

    onMounted(() => {
        isActive.value = isDotAnalyticsActive();
        window.addEventListener(ANALYTICS_READY_EVENT, onReady);
    });

    onBeforeUnmount(() => {
        window.removeEventListener(ANALYTICS_READY_EVENT, onReady);
    });

    return isActive;
}
