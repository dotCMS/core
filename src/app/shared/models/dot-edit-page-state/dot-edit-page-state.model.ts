import { PageMode } from '../../../portlets/dot-edit-content/components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';

export interface DotEditPageState {
    locked?: boolean;
    mode?: PageMode;
}
