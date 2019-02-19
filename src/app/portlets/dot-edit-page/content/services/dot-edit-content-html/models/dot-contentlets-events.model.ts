import { DotPageContent } from '@portlets/dot-edit-page/shared/models/dot-page-content.model';
import { DotPageContainer } from '@portlets/dot-edit-page/shared/models/dot-page-container.model';

export interface DotContentletEvent {
    name: string;
}

export interface DotContentletEventRelocate extends DotContentletEvent {
    data: DotRelocatePayload;
}

export interface DotContentletEventSelect extends DotContentletEvent {
    data: DotPageContent;
}

export interface DotContentletEventSave extends DotContentletEvent {
    data: DotPageContent;
}

export interface DotRelocatePayload {
    container: DotPageContainer;
    contentlet: DotContentletPayload;
}

interface DotContentletPayload {
    identifier: string;
    inode: string;
}
