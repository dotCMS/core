import { DotMenuItem } from './menu-item.model';

export interface DotMenu {
    tabDescription: string;
    tabName: string;
    url: string;
    menuItems: DotMenuItem[];
    isOpen: boolean;
}
