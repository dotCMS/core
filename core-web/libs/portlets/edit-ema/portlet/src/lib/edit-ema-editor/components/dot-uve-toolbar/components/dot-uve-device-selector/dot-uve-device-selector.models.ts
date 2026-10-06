import type { DotDevice } from '@dotcms/dotcms-models';

import type { Orientation } from '../../../../../store/models';

export interface DeviceSelectorState {
    device: DotDevice | null;
    socialMedia: string | null;
    orientation: Orientation | null;
}

export type DeviceSelectorChange =
    | { type: 'device'; device: DotDevice }
    | { type: 'socialMedia'; socialMedia: string }
    | { type: 'orientation'; orientation: Orientation };
