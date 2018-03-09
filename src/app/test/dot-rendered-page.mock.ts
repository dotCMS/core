import { DotTemplate } from './../portlets/dot-edit-page/shared/models/dot-template.model';
import { DotPage } from './../portlets/dot-edit-page/shared/models/dot-page.model';
import { DotRenderedPage } from '../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { DotLayout } from '../portlets/dot-edit-page/shared/models/dot-layout.model';

export const mockDotPage: DotPage = {
    canEdit: true,
    canLock: true,
    identifier: '123',
    languageId: 1,
    liveInode: '',
    lockMessage: '',
    lockedBy: 'someone',
    lockedByName: 'Some One',
    lockedOn: new Date(1517330917295),
    pageURI: 'an/url/test',
    shortyLive: '',
    shortyWorking: '',
    title: 'A title',
    workingInode: '999',
    contentType: {
        clazz: '',
        defaultType: true,
        fixed: true,
        folder: '',
        host: '',
        name: '',
        owner: '',
        system: true
    },
    fileAsset: true,
    friendlyName: '',
    host: '',
    inode: '',
    name: '',
    systemHost: false,
    type: '',
    uri: '',
    versionType: '',
};

export const mockDotLayout: DotLayout = {
    title: '',
    header: true,
    footer: false,
    sidebar: {
        location: 'left',
        width: 'small',
        containers: [
            {
                identifier: 'fc193c82-8c32-4abe-ba8a-49522328c93e',
                uuid: 'LEGACY_RELATION_TYPE'
            }
        ]
    },
    body: {
        rows: []
    }
};

export const mockDotContainers: any = {
    '5363c6c6-5ba0-4946-b7af-cf875188ac2e': {
        container: {
            type: 'containers',
            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
            name: 'Medium Column (md-1)',
            categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93'
        }
    },
    '56bd55ea-b04b-480d-9e37-5d6f9217dcc3': {
        container: {
            type: 'containers',
            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
            name: 'Large Column (lg-1)',
            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f'
        }
    }
};

export const mockDotTemplate: DotTemplate = {
    anonymous: true,
    friendlyName: '',
    identifier: '',
    inode: '123',
    name: '',
    title: 'anonymous_layout_1511798005268',
    type: '',
    versionType: '',
    drawed: true,
    canEdit: true
};

export const mockDotRenderedPage: DotRenderedPage = {
    containers: mockDotContainers,
    html: '<html></html>',
    layout: mockDotLayout,
    page: mockDotPage,
    template: mockDotTemplate,
    canCreateTemplate: true
};
