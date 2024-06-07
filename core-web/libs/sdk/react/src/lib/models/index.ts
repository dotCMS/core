export interface ContainerData {
    [key: string]: {
        container: {
            path: string;
            identifier: string;
            maxContentlets: number;
            parentPermissionable: Record<string, string>;
        };
        containerStructures: {
            contentTypeVar: string;
        }[];
        contentlets: {
            [key: string]: {
                contentType: string;
                identifier: string;
                title: string;
                inode: string;
                onNumberOfPages: number;
                widgetTitle?: string;
                baseType: string;
            }[];
        };
    };
}

export interface DotCMSPageContext {
    /**
     * `components` is a property of the `PageProviderProps` type.
     * It is an object that maps content type variables to their corresponding React components.
     *
     * It will be use to render the contentlets in the page.
     *
     * @property {Object} components
     * @memberof PageProviderProps
     * @type {Object.<string, React.ElementType>}
     */
    components: {
        [contentTypeVariable: string]: React.ElementType;
    };
    pageAsset: {
        containers: ContainerData;
        layout: {
            header: boolean;
            footer: boolean;
            body: {
                rows: {
                    styleClass: string;
                    columns: {
                        styleClass: string;
                        width: number;
                        leftOffset: number;
                        containers: {
                            identifier: string;
                            uuid: string;
                        }[];
                    }[];
                }[];
            };
        };
        page: {
            title: string;
            identifier: string;
        };
        viewAs: {
            language: {
                id: string;
            };
            persona: {
                keyTag: string;
            };
            // variant requested
            variantId: string;
        };
    };
    isInsideEditor: boolean;
}
