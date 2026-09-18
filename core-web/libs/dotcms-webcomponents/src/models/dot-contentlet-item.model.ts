export interface DotContentletItem {
    contentTypeIcon?: string;
    language: string;
    typeVariable: string;
    modDate: string;
    __wfstep__: string;
    title: string;
    sysPublishDate: string;
    baseType: string;
    inode: string;
    __title__: string;
    Identifier: string;
    permissions: string;
    contentStructureType: string;
    working: string;
    locked: string;
    live: string;
    owner: string;
    identifier: string;
    wfActionMapList: string;
    languageId: string;
    mediaType: string;
    statusIcons: string;
    hasLiveVersion: string;
    deleted: string;
    structureInode: string;
    __type__: string;
    __icon__: string;
    ownerCanRead: string;
    hasTitleImage: string | boolean;
    modUser: string;
    ownerCanWrite: string;
    ownerCanPublish: string;
    mimeType: string;
    titleImage: string;
    modDateMilis: number;
    icon?: string;
    /**
     * Present on some endpoints only, which is why `dot-contentlet-thumbnail` reads it
     * defensively (`contentlet['image'] || …`) when deciding between the `/dA/` and
     * `/contentAsset/` thumbnail URLs.
     */
    image?: string;
}
