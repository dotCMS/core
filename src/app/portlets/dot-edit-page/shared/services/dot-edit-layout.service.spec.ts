import { DotEditLayoutService } from './dot-edit-layout.service';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';

describe('DotEditLayoutService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotEditLayoutService]);
        this.DotEditLayoutService = this.injector.get(DotEditLayoutService);
    });

    it('should transform the data from the service to the grid format ', () => {
        const pageView: DotPageView = {
            containers: {
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
                },
                '6a12bbda-0ae2-4121-a98b-ad8069eaff3a': {
                    container: {
                        type: 'containers',
                        identifier: '6a12bbda-0ae2-4121-a98b-ad8069eaff3a',
                        name: 'Banner Carousel ',
                        categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e'
                    }
                },
                'a6e9652b-8183-4c09-b775-26196b09a300': {
                    container: {
                        type: 'containers',
                        identifier: 'a6e9652b-8183-4c09-b775-26196b09a300',
                        name: 'Default 4 (Page Content)',
                        categoryId: '8cbcb97e-8e04-4691-8555-da82c3dc4a91'
                    }
                },
                'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea': {
                    container: {
                        type: 'containers',
                        identifier: 'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea',
                        name: 'Blank Container',
                        categoryId: '3ba890c5-670c-467d-890d-bd8e9b9bb5ef'
                    }
                }
            },
            page: null,
            layout: {
                title: 'anonymouslayout1511296039453',
                header: false,
                footer: false,
                body: {
                    rows: [
                        {
                            columns: [
                                {
                                    containers: ['56bd55ea-b04b-480d-9e37-5d6f9217dcc3'],
                                    leftOffset: 1,
                                    width: 8
                                },
                                {
                                    containers: ['5363c6c6-5ba0-4946-b7af-cf875188ac2e'],
                                    leftOffset: 9,
                                    width: 4
                                }
                            ]
                        },
                        {
                            columns: [
                                {
                                    containers: [
                                        'd71d56b4-0a8b-4bb2-be15-ffa5a23366ea',
                                        'a6e9652b-8183-4c09-b775-26196b09a300'
                                    ],
                                    leftOffset: 1,
                                    width: 3
                                },
                                {
                                    containers: ['6a12bbda-0ae2-4121-a98b-ad8069eaff3a'],
                                    leftOffset: 4,
                                    width: 3
                                }
                            ]
                        }
                    ]
                },
                sidebar: null
            }
        };
        const grid: DotLayoutGridBox[] = this.DotEditLayoutService.getDotLayoutGridBox(pageView, {
            fixed: true,
            sizex: 3,
            maxCols: 12,
            maxRows: 1
        });

        expect(grid.length).toEqual(4);
        expect(grid[2].containers.length).toEqual(2);
        expect(grid[3].config).toEqual({
            col: 4,
            row: 2,
            sizex: 3,
            fixed: true,
            maxCols: 12,
            maxRows: 1
        });
    });

    it('should transform the grid data to LayoutBody to export the data', () => {
        const grid: DotLayoutGridBox[] = [
            {
                containers: [
                    {
                        type: 'containers',
                        identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                        name: 'Large Column (lg-1)',
                        categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f'
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
                    borderSize: 25
                }
            },
            {
                containers: [
                    {
                        type: 'containers',
                        identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                        name: 'Medium Column (md-1)',
                        categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93'
                    },
                    {
                        type: 'containers',
                        identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                        name: 'Large Column (lg-1)',
                        categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f'
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
                    borderSize: 25
                }
            }
        ];
        const layoutBody: DotLayoutBody = this.DotEditLayoutService.getDotLayoutBody(grid);

        expect(layoutBody.rows.length).toEqual(1);
        expect(layoutBody.rows[0].columns.length).toEqual(2, 'create two columns');
        expect(layoutBody.rows[0].columns[1].containers.length).toEqual(2, 'reate two containers');
        expect(layoutBody.rows[0].columns[1].leftOffset).toEqual(9, 'set leftOffset to 9');
        expect(layoutBody.rows[0].columns[1].width).toEqual(4, 'create 4 containers for the first row');
    });
});
