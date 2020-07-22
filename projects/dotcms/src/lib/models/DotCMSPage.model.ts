import { DotCMSSite } from './DotCMSSite.model';

export interface DotCMSPageParams {
    url: string;
    language?: string;
}

export interface DotCMSPageAsset {
    canCreateTemplate: boolean;
    containers: DotCMSPageAssetContainer;
    layout: DotCMSLayout;
    page: DotCMSPage;
    site: DotCMSSite;
    template: DotCMSTemplate;
    viewAs: DotCMSViewAs;
}

export interface DotCMSViewAs {
    language: {
        id: number;
        languageCode: string;
        countryCode: string;
        language: string;
        country: string;
    };
    mode: string;
}

export interface DotCMSPageAssetContainer {
    [key: string]: {
        container: DotCMSContainer;
        containerStructures: DotCMSContainerStructure[];
        contentlets: {
            [key: string]: DotCMSContentlet[];
        };
    };
}

export interface DotCMSContainerStructure {
    id: string;
    structureId: string;
    containerInode: string;
    containerId: string;
    code: string;
    contentTypeVar: string;
}

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

export interface DotPageAssetLayoutSidebar {
    preview: boolean;
    containers: DotCMSContainer[];
    location: string;
    widthPercent: number;
    width: string;
}

export interface DotPageAssetLayoutBody {
    rows: DotPageAssetLayoutRow[];
}

export interface DotPageAssetLayoutRow {
    identifier: number;
    value?: string;
    id?: string;
    columns: DotPageAssetLayoutColumn[];
    styleClass?: string;
}

export interface DotPageAssetLayoutColumn {
    preview: boolean;
    containers: DotCMSContainer[];
    widthPercent: number;
    width: number;
    leftOffset: number;
    left: number;
    styleClass?: string;
}

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
}

export interface DotCMSContentlet {
    owner: string;
    identifier: string;
    hostName: string;
    modDate: number;
    languageId: number;
    body?: string;
    title: string;
    url: string;
    baseType: string;
    inode: string;
    titleImage: string;
    modUserName: string;
    archived: boolean;
    folder: string;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    host: string;
    working: boolean;
    locked: boolean;
    stInode: string;
    contentType: string;
    live: boolean;
    code?: string;
    widgetTitle?: string;
    endDate?: number;
    description?: string;
    recurrenceOccurs?: string;
    recurrenceDayOfWeek?: number;
    recurrenceDayOfMonth?: number;
    recurrenceStart?: number;
    recurrenceWeekOfMonth?: number;
    recurs?: boolean;
    noRecurrenceEnd?: boolean;
    urlTitle?: string;
    URL_MAP_FOR_CONTENT?: string;
    recurrenceDaysOfWeek?: string;
    recurrenceInterval?: number;
    urlMap?: string;
    recurrenceDatesToIgnore?: string;
    recurrenceMonthOfYear?: number;
    startDate?: number;
    recurrenceEnd?: number;
    categories?: string;
    image?: string;
    tags?: string;
}

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
