import { PageMode } from '../../../portlets/dot-edit-page/shared/models/page-mode.enum';

export interface DotEditPageState {
    locked?: boolean;
    mode?: PageMode;
}
