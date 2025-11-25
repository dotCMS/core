export interface DotMenuItem {
    active: boolean;
    ajax: boolean;
    angular: boolean;
    id: string;
    label: string;
    url: string;
    menuLink: string;
    /**
     * @deprecated Use parentMenuLabel in MenuItemEntity instead. This property is redundant and will be removed in future versions.
     */
    labelParent?: string;
    parentMenuId: string;
}
