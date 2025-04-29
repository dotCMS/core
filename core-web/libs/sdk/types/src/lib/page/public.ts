/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Represents a DotCMS page asset with its associated components and metadata
 *
 * @template T - Type parameter extending an object with optional urlContentMap property
 * @interface DotCMSPageAsset
 * @property {boolean} [canCreateTemplate] - Indicates whether the current user has permissions to create a template from this page
 * @property {Object.<string, DotCMSPageAssetContainer>} containers - Collection of containers present on the page, keyed by container identifier
 * @property {DotCMSLayout} layout - Defines the structural layout of the page including rows, columns and their configurations
 * @property {DotCMSPage} page - Contains core page information such as title, URL, metadata and other page-specific properties
 * @property {DotCMSSite} site - Information about the site this page belongs to, including host name and identifier
 * @property {DotCMSTemplate} template - The template applied to this page, defining its base structure and design
 * @property {DotCMSViewAs} [viewAs] - Configuration for how the page should be rendered in different view modes (preview, edit, live)
 * @property {DotCMSVanityUrl} [vanityUrl] - Custom URL routing configuration for this page if applicable
 * @property {T['urlContentMap']} [urlContentMap] - Mapping of URL parameters to content, useful for dynamic pages
 * @property {Record<string, unknown>} [params] - Additional parameters and metadata associated with the page
 */
export interface DotCMSPageAsset<
    T extends { urlContentMap?: unknown } = { urlContentMap?: unknown }
> {
    canCreateTemplate?: boolean;
    containers: {
        [key: string]: DotCMSPageAssetContainer;
    };
    layout: DotCMSLayout;
    page: DotCMSPage;
    site: DotCMSSite;
    template: DotCMSTemplate;
    viewAs?: DotCMSViewAs;
    vanityUrl?: DotCMSVanityUrl;
    urlContentMap?: T extends { urlContentMap: infer U } ? U : T;
    params?: Record<string, unknown>;
}

/**
 * Represents a row in a page layout asset
 *
 * @interface DotPageAssetLayoutRow
 * @property {number} identifier - Unique numeric identifier for the row
 * @property {string} [value] - Optional value associated with the row
 * @property {string} [id] - Optional string identifier for the row
 * @property {DotPageAssetLayoutColumn[]} columns - Array of columns contained within this row
 * @property {string} [styleClass] - Optional CSS class name(s) to apply to the row
 */
export interface DotPageAssetLayoutRow {
    identifier: number;
    value?: string;
    id?: string;
    columns: DotPageAssetLayoutColumn[];
    styleClass?: string;
}

/**
 * Represents a vanity URL configuration for URL redirection and forwarding
 *
 * @interface DotCMSVanityUrl
 * @property {string} pattern - The URL pattern to match for this vanity URL rule
 * @property {string} vanityUrlId - Unique identifier for this vanity URL
 * @property {string} url - The actual URL that will be matched
 * @property {string} siteId - The ID of the site this vanity URL belongs to
 * @property {number} languageId - The language ID this vanity URL applies to
 * @property {string} forwardTo - The destination URL to forward/redirect to
 * @property {number} response - The HTTP response code to use
 * @property {number} order - The priority order of this vanity URL rule
 * @property {boolean} temporaryRedirect - Whether this is a temporary (302) redirect
 * @property {boolean} permanentRedirect - Whether this is a permanent (301) redirect
 * @property {boolean} forward - Whether to forward the request internally
 */
export interface DotCMSVanityUrl {
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
}

/**
 * Represents a column in a page layout asset
 *
 * @interface DotPageAssetLayoutColumn
 * @property {boolean} preview - Whether the column is in preview mode
 * @property {DotCMSColumnContainer[]} containers - Array of containers within this column
 * @property {number} widthPercent - Width of the column as a percentage
 * @property {number} width - Width of the column in pixels/units
 * @property {number} leftOffset - Left offset position of the column
 * @property {number} left - Left position of the column
 * @property {string} [styleClass] - Optional CSS class name(s) to apply to the column
 */
export interface DotPageAssetLayoutColumn {
    preview: boolean;
    containers: DotCMSColumnContainer[];
    widthPercent: number;
    width: number;
    leftOffset: number;
    left: number;
    styleClass?: string;
}

/**
 * Represents a container within a column in a page layout
 *
 * @interface DotCMSColumnContainer
 * @property {string} identifier - Unique identifier for the container
 * @property {string} uuid - UUID of the current container instance
 * @property {string[]} historyUUIDs - Array of historical UUIDs for this container's previous versions
 */
export interface DotCMSColumnContainer {
    identifier: string;
    uuid: string;
    historyUUIDs: string[];
}

/**
 * Represents a container asset within a page, including its structure and content
 *
 * @interface DotCMSPageAssetContainer
 * @property {DotCMSContainer} container - The container configuration and metadata
 * @property {DotCMSContainerStructure[]} containerStructures - Array of content type structures allowed in this container
 * @property {Object.<string, DotCMSBasicContentlet[]>} contentlets - Map of content entries in the container, keyed by UUID
 */
export interface DotCMSPageAssetContainer {
    container: DotCMSContainer;
    containerStructures: DotCMSContainerStructure[];
    contentlets: {
        [key: string]: DotCMSBasicContentlet[];
    };
}

/**
 * Represents a container in DotCMS that can hold content and has various configuration options
 *
 * @interface DotCMSContainer
 * @property {string} identifier - Unique identifier for the container
 * @property {string} uuid - UUID of the container instance
 * @property {number} iDate - Initial creation date timestamp
 * @property {string} type - Type of the container
 * @property {string} [owner] - Owner of the container
 * @property {string} inode - Unique inode identifier
 * @property {string} source - Source of the container
 * @property {string} title - Title of the container
 * @property {string} friendlyName - User-friendly name of the container
 * @property {number} modDate - Last modification date timestamp
 * @property {string} modUser - User who last modified the container
 * @property {number} sortOrder - Sort order position
 * @property {boolean} showOnMenu - Whether to show in navigation menus
 * @property {string} [code] - Optional container template code
 * @property {number} maxContentlets - Maximum number of content items allowed
 * @property {boolean} useDiv - Whether to wrap content in div elements
 * @property {string} [sortContentletsBy] - Field to sort contentlets by
 * @property {string} preLoop - Code to execute before content loop
 * @property {string} postLoop - Code to execute after content loop
 * @property {boolean} staticify - Whether to make container static
 * @property {string} [luceneQuery] - Optional Lucene query for filtering content
 * @property {string} notes - Additional notes about the container
 * @property {number} [languageId] - Language identifier
 * @property {string} [path] - Container path
 * @property {boolean} live - Whether container is live
 * @property {boolean} locked - Whether container is locked
 * @property {boolean} working - Whether container is in working state
 * @property {boolean} deleted - Whether container is deleted
 * @property {string} name - Name of the container
 * @property {boolean} archived - Whether container is archived
 * @property {string} permissionId - Permission identifier
 * @property {string} versionId - Version identifier
 * @property {string} versionType - Type of version
 * @property {string} permissionType - Type of permission
 * @property {string} categoryId - Category identifier
 * @property {number} idate - Creation date timestamp
 * @property {boolean} new - Whether container is new
 * @property {string} acceptTypes - Content types accepted by container
 * @property {DotCMSBasicContentlet[]} contentlets - Array of content items
 * @property {DotCMSSiteParentPermissionable} parentPermissionable - Parent permission configuration
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
    contentlets: DotCMSBasicContentlet[];
    parentPermissionable: DotCMSSiteParentPermissionable;
}

export interface DotCMSBasicContentlet {
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
    image?: any;
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
    widgetTitle?: string;
    onNumberOfPages?: string;
    __icon__?: string;
    [key: string]: any;
}

export interface DotcmsNavigationItem {
    code?: string;
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

interface DotCMSContainerStructure {
    id: string;
    structureId: string;
    containerInode: string;
    containerId: string;
    code: string;
    contentTypeVar: string;
}

interface DotPageAssetLayoutSidebar {
    preview: boolean;
    containers: DotCMSContainer[];
    location: string;
    widthPercent: number;
    width: string;
}

interface DotPageAssetLayoutBody {
    rows: DotPageAssetLayoutRow[];
}

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

interface DotCMSSiteContentType {
    owner?: any;
    parentPermissionable: DotCMSSiteParentPermissionable;
    permissionId: string;
    permissionType: string;
}

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
    publishDate: string;
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

/* GraphQL Page Types */

/**
 * Represents a basic page structure returned from GraphQL queries
 */
export interface DotCMSBasicGraphQLPage {
    publishDate: string;
    type: string;
    httpsRequired: boolean;
    inode: string;
    path: string;
    identifier: string;
    hasTitleImage: boolean;
    sortOrder: number;
    extension: string;
    canRead: boolean;
    pageURI: string;
    canEdit: boolean;
    archived: boolean;
    friendlyName: string;
    workingInode: string;
    url: string;
    hasLiveVersion: boolean;
    deleted: boolean;
    pageUrl: string;
    shortyWorking: string;
    mimeType: string;
    locked: boolean;
    stInode: string;
    contentType: string;
    creationDate: string;
    liveInode: string;
    name: string;
    shortyLive: string;
    modDate: string;
    title: string;
    baseType: string;
    working: boolean;
    canLock: boolean;
    live: boolean;
    isContentlet: boolean;
    statusIcons: string;

    // Language information
    conLanguage: {
        id: number;
        language: string;
        languageCode: string;
    };

    // Template information
    template: {
        drawed: boolean;
    };

    // Container information
    containers: DotCMSPageGraphQLContainer[];

    layout: DotCMSLayout;
    viewAs: DotCMSViewAs;
    urlContentMap: Record<string, unknown>;
    site: DotCMSSite;
    _map: Record<string, unknown>;
}

export interface DotCMSPageGraphQLContainer {
    path: string;
    identifier: string;
    maxContentlets?: number;
    containerStructures: DotCMSContainerStructure[];
    containerContentlets: DotCMSPageContainerContentlets[];
}

export interface DotCMSPageContainerContentlets {
    uuid: string;
    contentlets: DotCMSBasicContentlet[];
}

/**
 * Represents a GraphQL error
 * @interface DotCMSGraphQLError
 */
export interface DotCMSGraphQLError {
    message: string;
    locations: {
        line: number;
        column: number;
    }[];
    extensions: {
        classification: string;
    };
}

/**
 * Represents the complete response from a GraphQL page query
 *
 * @template TContent - The type of the content data
 * @template TNav - The type of the navigation data
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface DotCMSGraphQLPageResponse<TContent = Record<string, any>> {
    page: DotCMSBasicGraphQLPage;
    content?: TContent;
    errors?: DotCMSGraphQLError;
    graphql: {
        query: string;
        variables: Record<string, unknown>;
    };
}

/**
 * Payload for initializing the UVE
 * @interface DotCMSEditablePage
 */
export type DotCMSEditablePage = DotCMSGraphQLPageResponse | DotCMSPageAsset;
