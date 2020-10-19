import { MenuItem } from 'primeng/api';

export interface DotDataTableAction {
    shouldShow?: (x?: any) => boolean;
    menuItem: MenuItem;
}
