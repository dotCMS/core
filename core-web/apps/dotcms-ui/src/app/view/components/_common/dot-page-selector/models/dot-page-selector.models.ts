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
    originalEvent: InputEvent;
    query: string;
}
