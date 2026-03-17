import { faker } from '@faker-js/faker';

import { DotCMSContentlet, StructureType, StructureTypeView } from '@dotcms/dotcms-models';

import { createFakeLanguage } from './dot-language.mock';

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

/**
 * Creates a fake contentlet with optional overrides. This function generates a contentlet
 * with predefined fake data that can be overridden by passing specific properties.
 *
 * @param {Partial<DotCMSContentlet>} overrides - Optional overrides for default contentlet properties.
 * @return {DotCMSContentlet} - The fake contentlet with applied overrides.
 */
export function createFakeContentlet(overrides: Partial<DotCMSContentlet> = {}): DotCMSContentlet {
    const language = createFakeLanguage();

    const defaultContentlet: DotCMSContentlet = {
        id: faker.string.uuid(),
        title: faker.lorem.sentence(),
        language: language,
        languageId: language.id,
        modDate: new Date().toISOString(),
        inode: faker.string.uuid(),
        archived: faker.datatype.boolean(),
        baseType: 'content',
        contentType: 'test',
        folder: 'test',
        host: 'test',
        identifier: faker.string.uuid(),
        live: faker.datatype.boolean(),
        locked: faker.datatype.boolean(),
        owner: 'test',
        permissions: [],
        working: true,
        contentTypeId: 'test',
        url: 'test',
        hasLiveVersion: true,
        deleted: false,
        hasTitleImage: false,
        hostName: 'test',
        modUser: 'test',
        modUserName: 'test',
        publishDate: new Date().toISOString(),
        sortOrder: 0,
        versionType: 'test',
        stInode: 'test',
        titleImage: 'test'
    };

    return { ...defaultContentlet, ...overrides };
}

export const CONTENTLETS_MOCK_FOR_EDITOR = [
    {
        hostName: 'demo.dotcms.com',
        modDate: '2021-04-08 13:53:32.618',
        imageMetaData: {
            modDate: 1703020595125,
            sha256: '01bed04a0807b45245d38188da3bece44e42fcdd0cf8e8bfe0585e8bd7a61913',
            length: 15613,
            title: 'box-info-2-270x270.jpg',
            version: 20220201,
            isImage: true,
            fileSize: 15613,
            name: 'box-info-2-270x270.jpg',
            width: 270,
            contentType: 'image/jpeg',
            height: 270
        },
        publishDate: '2021-04-08 13:53:32.681',
        description:
            "Snowboarding, once a prime route for teen rebellion, today is definitely mainstream. Those teens — both guys and Shred Bettys, who took up snowboarding in the late '80s and '90s now are riding with their kids.",
        title: 'Snowboarding',
        body: "<p>As with skiing, there are different styles of riding. Free-riding is all-mountain snowboarding on the slopes, in the trees, down the steeps and through the moguls. Freestyle is snowboarding in a pipe or park filled with rails, fun boxes and other features.<br /><br />Snowboarding parks are designed for specific skill levels, from beginner parks with tiny rails hugging the ground to terrain parks with roller-coaster rails, fun boxes and tabletops for more experienced snowboarders.<br /><br />Whether you're a first-timer or already comfortable going lip-to-lip in a pipe, there are classes and special clinics for you at our ski and snowboard resorts. Our resorts offer multiday clinics, so if you're headed to ski this winter, consider wrapping your vacation dates around a snowboarding clinic.</p>",
        baseType: 'CONTENT',
        inode: 'd77576ce-6e3a-4cf3-b412-8e5209f56cae',
        archived: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        working: true,
        locked: false,
        stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        contentType: 'Activity',
        live: true,
        altTag: 'Snowboarding',
        owner: 'dotcms.org.1',
        imageVersion: '/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/image/box-info-2-270x270.jpg',
        identifier: '574f0aec-185a-4160-9c17-6d037b298318',
        image: '/dA/574f0aec-185a-4160-9c17-6d037b298318/image/box-info-2-270x270.jpg',
        imageContentAsset: '574f0aec-185a-4160-9c17-6d037b298318/image',
        urlTitle: 'snowboarding',
        languageId: 1,
        URL_MAP_FOR_CONTENT: '/activities/snowboarding',
        url: '/content.2f6fe5b8-a2cc-4ecb-a868-db632d695fca',
        tags: 'snowboarding,winterenthusiast',
        titleImage: 'image',
        modUserName: 'Admin User',
        urlMap: '/activities/snowboarding',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        modUser: 'dotcms.org.1',
        __icon__: 'contentIcon',
        contentTypeIcon: 'paragliding',
        variant: 'DEFAULT'
    },
    {
        hostName: 'demo.dotcms.com',
        modDate: '2020-09-02 16:42:10.049',
        imageMetaData: {
            modDate: 1703020594791,
            sha256: '7e1cf9d3c8c144f592af72658456031c8283bfe4a5ecce3e188c71aa7b1e590e',
            length: 37207,
            title: 'zip-line.jpg',
            version: 20220201,
            isImage: true,
            fileSize: 37207,
            name: 'zip-line.jpg',
            width: 270,
            contentType: 'image/jpeg',
            height: 270
        },
        publishDate: '2020-09-02 16:42:10.101',
        description:
            'Ever wondered what it is like to fly through the forest canopy? Zip-lining ais the best way to explore the forest canopy, where thick branches serve as platforms for the adventurous traveler, more than 100 feet above the forest floor.',
        title: 'Zip-Lining',
        body: '<p>Ever wondered what it is a monkey finds so fascinating about the forest canopy? Costa Rica is a pioneer in canopy exploration, where thick branches serve as platforms for the adventurous traveler, more than 100 feet above the jungle floor. If you&rsquo;re wondering why you&rsquo;d want to head to the top of a tree just to look around, remember that 90% of Costa Rica animals and 50% of plant species in rainforests live in the upper levels of the trees. When you explore that far off the ground, the view is something you&rsquo;ll never forget! A Costa Rica zip line tour, hanging bridges hike, or aerial tram tour are all fantastic ways to take advantage of Costa Rica&rsquo;s stunning forest canopy views.</p>\n<p>Almost anyone of any age and physical condition can enjoy a Costa Rica zip line adventure as it is not strenuous. Secured into a harness and attached to a sturdy cable, you will have the opportunity to fly through the rainforest canopy and experience a bird&rsquo;s eye view of the lively forest below. A Costa Rica zip line is about a five hour adventure that operates rain or shine all year and is led by bilingual guides. For the non-adrenaline junkie, the aerial tram can take you through the rainforest in comfort and safety. Prefer to linger? Hanging bridges offer panoramic views for acres, and an experienced guide will be happy to point out a variety of birds and animals.</p>',
        baseType: 'CONTENT',
        inode: '8df9a375-0386-401c-b5d6-da21c1c5c301',
        archived: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        working: true,
        locked: false,
        stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        contentType: 'Activity',
        live: true,
        altTag: 'Zip-line',
        owner: 'dotcms.org.1',
        imageVersion: '/dA/8df9a375-0386-401c-b5d6-da21c1c5c301/image/zip-line.jpg',
        identifier: '50757fb4-75df-4e2c-8335-35d36bdb944b',
        image: '/dA/50757fb4-75df-4e2c-8335-35d36bdb944b/image/zip-line.jpg',
        imageContentAsset: '50757fb4-75df-4e2c-8335-35d36bdb944b/image',
        urlTitle: 'zip-lining',
        languageId: 1,
        URL_MAP_FOR_CONTENT: '/activities/zip-lining',
        url: '/content.c17f9c4c-ad14-4777-ae61-334ee6c9fcbf',
        tags: 'ecoenthusiast,zip-lining',
        titleImage: 'image',
        modUserName: 'Admin User',
        urlMap: '/activities/zip-lining',
        hasLiveVersion: true,
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        modUser: 'dotcms.org.1',
        __icon__: 'contentIcon',
        contentTypeIcon: 'paragliding',
        variant: 'DEFAULT'
    }
];
