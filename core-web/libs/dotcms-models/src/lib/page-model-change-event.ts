import { DotPageContainer } from '@dotcms/dotcms-models';

import { PageModelChangeEventType } from './page-model-change-event.type';

export interface PageModelChangeEvent {
    model: DotPageContainer[];
    type: PageModelChangeEventType;
}
