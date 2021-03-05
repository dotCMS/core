import { CONTAINER_SOURCE, DotContainerMap } from '@models/container/dot-container.model';

export const dotContainerMapMock = (): DotContainerMap => {
    return {
        '5363c6c6-5ba0-4946-b7af-cf875188ac2e': {
            type: 'containers',
            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
            name: 'Medium Column (md-1)',
            categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93',
            source: CONTAINER_SOURCE.DB,
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        },
        '56bd55ea-b04b-480d-9e37-5d6f9217dcc3': {
            type: 'containers',
            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
            name: 'Large Column (lg-1)',
            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
            source: CONTAINER_SOURCE.FILE,
            path: '/container/path',
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        },
        '/container/path': {
            type: 'containers',
            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
            name: 'Large Column (lg-1)',
            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
            source: CONTAINER_SOURCE.FILE,
            path: '/container/path',
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        },
        '6a12bbda-0ae2-4121-a98b-ad8069eaff3a': {
            type: 'containers',
            identifier: '6a12bbda-0ae2-4121-a98b-ad8069eaff3a',
            name: 'Banner Carousel ',
            categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e',
            source: CONTAINER_SOURCE.DB,
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        },
        'a6e9652b-8183-4c09-b775-26196b09a300': {
            type: 'containers',
            identifier: 'a6e9652b-8183-4c09-b775-26196b09a300',
            name: 'Default 4 (Page Content)',
            categoryId: '8cbcb97e-8e04-4691-8555-da82c3dc4a91',
            source: CONTAINER_SOURCE.DB,
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        },
        'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea': {
            type: 'containers',
            identifier: 'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea',
            name: 'Blank Container',
            categoryId: '3ba890c5-670c-467d-890d-bd8e9b9bb5ef',
            source: CONTAINER_SOURCE.DB,
            parentPermissionable: {
                hostname: 'demo.dotcms.com'
            }
        }
    };
};
