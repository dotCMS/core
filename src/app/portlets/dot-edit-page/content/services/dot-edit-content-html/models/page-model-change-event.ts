import { DotPageContainer } from '@portlets/dot-edit-page/shared/models/dot-page-container.model';
import { PageModelChangeEventType } from './page-model-change-event.type';

export interface PageModelChangeEvent {
    model: DotPageContainer[];
    type: PageModelChangeEventType;
}
