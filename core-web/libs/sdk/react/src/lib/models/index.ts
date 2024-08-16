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
    baseType: string;
    deleted?: boolean;
    binary?: string;
    binaryContentAsset?: string;
    binaryVersion?: string;
    contentType: string;
    file?: string;
    folder: string;
    hasLiveVersion?: boolean;
    hasTitleImage: boolean;
    host: string;
    hostName: string;
    identifier: string;
    inode: string;
    image?: string;
    languageId: number;
    language?: string;
    live: boolean;
    locked: boolean;
    mimeType?: string;
    modDate: string;
    modUser: string;
    modUserName: string;
    owner: string;
    sortOrder: number;
    stInode: string;
    title: string;
    titleImage: string;
    text?: string;
    url: string;
    working: boolean;
    body?: string;
    contentTypeIcon?: string;
    variant?: string;
    __icon__?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    [key: string]: any; // This is a catch-all for any other custom properties that might be on the contentlet.
}
