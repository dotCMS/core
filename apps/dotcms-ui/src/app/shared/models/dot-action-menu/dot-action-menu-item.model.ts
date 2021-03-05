import { MenuItem } from 'primeng/api';

export interface DotActionMenuItem {
    shouldShow?: (x?: any) => boolean;
    menuItem: MenuItem;
}
