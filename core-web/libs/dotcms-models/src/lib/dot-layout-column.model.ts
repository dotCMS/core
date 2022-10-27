import { DotPageContainerStructure } from './dot-container.model';

export interface DotLayoutColumn {
    containers: DotPageContainerStructure[];
    leftOffset: number;
    width: number;
    styleClass?: string;
}
