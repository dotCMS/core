import { PageMode } from '../../../portlets/dot-edit-content/shared/page-mode.enum';


export interface DotEditPageState {
    locked?: boolean;
    mode?: PageMode;
}
