import { expect, jest, describe } from '@jest/globals';
import { Observable, of } from 'rxjs';
import { v4 as uuid } from 'uuid';

import { TestBed } from '@angular/core/testing';

import { pluck, take } from 'rxjs/operators';

import { DotContainer, DotContainerMap, CONTAINER_SOURCE } from '@dotcms/dotcms-models';
import { containersMock } from '@dotcms/utils-testing';

import { DotTemplateBuilderStore } from './template-builder.store';

import {
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateLayoutProperties
} from '../models/models';
import {
    GRIDSTACK_DATA_MOCK,
    INITIAL_STATE_MOCK,
    mockDefaultContainerWithPath,
    mockDefaultContainerWithoutPath,
    mockTemplateBuilderContainer,
    mockTempContainer,
    ROWS_MINIMAL_MOCK,
    SIDEBAR_MOCK,
    STYLE_CLASS_MOCK
} from '../utils/mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

// Here i just swapped the rows and changed the uuid as expected from the backend
const SWAPPED_ROWS_MOCK = [
    {
        ...ROWS_MINIMAL_MOCK[1],
        y: 0 // This sets the order of the rows
    },
    {
        ...ROWS_MINIMAL_MOCK[0],
        y: 1 // This sets the order of the rows
    }
];

// Update the containers uuid simulating the backend
const UPDATED_ROWS_MOCK: DotGridStackWidget[] = [
    {
        ...ROWS_MINIMAL_MOCK[1],
        id: 'random test 2',
        y: 0, // This sets the order of the rows
        subGridOpts: {
            ...ROWS_MINIMAL_MOCK[1].subGridOpts,
            children: ROWS_MINIMAL_MOCK[1].subGridOpts.children.map((col) => ({
                ...col,
                id: 'hello there 2',
                containers: col.containers.map((child, i) => ({
                    ...child,
                    uuid: `${i + 1}` // 1 for the 0 index
                }))
            }))
        }
    },
    {
        ...ROWS_MINIMAL_MOCK[0],
        id: 'random test 1',
        y: 1, // This sets the order of the rows
        subGridOpts: {
            ...ROWS_MINIMAL_MOCK[0].subGridOpts,
            children: ROWS_MINIMAL_MOCK[0].subGridOpts.children.map((col) => ({
                ...col,
                id: 'hello there 1',
                containers: col.containers.map((child, i) => ({
                    ...child,
                    uuid: `${i + 3}` // 1 for the 0 index and 2 for the first 2 containers
                }))
            }))
        }
    }
];

const RESULT_AFTER_MERGE_MOCK = [
    {
        ...ROWS_MINIMAL_MOCK[1],
        y: 0, // This sets the order of the rows
        subGridOpts: {
            ...ROWS_MINIMAL_MOCK[1].subGridOpts,
            children: ROWS_MINIMAL_MOCK[1].subGridOpts.children.map((col) => ({
                ...col,
                containers: col.containers.map((child, i) => ({
                    ...child,
                    uuid: `${i + 1}` // 1 for the 0 index
                }))
            }))
        }
    },
    {
        ...ROWS_MINIMAL_MOCK[0],
        y: 1, // This sets the order of the rows
        subGridOpts: {
            ...ROWS_MINIMAL_MOCK[0].subGridOpts,
            children: ROWS_MINIMAL_MOCK[0].subGridOpts.children.map((col) => ({
                ...col,

                containers: col.containers.map((child, i) => ({
                    ...child,
                    uuid: `${i + 3}` // 1 for the 0 index and 2 for the first 2 containers
                }))
            }))
        }
    }
];

describe('DotTemplateBuilderStore', () => {
    let service: DotTemplateBuilderStore;
    let rows$: Observable<{ rows: DotGridStackWidget[]; shouldEmit: boolean }>;
    let layoutProperties$: Observable<DotTemplateLayoutProperties>;
    let containerMap$: Observable<DotContainerMap>;
    let initialState: { rows: DotGridStackWidget[]; shouldEmit: boolean };
    const mockContainer = containersMock[0];
    const minDataMockContainer = {
        identifier: mockContainer.identifier
    };

    const addContainer = (container = mockContainer) => {
        const parentRow = initialState.rows[0];

        const columnToAddContainer: DotGridStackWidget = {
            ...parentRow.subGridOpts?.children[0],
            parentId: parentRow.id as string
        };
        service.addContainer({
            affectedColumn: columnToAddContainer,
            container
        });
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotTemplateBuilderStore]
        });
        service = TestBed.inject(DotTemplateBuilderStore);
        rows$ = service.rows$;
        layoutProperties$ = service.layoutProperties$;
        containerMap$ = service.vm$.pipe(pluck('containerMap'));

        // Reset the state because is manipulated by reference
        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                header: true,
                footer: true,
                sidebar: SIDEBAR_MOCK
            }
        });

        // Get the initial state
        rows$.pipe(take(1)).subscribe(({ rows }) => {
            initialState = { rows: structuredClone(rows), shouldEmit: true }; // To lose the reference
        });
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should initialize the state', (done) => {
        expect.assertions(2);
        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows).toEqual(initialState.rows);
            expect(shouldEmit).toEqual(initialState.shouldEmit);
            done();
        });
    });

    it('should add a new row', (done) => {
        expect.assertions(4);
        const mockRow: DotGridStackWidget = {
            styleClass: ['mock-class'],
            containers: [],
            y: 1
        };

        service.addRow(mockRow);

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows.length).toBeGreaterThan(initialState.rows.length);
            expect(rows[3].subGridOpts.children[0].w).toBe(3);
            expect(rows[3].subGridOpts.children[0].containers).toEqual([]);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should move a row', (done) => {
        expect.assertions(2);
        const mockAffectedRows: DotGridStackWidget[] = [
            { ...initialState.rows[1], y: 0 },
            { ...initialState.rows[0], y: 1 }
        ];

        service.moveRow(mockAffectedRows);

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows[0].y).toEqual(initialState.rows[1].y);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should remove a row', (done) => {
        expect.assertions(2);
        const rowToDelete = initialState.rows[0];

        const toDeleteID = rowToDelete.id;

        service.removeRow(toDeleteID as string);

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows).not.toContainEqual(rowToDelete);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should update a row', (done) => {
        expect.assertions(2);
        const updatedRow: DotGridStackWidget = {
            ...initialState.rows[0],
            styleClass: ['new-class', 'flex-mock'],
            containers: [{ identifier: 'mock-container', uuid: uuid() }]
        };

        service.updateRow(updatedRow);
        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows[0]).toEqual(updatedRow);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should update the rowResizingID', (done) => {
        expect.assertions(2);
        const rowId = uuid();
        service.setResizingRowID(rowId);
        service.vm$.subscribe(({ resizingRowID, shouldEmit }) => {
            expect(resizingRowID).toEqual(rowId);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should clean the rowResizingID', (done) => {
        expect.assertions(2);
        const rowId = uuid();
        service.setResizingRowID(rowId);
        service.setResizingRowID(null);
        service.vm$.subscribe(({ resizingRowID, shouldEmit }) => {
            expect(resizingRowID).toEqual(null);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should add a column', (done) => {
        expect.assertions(2);
        const parentId = initialState.rows[0].id as string;

        const grid = {
            grid: {
                parentGridItem: {
                    id: parentId
                }
            }
        };

        const newColumn: DotGridStackWidget = {
            x: 0,
            y: 0,
            w: 3,
            id: uuid()
        };

        service.addColumn({ ...newColumn, ...grid } as DotGridStackNode);

        rows$.subscribe(({ rows, shouldEmit }) => {
            const row = rows.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toContainEqual({
                x: newColumn.x,
                y: newColumn.y,
                w: newColumn.w,
                id: newColumn.id,
                parentId: parentId,
                styleClass: undefined,
                containers: []
            });
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should move a column in the Y-axis', (done) => {
        expect.assertions(3);
        const fromRow = initialState.rows[2];
        const toRow = initialState.rows[0];

        const oldParent = fromRow.id as string;
        const newParent = toRow.id as string;

        const node: DotGridStackNode = fromRow.subGridOpts?.children[0] as DotGridStackNode;

        const columnToDelete = {
            ...node,
            grid: {
                parentGridItem: {
                    id: oldParent
                }
            }
        };
        const columnToAdd = {
            ...node,
            grid: {
                parentGridItem: {
                    id: newParent
                }
            }
        };

        service.moveColumnInYAxis([columnToDelete, columnToAdd] as DotGridStackNode[]);

        rows$.subscribe(({ rows, shouldEmit }) => {
            const row = rows.find((item) => item.id === newParent);
            const oldRow = rows.find((item) => item.id === oldParent);

            expect(row?.subGridOpts?.children.length).toBeGreaterThan(
                toRow.subGridOpts?.children.length || 0
            );
            expect(oldRow?.subGridOpts?.children.length).toBeLessThan(
                fromRow.subGridOpts?.children.length as number
            );
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should update gridStack data of a column', (done) => {
        expect.assertions(2);
        const parentId = uuid();
        const [firstId, secondId, thirdId, fourthId] = [1, 2, 3, 4].map(() => uuid());

        const firstBox = {
            x: 0,
            y: 0,
            w: 1,
            id: firstId,
            parentId,
            grid: { parentGridItem: { id: parentId } }
        } as DotGridStackNode;

        const GRIDSTACK_DATA_MOCK = [
            {
                x: 0,
                y: 0,
                w: 12,
                id: parentId,
                subGridOpts: {
                    children: [
                        firstBox,
                        { x: 1, y: 0, w: 1, id: secondId, parentId },
                        { x: 2, y: 0, w: 1, id: thirdId, parentId },
                        { x: 3, y: 0, w: 1, id: fourthId, parentId }
                    ]
                }
            }
        ];

        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            }
        });

        const newWidth = 2;
        const affectedColumns: DotGridStackNode[] = [
            {
                ...firstBox,
                w: newWidth
            }
        ];

        service.updateColumnGridStackData(affectedColumns);

        rows$.subscribe(({ rows, shouldEmit }) => {
            const row = rows.find((item) => item.id === parentId);
            expect(row.subGridOpts.children[0].w).toEqual(newWidth);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });
    it('should update styleClass data of a column', (done) => {
        expect.assertions(2);
        const parentId = uuid();
        const firstId = uuid();

        const GRIDSTACK_DATA_MOCK = [
            {
                x: 0,
                y: 0,
                w: 12,
                id: parentId,
                subGridOpts: {
                    children: [
                        {
                            x: 0,
                            y: 0,
                            w: 1,
                            id: firstId,
                            styleClass: ['test', 'delete-this-class']
                        }
                    ]
                }
            }
        ];

        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            }
        });

        const affectedColumn: DotGridStackNode = {
            x: 1,
            y: 0,
            w: 1,
            id: firstId,
            styleClass: STYLE_CLASS_MOCK,
            parentId
        };

        service.updateColumnStyleClasses(affectedColumn);

        rows$.subscribe(({ rows, shouldEmit }) => {
            const row = rows.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children.map((child) => child.styleClass)).toContainEqual(
                STYLE_CLASS_MOCK
            );
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should remove a column', (done) => {
        expect.assertions(2);
        const parentRow = initialState.rows[2];

        const columnToDelete: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            parentId: parentRow.id as string
        };

        service.removeColumn(columnToDelete);

        rows$.subscribe(({ rows, shouldEmit }) => {
            const row = rows.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children).not.toContain(columnToDelete);
            expect(shouldEmit).toEqual(true);
            done();
        });
    });

    it('should update layout properties', (done) => {
        expect.assertions(1);
        service.updateLayoutProperties({
            header: true,
            footer: true,
            sidebar: { location: 'right' }
        });

        layoutProperties$.pipe(take(1)).subscribe((layoutProperties) => {
            expect(layoutProperties).toEqual({
                header: true,
                footer: true,
                sidebar: { ...SIDEBAR_MOCK, location: 'right' }
            });
            done();
        });
    });

    it('should update sidebar width properties', (done) => {
        expect.assertions(1);
        service.updateSidebarWidth('large');

        layoutProperties$.pipe(take(1)).subscribe((layoutProperties) => {
            expect(layoutProperties.sidebar).toEqual({
                containers: [],
                location: 'left',
                width: 'large'
            });
            done();
        });
    });

    it('should add a container to the sidebar', (done) => {
        expect.assertions(1);
        service.addSidebarContainer(mockContainer);
        service.vm$.subscribe(({ layoutProperties }) => {
            expect(layoutProperties.sidebar.containers[0]).toEqual(minDataMockContainer);
            done();
        });
    });

    it('should add a container to container map when adding it to sidebar', (done) => {
        expect.assertions(1);
        service.addSidebarContainer(mockContainer);
        service.vm$.subscribe(({ containerMap }) => {
            expect(containerMap[mockContainer.identifier]).toEqual(mockContainer);
            done();
        });
    });

    it('should delete a container from the sidebar', (done) => {
        expect.assertions(2);
        service.addSidebarContainer(mockContainer);
        service.vm$.pipe(take(1)).subscribe(({ layoutProperties }) => {
            expect(layoutProperties.sidebar.containers).toContainEqual(minDataMockContainer);
            service.deleteSidebarContainer(0);
            expect(layoutProperties.sidebar.containers).not.toContain(minDataMockContainer);
            done();
        });
    });

    it('should add a container to specific box', (done) => {
        expect.assertions(1);
        addContainer();

        rows$.subscribe(({ rows }) => {
            const row = rows.find((item) => item.id === initialState.rows[0].id);
            expect(row?.subGridOpts?.children[0]?.containers).toContainEqual(minDataMockContainer);
            done();
        });
    });

    it('should add a container to container map', (done) => {
        expect.assertions(1);
        addContainer();

        containerMap$.subscribe((containerMap) => {
            expect(containerMap).toHaveProperty(mockContainer.identifier);
            done();
        });
    });

    it('should delete a container from specific box', (done) => {
        expect.assertions(1);
        const parentRow = initialState.rows[0];

        const columnToDeleteContainer: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            containers: [mockTemplateBuilderContainer],
            parentId: parentRow.id as string
        };
        service.deleteContainer({
            affectedColumn: columnToDeleteContainer,
            containerIndex: 0
        });
        rows$.subscribe(({ rows }) => {
            const row = rows.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children[0].containers).not.toContain(
                mockTemplateBuilderContainer
            );
            done();
        });
    });

    it('should update the theme id', (done) => {
        expect.assertions(1);
        service.updateThemeId('test-1234');

        service.themeId$.subscribe((themeId) => {
            expect(themeId).toBe('test-1234');
            done();
        });
    });

    it('should update the rows with the new data', (done) => {
        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: SWAPPED_ROWS_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            }
        });

        service.updateOldRows({ newRows: UPDATED_ROWS_MOCK, templateIdentifier: '111' });

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows).toEqual(RESULT_AFTER_MERGE_MOCK);
            expect(shouldEmit).toEqual(false);
            done();
        });
    });

    it('should update the rows with the new data - when is anonymous template (Custom)', (done) => {
        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: SWAPPED_ROWS_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            }
        });

        service.updateOldRows({
            newRows: UPDATED_ROWS_MOCK,
            templateIdentifier: '11123',
            isAnonymousTemplate: true
        });

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows).toEqual(RESULT_AFTER_MERGE_MOCK);
            expect(shouldEmit).toEqual(false);
            done();
        });
    });

    it('should replace the rows with the new data - when is diffent template identifier', (done) => {
        // Here i just swapped the rows and changed the uuid as expected from the backend
        const swappedRows = [
            {
                ...ROWS_MINIMAL_MOCK[1],
                y: 0 // This sets the order of the rows
            },
            {
                ...ROWS_MINIMAL_MOCK[0],
                y: 1 // This sets the order of the rows
            }
        ];

        // Update the containers uuid simulating the backend
        const updatedRows: DotGridStackWidget[] = [
            {
                ...ROWS_MINIMAL_MOCK[1],
                id: 'random test 2',
                y: 0, // This sets the order of the rows
                subGridOpts: {
                    ...ROWS_MINIMAL_MOCK[1].subGridOpts,
                    children: ROWS_MINIMAL_MOCK[1].subGridOpts.children.map((col) => ({
                        ...col,
                        id: 'hello there 2',
                        containers: col.containers.map((child, i) => ({
                            ...child,
                            uuid: `${i + 1}` // 1 for the 0 index
                        }))
                    }))
                }
            },
            {
                ...ROWS_MINIMAL_MOCK[0],
                id: 'random test 1',
                y: 1, // This sets the order of the rows
                subGridOpts: {
                    ...ROWS_MINIMAL_MOCK[0].subGridOpts,
                    children: ROWS_MINIMAL_MOCK[0].subGridOpts.children.map((col) => ({
                        ...col,
                        id: 'hello there 1',
                        containers: col.containers.map((child, i) => ({
                            ...child,
                            uuid: `${i + 3}` // 1 for the 0 index and 2 for the first 2 containers
                        }))
                    }))
                }
            }
        ];

        service.setState({
            ...INITIAL_STATE_MOCK,
            rows: swappedRows,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            }
        });

        service.updateOldRows({ newRows: updatedRows, templateIdentifier: '222' }); //Different template identifier

        rows$.subscribe(({ rows, shouldEmit }) => {
            expect(rows).toEqual(updatedRows);
            expect(shouldEmit).toEqual(false);
            done();
        });
    });

    describe('defaultContainer', () => {
        it('should initialize with undefined defaultContainer', (done) => {
            expect.assertions(1);
            service.vm$.subscribe(({ defaultContainer }) => {
                expect(defaultContainer).toBeUndefined();
                done();
            });
        });

        it('should use defaultContainer when adding a new row', (done) => {
            expect.assertions(2);
            // Set state with defaultContainer
            service.setState({
                ...INITIAL_STATE_MOCK,
                rows: GRIDSTACK_DATA_MOCK,
                defaultContainer: mockDefaultContainerWithPath,
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: SIDEBAR_MOCK
                }
            });

            const mockRow: DotGridStackWidget = {
                styleClass: ['mock-class'],
                containers: [],
                y: 1
            };

            service.addRow(mockRow);

            rows$.subscribe(({ rows, shouldEmit }) => {
                const newRow = rows[rows.length - 1];
                expect(newRow.subGridOpts.children[0].containers[0].identifier).toBe(
                    mockDefaultContainerWithPath.path
                );
                expect(shouldEmit).toEqual(true);
                done();
            });
        });

        it('should use defaultContainer identifier when path is not available', (done) => {
            expect.assertions(1);
            // Set state with defaultContainer (no path property)
            service.setState({
                ...INITIAL_STATE_MOCK,
                rows: GRIDSTACK_DATA_MOCK,
                defaultContainer: mockDefaultContainerWithoutPath,
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: SIDEBAR_MOCK
                }
            });

            const mockRow: DotGridStackWidget = {
                styleClass: ['mock-class'],
                containers: [],
                y: 1
            };

            service.addRow(mockRow);

            rows$.subscribe(({ rows }) => {
                const newRow = rows[rows.length - 1];
                expect(newRow.subGridOpts.children[0].containers[0].identifier).toBe(
                    mockDefaultContainerWithoutPath.identifier
                );
                done();
            });
        });

        it('should use empty containers when defaultContainer is not set', (done) => {
            expect.assertions(1);
            // Ensure defaultContainer is undefined
            service.setState({
                ...INITIAL_STATE_MOCK,
                rows: GRIDSTACK_DATA_MOCK,
                defaultContainer: undefined,
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: SIDEBAR_MOCK
                }
            });

            const mockRow: DotGridStackWidget = {
                styleClass: ['mock-class'],
                containers: [],
                y: 1
            };

            service.addRow(mockRow);

            rows$.subscribe(({ rows }) => {
                const newRow = rows[rows.length - 1];
                expect(newRow.subGridOpts.children[0].containers).toEqual([]);
                done();
            });
        });

        it('should use defaultContainer when adding a new column', (done) => {
            expect.assertions(1);
            // Set state with defaultContainer
            service.setState({
                ...INITIAL_STATE_MOCK,
                rows: GRIDSTACK_DATA_MOCK,
                defaultContainer: mockDefaultContainerWithPath,
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: SIDEBAR_MOCK
                }
            });

            const parentId = GRIDSTACK_DATA_MOCK[0].id as string;
            const grid = {
                grid: {
                    parentGridItem: {
                        id: parentId
                    }
                }
            };

            const newColumn: DotGridStackWidget = {
                x: 0,
                y: 0,
                w: 3,
                id: uuid()
            };

            service.addColumn({ ...newColumn, ...grid } as DotGridStackNode);

            rows$.subscribe(({ rows }) => {
                const row = rows.find((item) => item.id === parentId);
                const addedColumn = row?.subGridOpts?.children.find(
                    (child) => child.id === newColumn.id
                );
                expect(addedColumn?.containers[0].identifier).toBe(
                    mockDefaultContainerWithPath.path
                );
                done();
            });
        });

        it('should update defaultContainer using updateDefaultContainer method', (done) => {
            expect.assertions(2);
            const newDefaultContainer: DotContainer = {
                identifier: 'new-default-container-id',
                name: 'New Default Container',
                type: 'containers',
                source: CONTAINER_SOURCE.FILE,
                live: true,
                working: true,
                deleted: false,
                locked: false,
                title: 'New Default Container Title',
                path: '/new/default/container/path',
                archived: false,
                categoryId: 'new-category',
                hostName: 'new-host'
            };

            service.updateDefaultContainer(newDefaultContainer);

            service.vm$.subscribe(({ defaultContainer, shouldEmit }) => {
                expect(defaultContainer).toEqual(newDefaultContainer);
                expect(shouldEmit).toEqual(true);
                done();
            });
        });

        it('should set defaultContainer to null using updateDefaultContainer method', (done) => {
            expect.assertions(2);
            // First set a defaultContainer
            service.setState({
                ...INITIAL_STATE_MOCK,
                defaultContainer: mockTempContainer
            });

            // Then set it to null
            service.updateDefaultContainer(null);

            service.vm$.subscribe(({ defaultContainer, shouldEmit }) => {
                expect(defaultContainer).toBeNull();
                expect(shouldEmit).toEqual(true);
                done();
            });
        });
    });

    describe('Util Methods', () => {
        describe('subGridOnDropped', () => {
            it('should execute moveColumnInYAxis when oldNode and newNode exist', (done) => {
                expect.assertions(1);
                jest.spyOn(service, 'moveColumnInYAxis').mockReturnValue(of('').subscribe());

                const oldNode: DotGridStackNode = {
                    x: 0,
                    y: 0,
                    w: 1,
                    id: uuid()
                };
                const newNode: DotGridStackNode = {
                    x: 0,
                    y: 0,
                    w: 1,
                    id: uuid()
                };

                service.subGridOnDropped(oldNode, newNode);

                expect(jest.mocked(service.moveColumnInYAxis).mock.calls).toHaveLength(1);
                done();
            });

            it('should execute addColumnInYAxis when oldNode is undefined', (done) => {
                jest.spyOn(service, 'addColumn').mockReturnValue(of('').subscribe());

                const newNode: DotGridStackNode = {
                    x: 0,
                    y: 0,
                    w: 1,
                    id: uuid()
                };

                service.subGridOnDropped(undefined, newNode);

                expect(jest.mocked(service.addColumn).mock.calls).toHaveLength(1);
                done();
            });
        });
    });
});
