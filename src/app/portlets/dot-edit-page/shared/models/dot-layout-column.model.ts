import { DotPageContainer } from './dot-page-container.model';

export interface DotLayoutColumn {
    containers: DotPageContainer[];
    leftOffset: number;
    width: number;
}
