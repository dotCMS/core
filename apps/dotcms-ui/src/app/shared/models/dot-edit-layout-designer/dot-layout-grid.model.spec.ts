import { DotLayoutGrid } from './dot-layout-grid.model';
import { CONTAINER_SOURCE } from '@shared/models/container/dot-container.model';
import { DotLayoutGridBox } from './dot-layout-grid-box.model';

describe('DotLayoutGridRow', () => {
    let dotLayoutGrid: DotLayoutGrid;

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
                    uuid: '0'
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
                borderSize: 25
            }
        },
        {
            containers: [
                {
                    container: {
                        type: 'containers',
                        identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc4',
                        name: 'Large Column (lg-1)',
                        categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94g',
                        source: CONTAINER_SOURCE.FILE,
                        path: 'container/path',
                        parentPermissionable: {
                            hostname: 'demo.dotcms.com'
                        }
                    },
                    uuid: '3'
                }
            ],
            config: {
                fixed: true,
                sizex: 8,
                maxCols: 12,
                maxRows: 1,
                col: 1,
                row: 2,
                sizey: 1,
                dragHandle: null,
                resizeHandle: null,
                draggable: true,
                resizable: true,
                borderSize: 25
            }
        }
    ];

    const rowClasses: string[] = [
        'test_column_class_1',
        'test_column_class_2',
        'test_column_class_3'
    ];

    beforeEach(() => {
        dotLayoutGrid = new DotLayoutGrid([...gridBoxes], [...rowClasses]);
    });

    it('should get default box config object', () => {
        expect(DotLayoutGrid.getDefaultConfig()).toEqual({
            fixed: true,
            sizex: 3,
            maxCols: 12,
            maxRows: 1,
            payload: {
                styleClass: ''
            }
        });
    });

    it('should get default grid layout object', () => {
        const defaultGrid = DotLayoutGrid.getDefaultGrid();

        expect(defaultGrid.boxes).toEqual([
            {
                config: {
                    fixed: true,
                    sizex: 12,
                    maxCols: 12,
                    maxRows: 1,
                    col: 1,
                    row: 1,
                    payload: {
                        styleClass: ''
                    }
                },
                containers: []
            }
        ]);

        expect(defaultGrid.getAllRowClass()).toEqual(['']);
    });

    it('should return all boxes', () => {
        expect(dotLayoutGrid.boxes).toEqual(gridBoxes);
    });

    it('should return all DotLayoutGridRow', () => {
        const copy: any = JSON.parse(JSON.stringify(gridBoxes));
        const row1: any = JSON.parse(JSON.stringify(gridBoxes));

        row1.splice(1, 1);
        row1[0].config = copy[0].config;
        row1[1].config = copy[1].config;
        row1[0].containers = [...copy[0].containers];
        row1[1].containers = [...copy[1].containers];

        expect(dotLayoutGrid.getRows()).toEqual([
            {
                boxes: row1,
                styleClass: 'test_column_class_1'
            },
            {
                boxes: [gridBoxes[2]],
                styleClass: 'test_column_class_2'
            }
        ]);
    });

    it('should the second box', () => {
        expect(dotLayoutGrid.box(1)).toEqual(gridBoxes[1]);
    });

    it('should add a new box', () => {
        dotLayoutGrid.addBox();
        expect(dotLayoutGrid.boxes.length).toEqual(4);

        expect(dotLayoutGrid.boxes[3]).toEqual({
            config: {
                fixed: true,
                sizex: 3,
                maxCols: 12,
                maxRows: 1,
                row: 2,
                col: 9,
                payload: {
                    styleClass: ''
                }
            },
            containers: []
        });
    });

    it('should remove a container 1 and Css class asociated to that row', () => {
        dotLayoutGrid.removeContainer(0);
        expect(dotLayoutGrid.boxes.length).toEqual(2);
        expect(dotLayoutGrid.getAllRowClass()).toEqual([
            'test_column_class_2',
            'test_column_class_3'
        ]);
    });

    it('should get all row class', () => {
        expect(dotLayoutGrid.getAllRowClass()).toEqual(rowClasses);
    });

    it('should get row class 1', () => {
        expect(dotLayoutGrid.getRowClass(1)).toEqual('test_column_class_2');
    });

    it('should set row class', () => {
        dotLayoutGrid.setRowClass('test_column_class_3', 1);
        expect(dotLayoutGrid.getRowClass(1)).toEqual('test_column_class_3');
    });
});
