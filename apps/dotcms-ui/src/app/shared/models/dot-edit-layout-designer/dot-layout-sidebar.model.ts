import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';

export interface DotLayoutSideBar {
    location?: string;
    containers?: DotPageContainer[];
    width?: string;
    widthPercent?: number;
    preview?: boolean;
}
