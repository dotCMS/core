import { DotMenuItem } from './menu-item.model';

export interface DotMenu {
    /** @deprecated The menu resource does not send it; do not use it as state. */
    active?: boolean;
    id: string;
    /** @deprecated The menu resource does not send it; the open group is the store's `openParentMenuId`. */
    isOpen?: boolean;
    /** @deprecated The menu resource does not send it; use `tabName`. */
    label?: string;
    menuItems: DotMenuItem[];
    name: string;
    /** @deprecated The menu resource does not send it. */
    tabDescription?: string;
    tabIcon: string;
    tabName: string;
    url: string;
}
