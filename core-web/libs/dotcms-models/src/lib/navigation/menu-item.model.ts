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
     * The Angular module path for dynamic lazy loading.
     * When set, the frontend can dynamically import and register this portlet's Angular module at runtime.
     * Example: "@dotcms/portlets/my-custom"
     */
    angularModule?: string;
}
