import { Site } from '@dotcms/dotcms-js';

import { DotPageAsset } from '../service/dot-page-selector.service';

export interface DotPageSelectorItem {
    label: string;
    payload: DotPageAsset | Site | DotFolder;
}

export interface DotSimpleURL {
    host: string;
    pathname: string;
}

export interface DotFolder {
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}

export interface CompleteEvent {
    // `Event`, matching PrimeNG's own `AutoCompleteCompleteEvent`; it is not narrowed to
    // `InputEvent` there and callers only read `query`.
    originalEvent: Event;
    query: string;
}
