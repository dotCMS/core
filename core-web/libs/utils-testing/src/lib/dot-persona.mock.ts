import { DEFAULT_PERSONA_IDENTIFIER_BY_BACKEND } from '@components/dot-persona-selector/dot-persona-selector.component';
import { DotPersona } from '@dotcms/dotcms-models';

export const mockDotPersona: DotPersona = {
    archived: false,
    baseType: 'PERSONA',
    contentType: 'persona',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: false,
    host: 'SYSTEM_HOST',
    hostFolder: 'SYSTEM_HOST',
    hostName: 'System Host',
    identifier: DEFAULT_PERSONA_IDENTIFIER_BY_BACKEND,
    inode: '',
    keyTag: 'dot:persona',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '0',
    modUser: 'system',
    modUserName: 'system user system user',
    name: 'Global Investor',
    personalized: true,
    photo: '/url',
    sortOrder: 0,
    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
    title: 'Default Visitor',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    working: false,
    owner: 'me',
    url: '/root'
};
