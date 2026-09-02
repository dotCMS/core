import { DotPageContainer } from './dot-page-container.model';

export interface DotLayoutColumn {
    containers: DotPageContainer[];
    leftOffset: number;
    width: number;
    styleClass?: string;
    metadata?: Record<string, unknown>;
}
