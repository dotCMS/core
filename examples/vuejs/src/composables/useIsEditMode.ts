import { UVE_MODE } from '@dotcms/types';
import { useDotCMSShowWhen } from '@dotcms/vue';
import type { Ref } from 'vue';

/**
 * Reports whether the app is currently rendered inside the UVE in edit mode.
 * A thin, readable alias over the SDK's {@link useDotCMSShowWhen}.
 */
export function useIsEditMode(): Readonly<Ref<boolean>> {
    return useDotCMSShowWhen(UVE_MODE.EDIT);
}
