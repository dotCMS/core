import { DotEditLayoutService } from './dot-edit-layout.service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { mockDotContainers } from '../../../../test/dot-page-render.mock';
import { DotContainerColumnBox } from '../models/dot-container-column-box.model';
import { DotLayoutBody } from '@portlets/dot-edit-page/shared/models/dot-layout-body.model';
import { DotLayoutGridBox } from '@portlets/dot-edit-page/shared/models/dot-layout-grid-box.model';
import { CONTAINER_SOURCE } from '@models/container/dot-container.model';
import { DotLayoutGrid } from '../models/dot-layout-grid.model';
import { ReflectiveInjector } from '@angular/core';

describe('DotEditLayoutService', () => {
    const containers = {
        '5363c6c6-5ba0-4946-b7af-cf875188ac2e': {
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
        '56bd55ea-b04b-480d-9e37-5d6f9217dcc3': {
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
        },
        '/container/path': {
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
        },
        '6a12bbda-0ae2-4121-a98b-ad8069eaff3a': {
            container: {
                type: 'containers',
                identifier: '6a12bbda-0ae2-4121-a98b-ad8069eaff3a',
                name: 'Banner Carousel ',
                categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        },
        'a6e9652b-8183-4c09-b775-26196b09a300': {
            container: {
                type: 'containers',
                identifier: 'a6e9652b-8183-4c09-b775-26196b09a300',
                name: 'Default 4 (Page Content)',
                categoryId: '8cbcb97e-8e04-4691-8555-da82c3dc4a91',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        },
        'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea': {
            container: {
                type: 'containers',
                identifier: 'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea',
                name: 'Blank Container',
                categoryId: '3ba890c5-670c-467d-890d-bd8e9b9bb5ef',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        }
    };

    let dotEditLayoutService: DotEditLayoutService;
    let templateContainersCacheService: TemplateContainersCacheService;

    beforeEach(() => {
        this.injector = ReflectiveInjector.resolveAndCreate([
            DotEditLayoutService,
            TemplateContainersCacheService
        ]);
        dotEditLayoutService = this.injector.get(DotEditLayoutService);
        templateContainersCacheService = this.injector.get(TemplateContainersCacheService);

        templateContainersCacheService.set(containers);
    });

    it('should transform the data from the service to the grid format ', () => {
        const dotLayoutBody: DotLayoutBody = {
            rows: [
                {
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '/container/path',
                                    uuid: '1'
                                }
                            ],
                            leftOffset: 1,
                            width: 8
                        },
                        {
                            containers: [
                                {
                                    identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                                    uuid: '2'
                                }
                            ],
                            leftOffset: 9,
                            width: 4
                        }
                    ]
                },
                {
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: 'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea',
                                    uuid: '3'
                                },
                                {
                                    identifier: 'a6e9652b-8183-4c09-b775-26196b09a300',
                                    uuid: '4'
                                },
                                {
                                    identifier: 'UNKNOWN ID',
                                    uuid: 'UNKNOWN'
                                }
                            ],
                            leftOffset: 1,
                            width: 3
                        },
                        {
                            containers: [
                                {
                                    identifier: '6a12bbda-0ae2-4121-a98b-ad8069eaff3a',
                                    uuid: '5'
                                }
                            ],
                            leftOffset: 4,
                            width: 3
                        }
                    ]
                }
            ]
        };

        const grid: DotLayoutGrid = dotEditLayoutService.getDotLayoutGridBox(dotLayoutBody);

        expect(grid.boxes.length).toEqual(4);
        expect(grid.boxes[2].containers.length).toEqual(2);
        expect(grid.boxes[3].config).toEqual({
            col: 4,
            row: 2,
            sizex: 3,
            fixed: true,
            maxCols: 12,
            maxRows: 1,
            payload: {
                containers: [{ identifier: '6a12bbda-0ae2-4121-a98b-ad8069eaff3a', uuid: '5' }],
                leftOffset: 4,
                width: 3
            }
        });
        expect(grid.boxes[0].containers.length).toEqual(1, 'map FILE type containers');
    });

    it('should transform the grid data to LayoutBody to export the data', () => {
        const gridBoxes: DotLayoutGridBox[] = [
            {
                containers: [
                    {
                        container: {
                            type: 'containers',
                            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                            name: 'Large Column (lg-1)',
                            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
                            source: CONTAINER_SOURCE.FILE,
                            path: 'container/path',
                            parentPermissionable: {
                                hostname: 'demo.dotcms.com'
                            }
                        },
                        uuid: '2'
                    }
                ],
                config: {
                    fixed: true,
                    sizex: 8,
                    maxCols: 12,
                    maxRows: 1,
                    col: 1,
                    row: 1,
                    sizey: 1,
                    dragHandle: null,
                    resizeHandle: null,
                    draggable: true,
                    resizable: true,
                    borderSize: 25,
                    payload: {
                        styleClass: 'test_column_class'
                    }
                }
            },
            {
                containers: [
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
                        },
                        uuid: '1'
                    },
                    {
                        container: {
                            type: 'containers',
                            identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                            name: 'Large Column (lg-1)',
                            categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f',
                            source: CONTAINER_SOURCE.FILE,
                            parentPermissionable: {
                                hostname: 'demo.dotcms.com'
                            }
                        },
                        uuid: '2'
                    },
                    {
                        container: null,
                        uuid: '1234'
                    }
                ],
                config: {
                    fixed: true,
                    sizex: 4,
                    maxCols: 12,
                    maxRows: 1,
                    col: 9,
                    row: 1,
                    sizey: 1,
                    dragHandle: null,
                    resizeHandle: null,
                    draggable: true,
                    resizable: true,
                    borderSize: 25,
                    payload: {
                        styleClass: ''
                    }
                }
            }
        ];
        const grid: DotLayoutGrid = new DotLayoutGrid(gridBoxes, ['test_row_class']);
        const layoutBody: DotLayoutBody = dotEditLayoutService.getDotLayoutBody(grid);

        expect(layoutBody.rows.length).toEqual(1);

        expect(layoutBody.rows[0].styleClass).toEqual('test_row_class');
        expect(layoutBody.rows[0].columns[0].styleClass).toEqual('test_column_class');
        expect(layoutBody.rows[0].columns[1].styleClass).toEqual('');
        expect(layoutBody.rows[0].columns.length).toEqual(2, 'create two columns');
        expect(layoutBody.rows[0].columns[1].containers.length).toEqual(2, 'create two containers');
        expect(layoutBody.rows[0].columns[1].leftOffset).toEqual(9, 'set leftOffset to 9');
        expect(layoutBody.rows[0].columns[1].width).toEqual(
            4,
            'set container box to 4 in the second column'
        );
    });

    it('should transform the Sidebar data to ContainerColumnBox (ignore UNKNOWN ids) to export the data', () => {
        const rawContainers = [
            {
                identifier: mockDotContainers[0].container.identifier,
                uuid: '1234567890'
            },
            {
                identifier: mockDotContainers[1].container.path,
                uuid: '1234567891'
            },
            {
                identifier: 'UNKNOWN ID',
                uuid: 'INVALID'
            }
        ];

        const containerColumnBox: DotContainerColumnBox[] = dotEditLayoutService.getDotLayoutSidebar(
            rawContainers
        );
        delete containerColumnBox[0].uuid;
        delete containerColumnBox[1].uuid;

        expect(containerColumnBox).toEqual(mockDotContainers);
    });

    it('should emit add box event', (done) => {
        dotEditLayoutService.getBoxes().subscribe((box: boolean) => {
            expect(box).toBe(true);
            done();
        });

        dotEditLayoutService.addBox();
    });
});
