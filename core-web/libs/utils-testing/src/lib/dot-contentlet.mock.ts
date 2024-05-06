import { DotCMSContentlet, StructureType, StructureTypeView } from '@dotcms/dotcms-models';

export const mockDotContentlet: StructureTypeView[] = [
    {
        name: 'CONTENT',
        label: 'Content',
        types: [
            {
                type: StructureType.CONTENT,
                name: 'Banner',
                inode: '4c441ada-944a-43af-a653-9bb4f3f0cb2b',
                action: '1',
                variable: 'Banner'
            },
            {
                type: StructureType.CONTENT,
                name: 'Blog',
                inode: '799f176a-d32e-4844-a07c-1b5fcd107578',
                action: '2',
                variable: 'Blog'
            }
        ]
    },
    {
        name: 'WIDGET',
        label: 'Widget',
        types: [
            {
                type: StructureType.WIDGET,
                name: 'Document Listing',
                inode: '4316185e-a95c-4464-8884-3b6523f694e9',
                action: '3',
                variable: 'DocumentListing'
            }
        ]
    },
    {
        name: 'RECENT_CONTENT',
        label: 'Recent Content',
        types: [
            {
                type: StructureType.CONTENT,
                name: 'Content (Generic)',
                inode: '2a3e91e4-fbbf-4876-8c5b-2233c1739b05',
                action: '4',
                variable: 'webPageContent'
            }
        ]
    }
];

export const EMPTY_CONTENTLET: DotCMSContentlet = {
    inode: '14dd5ad9-55ae-42a8-a5a7-e259b6d0901a',
    variantId: 'DEFAULT',
    locked: false,
    stInode: 'd5ea385d-32ee-4f35-8172-d37f58d9cd7a',
    contentType: 'Image',
    height: 4000,
    identifier: '93ca45e0-06d2-4eef-be1d-79bd6bf0fc99',
    hasTitleImage: true,
    sortOrder: 0,
    hostName: 'demo.dotcms.com',
    extension: 'jpg',
    isContent: true,
    baseType: 'FILEASSETS',
    archived: false,
    working: true,
    live: true,
    isContentlet: true,
    languageId: 1,
    titleImage: 'fileAsset',
    hasLiveVersion: true,
    deleted: false,
    folder: '',
    host: '',
    modDate: '',
    modUser: '',
    modUserName: '',
    owner: '',
    title: '',
    url: '',
    contentTypeIcon: 'assessment',
    __icon__: 'Icon'
};

export const URL_MAP_CONTENTLET: DotCMSContentlet = {
    URL_MAP_FOR_CONTENT: '/blog/post/french-polynesia-everything-you-need-to-know',
    archived: false,
    baseType: 'CONTENT',
    blogContent: {
        content: [],
        type: 'doc'
    },
    contentType: 'Blog',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: true,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    identifier: '2b100ac7-07b1-48c6-8270-dc01ff958c69',
    image: '/dA/2b100ac7-07b1-48c6-8270-dc01ff958c69/image/bora-bora-french-polynesia.jpeg',
    imageContentAsset: '2b100ac7-07b1-48c6-8270-dc01ff958c69/image',
    imageMetaData: {
        contentType: 'image/jpeg',
        editableAsText: false,
        fileSize: 1408137,
        height: 643,
        isImage: true,
        length: 1408137,
        modDate: 1709812933296,
        name: 'bora-bora-french-polynesia.jpeg',
        sha256: '56bea715367210eeddbe9ed7774780dae333c88853611911d7e963bde5a075a2',
        title: 'bora-bora-french-polynesia.jpeg',
        version: 20220201,
        width: 969
    },
    imageVersion: '/dA/d9170f0d-9b91-43b5-b883-7eaaef3c8ee6/image/bora-bora-french-polynesia.jpeg',
    inode: 'd9170f0d-9b91-43b5-b883-7eaaef3c8ee6',
    languageId: 1,
    live: true,
    locked: false,
    metaDescription:
        'Few to no other place in the world has a more exotic feel to its name than what French Polynesia has.',
    modDate: '1687551926486',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    ogDescription: '',
    ogImage: '6b32db9ff821e832417d6b22f4073400',
    ogTitle: 'French Polynesia Everything You Need to Know',
    ogType: 'article',
    owner: 'dotcms.org.1',
    pageTitle: 'French Polynesia Everything You Need to Know',
    postingDate: 1568930340000,
    publishDate: 1687551926586,
    searchEngineIndex: 'index,follow,snippet',
    sitemap: 'true',
    sitemapImportance: '0.5',
    sortOrder: 0,
    stInode: '799f176a-d32e-4844-a07c-1b5fcd107578',
    tags: 'vaction,french polynesia,waterenthusiast',
    teaser: 'Few to no other place in the world has a more exotic feel to its name than what French Polynesia has.',
    title: 'French Polynesia Everything You Need to Know',
    titleImage: 'image',
    url: '/content.ec5c6e2f-4266-4ff8-adfc-22f76ba453b7',
    urlMap: '/blog/post/french-polynesia-everything-you-need-to-know',
    urlTitle: 'french-polynesia-everything-you-need-to-know',
    working: true
};

export const EMPTY_IMAGE_CONTENTLET: DotCMSContentlet = {
    mimeType: 'image/jpeg',
    type: 'file_asset',
    fileAssetVersion: 'https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg',
    fileAsset: 'https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg',
    ...EMPTY_CONTENTLET
};
