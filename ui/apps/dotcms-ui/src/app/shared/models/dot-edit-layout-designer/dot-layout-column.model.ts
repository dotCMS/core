import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';

export interface DotLayoutColumn {
    containers: DotPageContainer[];
    leftOffset: number;
    width: number;
    styleClass?: string;
}
