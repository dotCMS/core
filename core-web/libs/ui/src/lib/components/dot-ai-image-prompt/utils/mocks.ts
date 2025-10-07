import {
    DotAIImageContent,
    DotAIImageOrientation,
    DotCMSContentlet,
    DotGeneratedAIImage,
    PromptType
} from '@dotcms/dotcms-models';

const mcokContentLetImage: DotCMSContentlet = {
    AUTO_ASSIGN_WORKFLOW: false,
    __IS_NEW_CONTENT__: true,
    __icon__: 'Icon',
    archived: false,
    asset: '/dA/9822f02de0ada4cffb5ae6ea045f4d8c/asset/create_20241014_035505.png',
    assetContentAsset: '9822f02de0ada4cffb5ae6ea045f4d8c/asset',
    assetMetaData: {
        contentType: 'image/png',
        editableAsText: false,
        fileSize: 3561005,
        height: 1024,
        isImage: true,
        length: 3561005,
        modDate: 1728921308868,
        name: 'create_20241014_035505.png',
        sha256: '540395e845f7d51e7a15ae91455fb462276844e97f7488e40fea00858623596a',
        title: 'create_20241014_035505.png',
        version: 20220201,
        width: 1792
    },
    assetVersion: '/dA/b86f89b9-b6be-49d0-a8a4-b94f4d996555/asset/create_20241014_035505.png',
    baseType: 'DOTASSET',
    contentType: 'Images',
    creationDate: 1728921308933,
    extension: 'png',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: true,
    host: 'SYSTEM_HOST',
    hostName: 'System Host',
    identifier: '9822f02de0ada4cffb5ae6ea045f4d8c',
    inode: 'b86f89b9-b6be-49d0-a8a4-b94f4d996555',
    isContentlet: true,
    languageId: 1,
    live: true,
    locked: false,
    mimeType: 'image/png',
    modDate: '1728921308931',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    name: 'create_20241014_035505.png',
    owner: 'dotcms.org.1',
    ownerUserName: 'Admin User',
    path: '/content.b86f89b9-b6be-49d0-a8a4-b94f4d996555',
    publishDate: 1728921308967,
    publishUser: 'dotcms.org.1',
    publishUserName: 'Admin User',
    size: 3561005,
    sortOrder: 0,
    stInode: '4061db324fdce3993a494c856e76ab1e',
    statusIcons:
        "<span class='greyDotIcon' style='opacity:.4'></span><span class='liveIcon'></span>",
    title: 'create_20241014_035505.png',
    titleImage: 'asset',
    type: 'dotasset',
    url: '/content.b86f89b9-b6be-49d0-a8a4-b94f4d996555',
    working: true
};

export const MOCK_AI_IMAGE_CONTENT: DotAIImageContent = {
    originalPrompt: 'Test prompt',
    response: 'Test response',
    revised_prompt: 'Test revised prompt',
    tempFileName: 'Test Imagae',
    url: 'Test url',
    contentlet: mcokContentLetImage
};

export const MOCK_GENERATED_AI_IMAGE: DotGeneratedAIImage = {
    request: {
        text: 'Test prompt',
        type: PromptType.INPUT,
        size: DotAIImageOrientation.HORIZONTAL
    },
    response: MOCK_AI_IMAGE_CONTENT
};
