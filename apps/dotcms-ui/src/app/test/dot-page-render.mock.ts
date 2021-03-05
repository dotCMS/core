import { DotPage } from '@shared/models/dot-page/dot-page.model';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { mockDotLanguage } from './dot-language.mock';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { CONTAINER_SOURCE } from '@models/container/dot-container.model';
import { dotcmsContentTypeBasicMock } from './dot-content-types.mock';
import { DotLayout, DotTemplate } from '@models/dot-edit-layout-designer';

export const mockDotPage = (): DotPage => {
    return {
        canEdit: true,
        canLock: true,
        canRead: true,
        identifier: '123',
        languageId: 1,
        liveInode: '456',
        lockMessage: '',
        lockedBy: '123',
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
        rendered: '<html><head></header><body><p>Hello World</p></body></html>'
    };
};

export const mockDotLayout = (): DotLayout => {
    return {
        title: '',
        header: false,
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
        },
        width: ''
    };
};

export const mockDotContainers = (): any => {
    return [
        {
            container: {
                type: 'containers',
                identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                name: 'Medium Column (md-1)',
                categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        },
        {
            container: {
                type: 'containers',
                identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                name: 'Large Column (lg-1)',
                categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
                source: CONTAINER_SOURCE.FILE,
                path: '/container/path',
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        }
    ];
};

export const mockDotTemplate = () => {
    return {
        anonymous: false,
        friendlyName: '',
        identifier: '',
        inode: '123',
        name: '',
        title: 'Template Name',
        type: '',
        versionType: '',
        drawed: true,
        canEdit: true,
        theme: '',
        layout: null,
        hasLiveVersion: true,
        working: true
    };
};

export const mockDotTemplateLayout: DotTemplate = {
    ...mockDotTemplate(),
    anonymous: false,
    title: 'anonymous_layout_1511798005268'
};

export const mockDotRenderedPage = (): DotPageRender.Parameters => {
    return {
        containers: mockDotContainers(),
        layout: mockDotLayout(),
        page: mockDotPage(),
        template: mockDotTemplate(),
        canCreateTemplate: true,
        numberContents: 1,
        viewAs: {
            language: mockDotLanguage.id,
            mode: DotPageMode.PREVIEW
        }
    };
};
