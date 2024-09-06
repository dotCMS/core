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

export interface DotCMSContentlet {
    archived: boolean;
    binaryContentAsset?: string;
    deleted?: boolean;
    baseType: string;
    binary?: string;
    binaryVersion?: string;
    file?: string;
    contentType: string;
    hasLiveVersion?: boolean;
    folder: string;
    hasTitleImage: boolean;
    hostName: string;
    host: string;
    inode: string;
    identifier: string;
    languageId: number;
    image?: string;
    locked: boolean;
    language?: string;
    mimeType?: string;
    modUser: string;
    modDate: string;
    live: boolean;
    sortOrder: number;
    owner: string;
    title: string;
    stInode: string;
    titleImage: string;
    modUserName: string;
    text?: string;
    working: boolean;
    url: string;
    contentTypeIcon?: string;
    body?: string;
    variant?: string;
    __icon__?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    [key: string]: any; // This is a catch-all for any other custom properties that might be on the contentlet.
}
