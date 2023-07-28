import { expect, jest, describe } from '@jest/globals';
import { Observable, of } from 'rxjs';
import { v4 as uuid } from 'uuid';

import { TestBed } from '@angular/core/testing';

import { pluck, take } from 'rxjs/operators';

import { DotContainerMap } from '@dotcms/dotcms-models';
import { containersMock } from '@dotcms/utils-testing';

import { DotTemplateBuilderStore } from './template-builder.store';

import {
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateLayoutProperties,
    SYSTEM_CONTAINER_IDENTIFIER
} from '../models/models';
import {
    GRIDSTACK_DATA_MOCK,
    mockTemplateBuilderContainer,
    SIDEBAR_MOCK,
    STYLE_CLASS_MOCK
} from '../utils/mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

describe('DotTemplateBuilderStore', () => {
    let service: DotTemplateBuilderStore;
    let rows$: Observable<DotGridStackWidget[]>;
    let layoutProperties$: Observable<DotTemplateLayoutProperties>;
    let containerMap$: Observable<DotContainerMap>;
    let initialState: DotGridStackWidget[];
    const mockContainer = containersMock[0];
    const minDataMockContainer = {
        identifier: mockContainer.identifier
    };

    const addContainer = () => {
        const parentRow = initialState[0];

        const columnToAddContainer: DotGridStackWidget = {
            ...parentRow.subGridOpts?.children[0],
            parentId: parentRow.id as string
        };
        service.addContainer({ affectedColumn: columnToAddContainer, container: mockContainer });
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
        service.init({
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                header: true,
                footer: true,
                sidebar: SIDEBAR_MOCK
            },
            resizingRowID: '',
            containerMap: {}
        });

        // Get the initial state
        rows$.pipe(take(1)).subscribe((items) => {
            initialState = structuredClone(items); // To lose the reference
        });
    });

    it('should be created', () => {
        expect.assertions(1);
        expect(service).toBeTruthy();
    });

    it('should initialize the state', (done) => {
        expect.assertions(1);
        rows$.subscribe((items) => {
            expect(items).toEqual(initialState);
            done();
        });
    });

    it('should add a new row', (done) => {
        expect.assertions(3);
        const mockRow: DotGridStackWidget = {
            styleClass: ['mock-class'],
            containers: [],
            y: 1
        };

        service.addRow(mockRow);

        rows$.subscribe((items) => {
            expect(items.length).toBeGreaterThan(initialState.length);
            expect(items[3].subGridOpts.children[0].w).toBe(3);
            expect(items[3].subGridOpts.children[0].containers[0].identifier).toBe(
                SYSTEM_CONTAINER_IDENTIFIER
            );
            done();
        });
    });

    it('should move a row', (done) => {
        expect.assertions(1);
        const mockAffectedRows: DotGridStackWidget[] = [
            { ...initialState[1], y: 0 },
            { ...initialState[0], y: 1 }
        ];

        service.moveRow(mockAffectedRows);

        rows$.subscribe((items) => {
            expect(items[0].y).toEqual(initialState[1].y);
            done();
        });
    });

    it('should remove a row', (done) => {
        expect.assertions(1);
        const rowToDelete = initialState[0];

        const toDeleteID = rowToDelete.id;

        service.removeRow(toDeleteID as string);

        rows$.subscribe((items) => {
            expect(items).not.toContainEqual(rowToDelete);
            done();
        });
    });

    it('should update a row', (done) => {
        expect.assertions(1);
        const updatedRow: DotGridStackWidget = {
            ...initialState[0],
            styleClass: ['new-class', 'flex-mock'],
            containers: [{ identifier: 'mock-container', uuid: uuid() }]
        };

        service.updateRow(updatedRow);
        rows$.subscribe((items) => {
            expect(items[0]).toEqual(updatedRow);
            done();
        });
    });

    it('should update the rowResizingID', (done) => {
        expect.assertions(1);
        const rowId = uuid();
        service.setResizingRowID(rowId);
        service.vm$.subscribe(({ resizingRowID }) => {
            expect(resizingRowID).toEqual(rowId);
            done();
        });
    });

    it('should clean the rowResizingID', (done) => {
        expect.assertions(1);
        const rowId = uuid();
        service.setResizingRowID(rowId);
        service.setResizingRowID(null);
        service.vm$.subscribe(({ resizingRowID }) => {
            expect(resizingRowID).toEqual(null);
            done();
        });
    });

    it('should add a column', (done) => {
        expect.assertions(1);
        const parentId = initialState[0].id as string;

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

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toContainEqual({
                x: newColumn.x,
                y: newColumn.y,
                w: newColumn.w,
                id: newColumn.id,
                parentId: parentId,
                styleClass: undefined,
                containers: [
                    {
                        identifier: SYSTEM_CONTAINER_IDENTIFIER
                    }
                ]
            });
            done();
        });
    });

    it('should move a column in the Y-axis', (done) => {
        expect.assertions(2);
        const fromRow = initialState[2];
        const toRow = initialState[0];

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

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === newParent);
            const oldRow = items.find((item) => item.id === oldParent);

            expect(row?.subGridOpts?.children.length).toBeGreaterThan(
                toRow.subGridOpts?.children.length || 0
            );
            expect(oldRow?.subGridOpts?.children.length).toBeLessThan(
                fromRow.subGridOpts?.children.length as number
            );
            done();
        });
    });

    it('should update gridStack data of a column', (done) => {
        expect.assertions(1);
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
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            },
            resizingRowID: '',
            containerMap: {}
        });

        const newWidth = 2;
        const affectedColumns: DotGridStackNode[] = [
            {
                ...firstBox,
                w: newWidth
            }
        ];

        service.updateColumnGridStackData(affectedColumns);

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row.subGridOpts.children[0].w).toEqual(newWidth);
            done();
        });
    });
    it('should update styleClass data of a column', (done) => {
        expect.assertions(1);
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
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                footer: false,
                header: false,
                sidebar: {}
            },
            resizingRowID: '',
            containerMap: {}
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

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children.map((child) => child.styleClass)).toContainEqual(
                STYLE_CLASS_MOCK
            );
            done();
        });
    });

    it('should remove a column', (done) => {
        expect.assertions(1);
        const parentRow = initialState[2];

        const columnToDelete: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            parentId: parentRow.id as string
        };

        service.removeColumn(columnToDelete);

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children).not.toContain(columnToDelete);
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

        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === initialState[0].id);
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
        const parentRow = initialState[0];

        const columnToDeleteContainer: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            containers: [mockTemplateBuilderContainer],
            parentId: parentRow.id as string
        };
        service.deleteContainer({
            affectedColumn: columnToDeleteContainer,
            containerIndex: 0
        });
        rows$.subscribe((items) => {
            const row = items.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children[0].containers).not.toContain(
                mockTemplateBuilderContainer
            );
            done();
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
