import { DotPageMode } from '@portlets/dot-edit-page/shared/models/dot-page-mode.enum';

export interface DotEditPageState {
    locked?: boolean;
    mode?: DotPageMode;
}
