import { MenuItem } from 'primeng/primeng';

export interface DotDataTableAction {
    shouldShow?: (x?: any) => boolean;
    menuItem: MenuItem;
}
