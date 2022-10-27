import { DotPageContainerStructure } from './dot-container.model';

export interface DotLayoutSideBar {
    location?: string;
    containers?: DotPageContainerStructure[];
    width?: string;
    widthPercent?: number;
    preview?: boolean;
}
