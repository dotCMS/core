import { DotMenuItem } from './menu-item.model';

export interface DotMenu {
    id: string;
    name: string;
    tabDescription: string;
    tabName: string;
    url: string;
    menuItems: DotMenuItem[];
    isOpen: boolean;
}
