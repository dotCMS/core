import { DotPageAsset } from '../service/dot-page-selector.service';
import { Site } from '@dotcms/dotcms-js';

export interface DotPageSeletorItem {
    label: string;
    payload: DotPageAsset | Site;
}

export interface DotPageSelectorResults {
    data: DotPageSeletorItem[];
    type: string;
    query: string;
}

export interface DotSimpleURL {
    host: string;
    pathname: string;
}
