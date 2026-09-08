import { MenuItem } from 'primeng/api';

export interface DotPortletToolbarActions {
    /** Null when the toolbar shows only a cancel button. */
    primary: MenuItem[] | null;
    cancel: (event: MouseEvent) => void;
}
