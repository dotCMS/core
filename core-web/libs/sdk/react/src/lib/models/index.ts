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
        vanityUrl?: {
            pattern: string;
            vanityUrlId: string;
            url: string;
            siteId: string;
            languageId: number;
            forwardTo: string;
            response: number;
            order: number;
            temporaryRedirect: boolean;
            permanentRedirect: boolean;
            forward: boolean;
        };
    };
    isInsideEditor: boolean;
}

export interface ContentNode<T = Record<string, string>> {
    type: string;
    content: ContentNode[];
    attrs?: T;
    marks?: Mark[];
    text?: string;
}

export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export interface DotContentProps {
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    working: boolean;
    locked: boolean;
    contentType: string;
    live: boolean;
    identifier: string;
    image: string;
    imageContentAsset: string;
    urlTitle: string;
    url: string;
    titleImage: string;
    urlMap: string;
    hasLiveVersion: boolean;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    language: string;
    description: string;
    shortDescription: string;
    salePrice: string;
    retailPrice: string;
    mimeType: string;
}

export interface BlockProps {
    children: React.ReactNode;
}
