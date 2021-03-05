import { DotPage } from '@portlets/dot-edit-page/shared/models';
import { dotcmsContentTypeBasicMock } from './dot-content-types.mock';

export const mockDotPage: DotPage = {
    canEdit: true,
    canLock: true,
    canRead: true,
    identifier: '123',
    languageId: 1,
    liveInode: '456',
    lockMessage: '',
    lockedBy: 'someone',
    lockedByName: 'Some One',
    lockedOn: new Date(1517330917295),
    pageURI: '/an/url/test',
    shortyLive: '',
    shortyWorking: '',
    title: 'A title',
    workingInode: '999',
    contentType: {
        ...dotcmsContentTypeBasicMock,
        defaultType: true,
        fixed: true,
        system: true
    },
    fileAsset: true,
    friendlyName: '',
    host: '',
    inode: '2',
    name: '',
    systemHost: false,
    type: '',
    uri: '',
    versionType: '',
    rendered: '<html></html>'
};
