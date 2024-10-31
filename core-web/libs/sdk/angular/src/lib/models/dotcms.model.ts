/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Represents a DotCMS page asset.
 */
export interface DotCMSPageAsset {
    /**
     * Indicates if the user can create a template.
     */
    canCreateTemplate: boolean;
    /**
     * The containers of the page asset.
     */
    containers: DotCMSPageAssetContainer;
    /**
     * The layout of the page asset.
     */
    layout: DotCMSLayout;
    /**
     * The page information.
     */
    page: DotCMSPage;
    /**
     * The site information.
     */
    site: DotCMSSite;
    /**
     * The template information.
     */
    template: DotCMSTemplate;
    /**
     * The view-as information.
     */
    viewAs: DotCMSViewAs;
    /**
     * The vanity URL information.
     */
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
}

/**
 * Represents a row in the page asset layout.
 */
export interface DotPageAssetLayoutRow {
    /**
     * The identifier of the row.
     */
    identifier: number;
    /**
     * The value of the row.
     */
    value?: string;
    /**
     * The ID of the row.
     */
    id?: string;
    /**
     * The columns in the row.
     */
    columns: DotPageAssetLayoutColumn[];
    /**
     * The style class of the row.
     */
    styleClass?: string;
}

/**
 * Represents a column in the page asset layout.
 */
export interface DotPageAssetLayoutColumn {
    /**
     * Indicates if the column is in preview mode.
     */
    preview: boolean;
    /**
     * The containers in the column.
     */
    containers: DotCMSContainer[];
    /**
     * The width percentage of the column.
     */
    widthPercent: number;
    /**
     * The width of the column.
     */
    width: number;
    /**
     * The left offset of the column.
     */
    leftOffset: number;
    /**
     * The left position of the column.
     */
    left: number;
    /**
     * The style class of the column.
     */
    styleClass?: string;
}

/**
 * Represents the containers of a DotCMS page asset.
 */
export interface DotCMSPageAssetContainer {
    [key: string]: {
        /**
         * The container information.
         */
        container: DotCMSContainer;
        /**
         * The container structures.
         */
        containerStructures: DotCMSContainerStructure[];
        /**
         * The contentlets in the container.
         */
        contentlets: {
            [key: string]: DotCMSContentlet[];
        };
    };
}

/**
 * Represents a container in DotCMS.
 */
export interface DotCMSContainer {
    identifier: string;
    uuid: string;
    iDate: number;
    type: string;
    owner?: string;
    inode: string;
    source: string;
    title: string;
    friendlyName: string;
    modDate: number;
    modUser: string;
    sortOrder: number;
    showOnMenu: boolean;
    code?: string;
    maxContentlets: number;
    useDiv: boolean;
    sortContentletsBy?: string;
    preLoop: string;
    postLoop: string;
    staticify: boolean;
    luceneQuery?: string;
    notes: string;
    languageId?: number;
    path?: string;
    live: boolean;
    locked: boolean;
    working: boolean;
    deleted: boolean;
    name: string;
    archived: boolean;
    permissionId: string;
    versionId: string;
    versionType: string;
    permissionType: string;
    categoryId: string;
    idate: number;
    new: boolean;
    acceptTypes: string;
    contentlets: DotCMSContentlet[];
    parentPermissionable: DotCMSSiteParentPermissionable;
}

/**
 * Represents a contentlet in DotCMS.
 */
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
    [key: string]: any; // This is a catch-all for any other custom properties that might be on the contentlet.
}

/**
 * Represents a navigation item in DotCMS.
 */
export interface DotcmsNavigationItem {
    code?: any;
    folder: string;
    children?: DotcmsNavigationItem[];
    host: string;
    languageId: number;
    href: string;
    title: string;
    type: string;
    hash: number;
    target: string;
    order: number;
}

/**
 * Represents a template in DotCMS.
 */
interface DotCMSTemplate {
    iDate: number;
    type: string;
    owner: string;
    inode: string;
    identifier: string;
    source: string;
    title: string;
    friendlyName: string;
    modDate: number;
    modUser: string;
    sortOrder: number;
    showOnMenu: boolean;
    image: string;
    drawed: boolean;
    drawedBody: string;
    theme: string;
    anonymous: boolean;
    template: boolean;
    name: string;
    live: boolean;
    archived: boolean;
    locked: boolean;
    working: boolean;
    permissionId: string;
    versionId: string;
    versionType: string;
    deleted: boolean;
    permissionType: string;
    categoryId: string;
    idate: number;
    new: boolean;
    canEdit: boolean;
}

/**
 * Represents a page in DotCMS.
 */
interface DotCMSPage {
    template: string;
    modDate: number;
    metadata: string;
    cachettl: string;
    pageURI: string;
    title: string;
    type: string;
    showOnMenu: string;
    httpsRequired: boolean;
    inode: string;
    disabledWYSIWYG: any[];
    seokeywords: string;
    host: string;
    lastReview: number;
    working: boolean;
    locked: boolean;
    stInode: string;
    friendlyName: string;
    live: boolean;
    owner: string;
    identifier: string;
    nullProperties: any[];
    friendlyname: string;
    pagemetadata: string;
    languageId: number;
    url: string;
    seodescription: string;
    modUserName: string;
    folder: string;
    deleted: boolean;
    sortOrder: number;
    modUser: string;
    pageUrl: string;
    workingInode: string;
    shortyWorking: string;
    canEdit: boolean;
    canRead: boolean;
    canLock: boolean;
    lockedOn: number;
    lockedBy: string;
    lockedByName: string;
    liveInode: string;
    shortyLive: string;
}

/**
 * Represents the view-as information in DotCMS.
 */
interface DotCMSViewAs {
    language: {
        id: number;
        languageCode: string;
        countryCode: string;
        language: string;
        country: string;
    };
    mode: string;
}

/**
 * Represents the layout of a DotCMS page asset.
 */
interface DotCMSLayout {
    pageWidth: string;
    width: string;
    layout: string;
    title: string;
    header: boolean;
    footer: boolean;
    body: DotPageAssetLayoutBody;
    sidebar: DotPageAssetLayoutSidebar;
}

/**
 * Represents a container structure in DotCMS.
 */
interface DotCMSContainerStructure {
    id: string;
    structureId: string;
    containerInode: string;
    containerId: string;
    code: string;
    contentTypeVar: string;
}

/**
 * Represents the sidebar layout of a DotCMS page asset.
 */
interface DotPageAssetLayoutSidebar {
    preview: boolean;
    containers: DotCMSContainer[];
    location: string;
    widthPercent: number;
    width: string;
}

/**
 * Represents the body layout of a DotCMS page asset.
 */
interface DotPageAssetLayoutBody {
    rows: DotPageAssetLayoutRow[];
}

/**
 * Represents a site in DotCMS.
 */
interface DotCMSSite {
    lowIndexPriority: boolean;
    name: string;
    default: boolean;
    aliases: string;
    parent: boolean;
    tagStorage: string;
    systemHost: boolean;
    inode: string;
    versionType: string;
    structureInode: string;
    hostname: string;
    hostThumbnail?: any;
    owner: string;
    permissionId: string;
    permissionType: string;
    type: string;
    identifier: string;
    modDate: number;
    host: string;
    live: boolean;
    indexPolicy: string;
    categoryId: string;
    actionId?: any;
    new: boolean;
    archived: boolean;
    locked: boolean;
    disabledWysiwyg: any[];
    modUser: string;
    working: boolean;
    titleImage: {
        present: boolean;
    };
    folder: string;
    htmlpage: boolean;
    fileAsset: boolean;
    vanityUrl: boolean;
    keyValue: boolean;
    structure?: DotCMSSiteStructure;
    title: string;
    languageId: number;
    indexPolicyDependencies: string;
    contentTypeId: string;
    versionId: string;
    lastReview: number;
    nextReview?: any;
    reviewInterval?: any;
    sortOrder: number;
    contentType: DotCMSSiteContentType;
}

/**
 * Represents the content type of a DotCMS site.
 */
interface DotCMSSiteContentType {
    owner?: any;
    parentPermissionable: DotCMSSiteParentPermissionable;
    permissionId: string;
    permissionType: string;
}

/**
 * Represents the parent permissionable of a DotCMS site.
 */
export interface DotCMSSiteParentPermissionable {
    Inode: string;
    Identifier: string;
    permissionByIdentifier: boolean;
    type: string;
    owner?: any;
    identifier: string;
    permissionId: string;
    parentPermissionable?: any;
    permissionType: string;
    inode: string;
    childrenPermissionable?: any;
    variantId?: string;
}

/**
 * Represents the structure of a DotCMS site.
 */
interface DotCMSSiteStructure {
    iDate: number;
    type: string;
    owner?: any;
    inode: string;
    identifier: string;
    name: string;
    description: string;
    defaultStructure: boolean;
    reviewInterval?: any;
    reviewerRole?: any;
    pagedetail?: any;
    structureType: number;
    fixed: boolean;
    system: boolean;
    velocityVarName: string;
    urlMapPattern?: any;
    host: string;
    folder: string;
    publishDateVar?: any;
    expireDateVar?: any;
    modDate: number;
    fields: DotCMSSiteField[];
    widget: boolean;
    detailPage?: any;
    fieldsBySortOrder: DotCMSSiteField[];
    form: boolean;
    htmlpageAsset: boolean;
    content: boolean;
    fileAsset: boolean;
    persona: boolean;
    permissionId: string;
    permissionType: string;
    live: boolean;
    categoryId: string;
    idate: number;
    new: boolean;
    archived: boolean;
    locked: boolean;
    modUser: string;
    working: boolean;
    title: string;
    versionId: string;
    versionType: string;
}

/**
 * Represents a field in a DotCMS site structure.
 */
interface DotCMSSiteField {
    iDate: number;
    type: string;
    owner?: any;
    inode: string;
    identifier: string;
    structureInode: string;
    fieldName: string;
    fieldType: string;
    fieldRelationType?: any;
    fieldContentlet: string;
    required: boolean;
    velocityVarName: string;
    sortOrder: number;
    values?: any;
    regexCheck?: any;
    hint?: any;
    defaultValue?: any;
    indexed: boolean;
    listed: boolean;
    fixed: boolean;
    readOnly: boolean;
    searchable: boolean;
    unique: boolean;
    modDate: number;
    dataType: string;
    live: boolean;
    categoryId: string;
    idate: number;
    new: boolean;
    archived: boolean;
    locked: boolean;
    modUser: string;
    working: boolean;
    permissionId: string;
    parentPermissionable?: any;
    permissionType: string;
    title: string;
    versionId: string;
    versionType: string;
}
