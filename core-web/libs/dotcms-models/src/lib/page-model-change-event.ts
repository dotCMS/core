import { DotPageContainer } from './dot-page-container.model';
import { PageModelChangeEventType } from './page-model-change-event.type';

export interface PageModelChangeEvent {
    model: DotPageContainer[];
    type: PageModelChangeEventType;
}
