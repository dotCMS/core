/* eslint-disable @typescript-eslint/no-explicit-any */

import { DotHttpError } from '../client/public';

/**
 * Represents a map of container identifiers to their container objects
 *
 * @interface DotCMSPageAssetContainers
 * @property {string} [key] - The identifier of the container
 * @property {DotCMSPageAssetContainer} [value] - The container object
 */
export interface DotCMSPageAssetContainers {
    [key: string]: DotCMSPageAssetContainer;
}

/**
 * Represents a DotCMS page asset that contains all the components and configuration needed to render a page
 *
 * @interface DotCMSPageAsset
 * @property {boolean} [canCreateTemplate] - Whether the current user has permission to create templates
 * @property {Object.<string, DotCMSPageAssetContainer>} containers - Map of container identifiers to their container objects
 * @property {DotCMSLayout} layout - The layout configuration for this page
 * @property {DotCMSPage} page - The page metadata and configuration
 * @property {DotCMSSite} site - The site this page belongs to
 * @property {DotCMSTemplate} template - The template used to render this page
 * @property {DotCMSViewAs} [viewAs] - Optional view configuration for preview/editing modes
 * @property {DotCMSVanityUrl} [vanityUrl] - Optional vanity URL configuration for this page
 * @property {DotCMSURLContentMap} [urlContentMap] - Optional URL to content mapping configuration
 * @property {Record<string, unknown>} [params] - Optional parameters used when requesting the page
 */
export interface DotCMSPageAsset {
    canCreateTemplate?: boolean;
    numberContents?: number;
    containers: DotCMSPageAssetContainers;
    layout: DotCMSLayout;
    page: DotCMSPage;
    site: DotCMSSite;
    template:
        | DotCMSTemplate
        | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'>;
    viewAs?: DotCMSViewAs;
    vanityUrl?: DotCMSVanityUrl;
    urlContentMap?: DotCMSURLContentMap;
    params?: Record<string, unknown>;
    runningExperimentId?: string;
}

/**
 * Represents a URL to content mapping configuration that extends the basic contentlet
 *
 * @interface DotCMSURLContentMap
 * @extends {DotCMSBasicContentlet}
 * @property {string} URL_MAP_FOR_CONTENT - The content identifier that this URL maps to
 * @property {string} urlMap - The URL pattern/mapping configuration
 */
export interface DotCMSURLContentMap extends DotCMSBasicContentlet {
    URL_MAP_FOR_CONTENT: string;
    urlMap: string;
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
    uri?: string;
    action?: number;
    siteId: string;
    languageId: number;
    forwardTo: string;
    response: number;
    order: number;
    temporaryRedirect?: boolean;
    permanentRedirect?: boolean;
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

/**
 * Represents a basic contentlet in dotCMS with common properties shared across content types
 *
 * @interface DotCMSBasicContentlet
 * @property {boolean} archived - Whether the contentlet is archived
 * @property {string} baseType - The base content type
 * @property {boolean} [deleted] - Whether the contentlet is deleted
 * @property {string} [binary] - Binary content identifier
 * @property {string} [binaryContentAsset] - Binary content asset identifier
 * @property {string} [binaryVersion] - Version of binary content
 * @property {string} contentType - The specific content type
 * @property {string} [file] - Associated file path
 * @property {string} folder - Folder path containing the contentlet
 * @property {boolean} [hasLiveVersion] - Whether a live version exists
 * @property {boolean} hasTitleImage - Whether the contentlet has a title image
 * @property {string} host - Host identifier
 * @property {string} hostName - Host name
 * @property {string} identifier - Unique identifier
 * @property {string} inode - Internal node identifier
 * @property {any} [image] - Associated image
 * @property {number} languageId - Language identifier
 * @property {string} [language] - Language name/code
 * @property {boolean} live - Whether contentlet is live
 * @property {boolean} locked - Whether contentlet is locked
 * @property {string} [mimeType] - MIME type for binary content
 * @property {string} modDate - Last modification date
 * @property {string} modUser - User who last modified
 * @property {string} modUserName - Display name of user who last modified
 * @property {string} owner - Owner of the contentlet
 * @property {string} ownerUserName - Display name of owner of the contentlet
 * @property {number} sortOrder - Sort order position
 * @property {string} stInode - Structure inode
 * @property {string} title - Title of the contentlet
 * @property {string} titleImage - Title image path/identifier
 * @property {string} [text] - Text content
 * @property {string} url - URL for the contentlet
 * @property {boolean} working - Whether contentlet is in working version
 * @property {string} [body] - Body content
 * @property {string} [contentTypeIcon] - Icon for the content type
 * @property {string} [variant] - Content variant identifier
 * @property {string} [widgetTitle] - Title for widget type content
 * @property {string} [onNumberOfPages] - Number of pages setting
 * @property {string} [__icon__] - Icon identifier
 * @property {any} [key: string] - Additional dynamic properties
 */
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
    ownerUserName?: string;
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
    url?: string;
    working: boolean;
    body?: string;
    contentTypeIcon?: string;
    variant?: string;
    widgetTitle?: string;
    onNumberOfPages?: string;
    __icon__?: string;
    _map?: Record<string, unknown>;
}

/**
 * Represents a template in DotCMS that defines the layout and structure of pages
 *
 * @interface DotCMSTemplate
 * @property {number} iDate - Initial creation date timestamp
 * @property {string} type - Type of the template
 * @property {string} owner - Owner of the template
 * @property {string} inode - Unique inode identifier
 * @property {string} identifier - Unique identifier for the template
 * @property {string} source - Source of the template
 * @property {string} title - Title of the template
 * @property {string} friendlyName - User-friendly name of the template
 * @property {number} modDate - Last modification date timestamp
 * @property {string} modUser - User who last modified the template
 * @property {number} sortOrder - Sort order position
 * @property {boolean} showOnMenu - Whether to show in navigation menus
 * @property {string} image - Image associated with the template
 * @property {boolean} drawed - Whether template was drawn in template designer
 * @property {string} drawedBody - Template body from designer
 * @property {string} theme - Theme applied to the template
 * @property {boolean} anonymous - Whether template is accessible anonymously
 * @property {boolean} template - Whether this is a template
 * @property {string} name - Name of the template
 * @property {boolean} live - Whether template is live
 * @property {boolean} archived - Whether template is archived
 * @property {boolean} locked - Whether template is locked
 * @property {boolean} working - Whether template is in working state
 * @property {string} permissionId - Permission identifier
 * @property {string} versionId - Version identifier
 * @property {string} versionType - Type of version
 * @property {boolean} deleted - Whether template is deleted
 * @property {string} permissionType - Type of permission
 * @property {string} categoryId - Category identifier
 * @property {number} idate - Creation date timestamp
 * @property {boolean} new - Whether template is new
 * @property {boolean} canEdit - Whether current user can edit template
 */
export interface DotCMSTemplate {
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
 * Represents a page in DotCMS with its metadata, permissions and configuration
 *
 * @interface DotCMSPage
 * @property {string} template - Template identifier used by this page
 * @property {number} modDate - Last modification date timestamp
 * @property {string} metadata - Page metadata
 * @property {string} cachettl - Cache time to live configuration
 * @property {string} pageURI - URI path of the page
 * @property {string} title - Page title
 * @property {string} type - Type of page
 * @property {string} showOnMenu - Menu display configuration
 * @property {boolean} httpsRequired - Whether HTTPS is required
 * @property {string} inode - Unique inode identifier
 * @property {any[]} disabledWYSIWYG - Disabled WYSIWYG editors
 * @property {string} seokeywords - SEO keywords
 * @property {string} host - Host identifier
 * @property {number} lastReview - Last review date timestamp
 * @property {boolean} working - Whether page is in working state
 * @property {boolean} locked - Whether page is locked
 * @property {string} stInode - Structure inode identifier
 * @property {string} friendlyName - User-friendly name
 * @property {boolean} live - Whether page is live
 * @property {string} owner - Page owner
 * @property {string} identifier - Unique identifier
 * @property {any[]} nullProperties - Properties with null values
 * @property {string} friendlyname - Alternative friendly name
 * @property {string} pagemetadata - Additional page metadata
 * @property {number} languageId - Language identifier
 * @property {string} url - Page URL
 * @property {string} seodescription - SEO description
 * @property {string} modUserName - Name of user who last modified
 * @property {string} folder - Folder path
 * @property {boolean} deleted - Whether page is deleted
 * @property {number} sortOrder - Sort order position
 * @property {string} modUser - User who last modified
 * @property {string} pageUrl - Full page URL
 * @property {string} workingInode - Working version inode
 * @property {string} shortyWorking - Short working version ID
 * @property {boolean} canEdit - Whether current user can edit
 * @property {boolean} canRead - Whether current user can read
 * @property {boolean} canLock - Whether current user can lock
 * @property {number} lockedOn - Lock timestamp
 * @property {string} lockedBy - User who locked the page
 * @property {string} lockedByName - Name of user who locked
 * @property {string} liveInode - Live version inode
 * @property {string} shortyLive - Short live version ID
 */
export interface DotCMSPage {
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
    rendered?: string;
    contentType: string;
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
    canSeeRules?: boolean;
}

/**
 * Represents the language configuration for a DotCMS page
 *
 * @export
 * @interface DotCMSViewAsLanguage
 * @property {number} id - Unique identifier for the language
 * @property {string} languageCode - ISO 639-1 language code (e.g. 'en', 'es')
 * @property {string} countryCode - ISO 3166-1 country code (e.g. 'US', 'ES')
 * @property {string} language - Full name of the language (e.g. 'English', 'Spanish')
 * @property {string} country - Full name of the country (e.g. 'United States', 'Spain')
 */
export interface DotCMSViewAsLanguage {
    id: number;
    languageCode: string;
    countryCode: string;
    language: string;
    country: string;
}

/**
 * Represents the persona configuration for a DotCMS page
 *
 * @export
 * @interface DotCMSViewAsPersona
 * @property {number} modDate - Last modification date timestamp
 * @property {string} inode - Unique inode identifier
 * @property {string} name - Name of the persona
 * @property {string} identifier - Unique identifier
 * @property {string} keyTag - Key tag for the persona
 * @property {string} photo - Photo of the persona
 */
export interface DotCMSViewAsPersona extends DotCMSBasicContentlet {
    name: string;
    keyTag: string;
    personalized?: boolean;
    photo?: {
        versionPath: string;
    };
}

/**
 * Represents view configuration settings for preview/editing modes
 *
 * @interface DotCMSViewAs
 * @property {Object} language - Language configuration for the view
 * @property {number} language.id - Unique identifier for the language
 * @property {string} language.languageCode - ISO 639-1 language code (e.g. 'en', 'es')
 * @property {string} language.countryCode - ISO 3166-1 country code (e.g. 'US', 'ES')
 * @property {string} language.language - Full name of the language (e.g. 'English', 'Spanish')
 * @property {string} language.country - Full name of the country (e.g. 'United States', 'Spain')
 * @property {string} mode - View mode for the page ('PREVIEW_MODE' | 'EDIT_MODE' | 'LIVE')
 */
export interface DotCMSViewAs {
    language: DotCMSViewAsLanguage;
    mode: string;
    persona?: DotCMSViewAsPersona;
    variantId?: string;
}

/**
 * Represents the layout configuration for a DotCMS page
 *
 * @interface DotCMSLayout
 * @property {string} pageWidth - The overall width of the page
 * @property {string} width - The width of the main content area
 * @property {string} layout - The layout template/configuration identifier
 * @property {string} title - The title of the layout
 * @property {boolean} header - Whether the layout includes a header section
 * @property {boolean} footer - Whether the layout includes a footer section
 * @property {DotPageAssetLayoutBody} body - The main content body configuration
 * @property {DotPageAssetLayoutSidebar} sidebar - The sidebar configuration
 */
export interface DotCMSLayout {
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
 * Represents the structure configuration for a DotCMS container
 *
 * @interface DotCMSContainerStructure
 * @property {string} id - Unique identifier for the container structure
 * @property {string} structureId - ID of the content structure/type
 * @property {string} containerInode - Inode of the container
 * @property {string} containerId - ID of the container
 * @property {string} code - Template code for rendering the structure
 * @property {string} contentTypeVar - Variable name of the content type
 */
export interface DotCMSContainerStructure {
    id: string;
    structureId: string;
    containerInode: string;
    containerId: string;
    code: string;
    contentTypeVar: string;
}

/**
 * Represents the sidebar configuration for a DotCMS page layout
 *
 * @interface DotPageAssetLayoutSidebar
 * @property {boolean} preview - Whether the sidebar is in preview mode
 * @property {DotCMSContainer[]} containers - Array of containers placed in the sidebar
 * @property {string} location - Position/location of the sidebar
 * @property {number} widthPercent - Width of the sidebar as a percentage
 * @property {string} width - Width of the sidebar (CSS value)
 */
interface DotPageAssetLayoutSidebar {
    preview: boolean;
    containers: DotCMSContainer[];
    location: string;
    widthPercent: number;
    width: string;
}

/**
 * Represents the body section of a DotCMS page layout
 *
 * @interface DotPageAssetLayoutBody
 * @property {DotPageAssetLayoutRow[]} rows - Array of layout rows that make up the body content
 */
interface DotPageAssetLayoutBody {
    rows: DotPageAssetLayoutRow[];
}

/**
 * Represents a DotCMS site/host with its configuration and metadata
 *
 * @interface DotCMSSite
 * @property {boolean} lowIndexPriority - Whether this site has low priority for indexing
 * @property {string} name - Name of the site
 * @property {boolean} default - Whether this is the default site
 * @property {string} aliases - Comma-separated list of domain aliases
 * @property {boolean} parent - Whether this site is a parent site
 * @property {string} tagStorage - Location for storing tags
 * @property {boolean} systemHost - Whether this is a system host
 * @property {string} inode - Unique inode identifier
 * @property {string} versionType - Type of version
 * @property {string} structureInode - Structure inode reference
 * @property {string} hostname - Primary hostname
 * @property {any} [hostThumbnail] - Optional thumbnail image
 * @property {string} owner - Owner of the site
 * @property {string} permissionId - Permission identifier
 * @property {string} permissionType - Type of permission
 * @property {string} type - Type of site
 * @property {string} identifier - Unique identifier
 * @property {number} modDate - Last modification date timestamp
 * @property {string} host - Host identifier
 * @property {boolean} live - Whether site is live
 * @property {string} indexPolicy - Indexing policy configuration
 * @property {string} categoryId - Category identifier
 * @property {any} [actionId] - Optional action identifier
 * @property {boolean} new - Whether site is new
 * @property {boolean} archived - Whether site is archived
 * @property {boolean} locked - Whether site is locked
 * @property {any[]} disabledWysiwyg - Array of disabled WYSIWYG editors
 * @property {string} modUser - User who last modified the site
 * @property {boolean} working - Whether site is in working state
 * @property {Object} titleImage - Title image configuration
 * @property {boolean} titleImage.present - Whether title image exists
 * @property {string} folder - Folder path
 * @property {boolean} htmlpage - Whether site contains HTML pages
 * @property {boolean} fileAsset - Whether site contains file assets
 * @property {boolean} vanityUrl - Whether site uses vanity URLs
 * @property {boolean} keyValue - Whether site uses key-value pairs
 * @property {DotCMSSiteStructure} [structure] - Optional site structure configuration
 * @property {string} title - Title of the site
 * @property {number} languageId - Language identifier
 * @property {string} indexPolicyDependencies - Index policy for dependencies
 * @property {string} contentTypeId - Content type identifier
 * @property {string} versionId - Version identifier
 * @property {number} lastReview - Last review timestamp
 * @property {any} [nextReview] - Optional next review date
 * @property {any} [reviewInterval] - Optional review interval
 * @property {number} sortOrder - Sort order position
 * @property {DotCMSSiteContentType} contentType - Content type configuration
 */
export interface DotCMSSite {
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
 * Represents a content type configuration for a DotCMS site
 *
 * @interface DotCMSSiteContentType
 * @property {any} [owner] - Optional owner of the content type
 * @property {DotCMSSiteParentPermissionable} parentPermissionable - Parent permission configuration
 * @property {string} permissionId - Permission identifier
 * @property {string} permissionType - Type of permission
 */
interface DotCMSSiteContentType {
    owner?: any;
    parentPermissionable: DotCMSSiteParentPermissionable;
    permissionId: string;
    permissionType: string;
}

/**
 * Represents parent permissionable configuration for a DotCMS site
 *
 * @interface DotCMSSiteParentPermissionable
 * @property {string} Inode - The inode identifier (legacy casing)
 * @property {string} Identifier - The identifier (legacy casing)
 * @property {boolean} permissionByIdentifier - Whether permissions are managed by identifier
 * @property {string} type - The type of the permissionable
 * @property {any} [owner] - Optional owner of the permissionable
 * @property {string} identifier - The identifier (modern casing)
 * @property {string} permissionId - Permission identifier
 * @property {any} [parentPermissionable] - Optional parent permissionable reference
 * @property {string} permissionType - Type of permission
 * @property {string} inode - The inode identifier (modern casing)
 * @property {any} [childrenPermissionable] - Optional children permissionable references
 * @property {string} [variantId] - Optional variant identifier
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
 * Represents a content structure/type definition in DotCMS
 *
 * @interface DotCMSSiteStructure
 * @property {number} iDate - Initial creation date timestamp
 * @property {string} type - Type of the structure
 * @property {any} [owner] - Optional owner of the structure
 * @property {string} inode - Unique inode identifier
 * @property {string} identifier - Unique identifier
 * @property {string} name - Name of the structure
 * @property {string} description - Description of the structure
 * @property {boolean} defaultStructure - Whether this is a default structure
 * @property {any} [reviewInterval] - Optional review interval configuration
 * @property {any} [reviewerRole] - Optional reviewer role configuration
 * @property {any} [pagedetail] - Optional page detail configuration
 * @property {number} structureType - Type identifier for the structure
 * @property {boolean} fixed - Whether structure is fixed/immutable
 * @property {boolean} system - Whether this is a system structure
 * @property {string} velocityVarName - Velocity variable name
 * @property {any} [urlMapPattern] - Optional URL mapping pattern
 * @property {string} host - Host identifier
 * @property {string} folder - Folder path
 * @property {string} publishDate - Publication date
 * @property {any} [publishDateVar] - Optional publish date variable
 * @property {any} [expireDateVar] - Optional expiration date variable
 * @property {number} modDate - Last modification date timestamp
 * @property {DotCMSSiteField[]} fields - Array of field definitions
 * @property {boolean} widget - Whether structure is a widget
 * @property {any} [detailPage] - Optional detail page configuration
 * @property {DotCMSSiteField[]} fieldsBySortOrder - Fields sorted by order
 * @property {boolean} form - Whether structure is a form
 * @property {boolean} htmlpageAsset - Whether structure is an HTML page asset
 * @property {boolean} content - Whether structure is content
 * @property {boolean} fileAsset - Whether structure is a file asset
 * @property {boolean} persona - Whether structure is a persona
 * @property {string} permissionId - Permission identifier
 * @property {string} permissionType - Type of permission
 * @property {boolean} live - Whether structure is live
 * @property {string} categoryId - Category identifier
 * @property {number} idate - Creation date timestamp
 * @property {boolean} new - Whether structure is new
 * @property {boolean} archived - Whether structure is archived
 * @property {boolean} locked - Whether structure is locked
 * @property {string} modUser - User who last modified
 * @property {boolean} working - Whether structure is in working state
 * @property {string} title - Title of the structure
 * @property {string} versionId - Version identifier
 * @property {string} versionType - Type of version
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

/**
 * Represents a field in a DotCMS site structure/content type
 *
 * @interface DotCMSSiteField
 * @property {number} iDate - Initial creation date timestamp
 * @property {string} type - Type of the field
 * @property {any} [owner] - Owner of the field
 * @property {string} inode - Unique inode identifier
 * @property {string} identifier - Unique identifier
 * @property {string} structureInode - Inode of the parent structure/content type
 * @property {string} fieldName - Name of the field
 * @property {string} fieldType - Type of field (text, textarea, etc)
 * @property {any} [fieldRelationType] - Type of relationship if field is relational
 * @property {string} fieldContentlet - Contentlet field mapping
 * @property {boolean} required - Whether field is required
 * @property {string} velocityVarName - Velocity variable name
 * @property {number} sortOrder - Sort order position
 * @property {any} [values] - Possible field values
 * @property {any} [regexCheck] - Regular expression validation
 * @property {any} [hint] - Help text/hint
 * @property {any} [defaultValue] - Default value
 * @property {boolean} indexed - Whether field is indexed
 * @property {boolean} listed - Whether field appears in listings
 * @property {boolean} fixed - Whether field is fixed/immutable
 * @property {boolean} readOnly - Whether field is read-only
 * @property {boolean} searchable - Whether field is searchable
 * @property {boolean} unique - Whether field must be unique
 * @property {number} modDate - Last modification date timestamp
 * @property {string} dataType - Data type of the field
 * @property {boolean} live - Whether field is live
 * @property {string} categoryId - Category identifier
 * @property {number} idate - Creation date timestamp
 * @property {boolean} new - Whether field is new
 * @property {boolean} archived - Whether field is archived
 * @property {boolean} locked - Whether field is locked
 * @property {string} modUser - User who last modified
 * @property {boolean} working - Whether field is in working state
 * @property {string} permissionId - Permission identifier
 * @property {any} [parentPermissionable] - Parent permission configuration
 * @property {string} permissionType - Type of permission
 * @property {string} title - Title of the field
 * @property {string} versionId - Version identifier
 * @property {string} versionType - Type of version
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

/**
 * Represents a basic page object from the GraphQL API
 *
 * @interface DotCMSGraphQLPage
 * @property {string} publishDate - The date the page was published
 * @property {string} type - The type of the page
 * @property {boolean} httpsRequired - Whether HTTPS is required to access the page
 * @property {string} inode - Unique inode identifier
 * @property {string} path - The path/URL of the page
 * @property {string} identifier - Unique identifier for the page
 * @property {boolean} hasTitleImage - Whether the page has a title image
 * @property {number} sortOrder - Sort order position
 * @property {string} extension - File extension
 * @property {boolean} canRead - Whether current user can read the page
 * @property {string} pageURI - URI of the page
 * @property {boolean} canEdit - Whether current user can edit the page
 * @property {boolean} archived - Whether page is archived
 * @property {string} friendlyName - User-friendly name
 * @property {string} workingInode - Working version inode
 * @property {string} url - URL of the page
 * @property {boolean} hasLiveVersion - Whether page has a live version
 * @property {boolean} deleted - Whether page is deleted
 * @property {string} pageUrl - URL of the page
 * @property {string} shortyWorking - Short identifier for working version
 * @property {string} mimeType - MIME type of the page
 * @property {boolean} locked - Whether page is locked
 * @property {string} stInode - Structure inode
 * @property {string} contentType - Content type
 * @property {string} creationDate - Date page was created
 * @property {string} liveInode - Live version inode
 * @property {string} name - Name of the page
 * @property {string} shortyLive - Short identifier for live version
 * @property {string} modDate - Last modification date
 * @property {string} title - Title of the page
 * @property {string} baseType - Base content type
 * @property {boolean} working - Whether page is in working state
 * @property {boolean} canLock - Whether current user can lock the page
 * @property {boolean} live - Whether page is live
 * @property {boolean} isContentlet - Whether page is a contentlet
 * @property {string} statusIcons - Status icons
 * @property {Object} conLanguage - Language information
 * @property {number} conLanguage.id - Language ID
 * @property {string} conLanguage.language - Language name
 * @property {string} conLanguage.languageCode - Language code
 * @property {Object} template - Template information
 * @property {boolean} template.drawed - Whether template is drawn
 * @property {DotCMSPageContainer[]} containers - Array of containers on the page
 * @property {DotCMSLayout} layout - Layout configuration
 * @property {DotCMSViewAs} viewAs - View configuration
 * @property {DotCMSURLContentMap} urlContentMap - URL to content mapping
 * @property {Record<string, unknown>} host - Site information
 * @property {Record<string, unknown>} vanityUrl - Vanity URL information
 * @property {Record<string, unknown>} _map - Additional mapping data
 */
export interface DotCMSGraphQLPage {
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
    runningExperimentId?: string;
    canSeeRules?: boolean;
    // Language information
    conLanguage: {
        id: number;
        language: string;
        languageCode: string;
    };
    // Template information
    template: Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'>;
    // Container information
    containers: DotCMSGraphQLPageContainer[];
    layout: DotCMSLayout;
    viewAs: DotCMSViewAs;
    urlContentMap: {
        _map: DotCMSURLContentMap;
    };
    host: DotCMSSite;
    vanityUrl: DotCMSVanityUrl;
    _map: Record<string, unknown>;
}

/**
 * Represents a container in a page
 *
 * @interface DotCMSGraphQLPageContainer
 * @property {string} path - The path/location of the container in the page
 * @property {string} identifier - Unique identifier for the container
 * @property {number} [maxContentlets] - Optional maximum number of content items allowed in container
 * @property {DotCMSContainerStructure[]} containerStructures - Array of content type structures allowed in container
 * @property {DotCMSPageContainerContentlets[]} containerContentlets - Array of content items in container
 */
export interface DotCMSGraphQLPageContainer {
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
 * dotCMS's GraphQL API response with a page and content query
 */
export interface DotGraphQLApiResponse {
    data: {
        page: DotCMSGraphQLPage;
        content?: Record<string, unknown>;
    };
    errors?: DotCMSGraphQLError[];
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
 * Represents the complete response from a page query
 */
export interface DotCMSPageResponse {
    pageAsset: DotCMSPageAsset;
    content?: Record<string, unknown> | unknown;
    error?: DotCMSGraphQLError;
    graphql: {
        query: string;
        variables: Record<string, unknown>;
    };
}

// Pick only the page and content properties to be able to extend these properties, they are optional
export type DotCMSExtendedPageResponse = Partial<Pick<DotCMSPageResponse, 'pageAsset' | 'content'>>;

// Compose the page with the extended properties
export type DotCMSComposedPageAsset<T extends DotCMSExtendedPageResponse> =
    T['pageAsset'] extends DotCMSPageResponse['pageAsset']
        ? DotCMSPageResponse['pageAsset'] & T['pageAsset']
        : DotCMSPageResponse['pageAsset'];

// Compose the content with the extended properties
export type DotCMSComposedContent<T extends Pick<DotCMSPageResponse, 'content'>> =
    T['content'] extends undefined ? DotCMSPageResponse['content'] : T['content'];

// Compose the page response with the extended properties
export type DotCMSComposedPageResponse<T extends DotCMSExtendedPageResponse> = Omit<
    DotCMSPageResponse,
    'pageAsset' | 'content'
> & {
    pageAsset: DotCMSComposedPageAsset<T>;
    content?: DotCMSComposedContent<T>;
};

// Compose the client get response with the extended properties
export type DotCMSClientPageGetResponse<T extends DotCMSExtendedPageResponse> = Promise<
    DotCMSComposedPageResponse<T>
>;

/**
 * Page API specific error class
 * Wraps HTTP errors and adds page-specific context including GraphQL information
 */
export class DotErrorPage extends Error {
    public readonly httpError?: DotHttpError;
    public readonly graphql?: {
        query: string;
        variables: Record<string, unknown>;
    };

    constructor(
        message: string,
        httpError?: DotHttpError,
        graphql?: { query: string; variables: Record<string, unknown> }
    ) {
        super(message);
        this.name = 'DotCMSPageError';
        this.httpError = httpError;
        this.graphql = graphql;

        // Ensure proper prototype chain for instanceof checks
        Object.setPrototypeOf(this, DotErrorPage.prototype);
    }

    /**
     * Serializes the error to a plain object for logging or transmission
     */
    toJSON() {
        return {
            name: this.name,
            message: this.message,
            httpError: this.httpError?.toJSON(),
            graphql: this.graphql,
            stack: this.stack
        };
    }
}
