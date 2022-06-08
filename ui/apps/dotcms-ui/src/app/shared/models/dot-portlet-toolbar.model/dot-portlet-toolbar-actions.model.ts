import { MenuItem } from 'primeng/api';

export interface DotPortletToolbarActions {
    primary: MenuItem[];
    cancel: (event: MouseEvent) => void;
}
