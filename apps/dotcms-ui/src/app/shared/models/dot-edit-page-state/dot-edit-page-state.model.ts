import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

export interface DotEditPageState {
    locked?: boolean;
    mode?: DotPageMode;
}
