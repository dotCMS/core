import { MenuItem } from 'primeng/api';

export interface DotActionMenuItem {
    shouldShow?: (x?: Record<string, unknown>) => boolean;
    menuItem: MenuItem;
}
