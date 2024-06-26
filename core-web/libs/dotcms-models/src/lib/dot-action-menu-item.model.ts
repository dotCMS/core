import { MenuItem } from 'primeng/api';

import { DotCMSContentType } from './dot-content-types.model';

interface CustomMenuItem extends Omit<MenuItem, 'command'> {
    command?(event: DotCMSContentType): void;
}

export interface DotActionMenuItem {
    shouldShow?: (x?: Record<string, unknown>) => boolean;
    menuItem: CustomMenuItem;
}
