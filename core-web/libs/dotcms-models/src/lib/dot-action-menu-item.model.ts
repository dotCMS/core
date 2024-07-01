import { MenuItem, MenuItemCommandEvent } from 'primeng/api';

export interface CustomMenuItem<T = unknown> extends Omit<MenuItem, 'command'> {
    command?(event?: T): void;
}

export interface DotActionMenuItem<T = unknown> {
    shouldShow?: (x?: Record<string, unknown>) => boolean;
    menuItem: CustomMenuItem<T>;
}

export interface DotMenuItemCommandEvent extends MenuItemCommandEvent {
    inode: string;
    categoryName: string;
}
