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
    /**
     * Init parameters from the portlet's configuration.
     * May contain 'angular-module' for dynamic lazy loading of Angular modules,
     * or 'parent' for a translated, one-level navigation group within the layout.
     * Example: { parent: 'Identity-Access' }
     */
    initParams?: Record<string, string>;
}
