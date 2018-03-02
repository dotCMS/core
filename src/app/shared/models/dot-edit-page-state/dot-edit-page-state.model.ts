import { PageMode } from '../../../portlets/dot-edit-page/content/shared/page-mode.enum';

export interface DotEditPageState {
    locked?: boolean;
    mode?: PageMode;
}
