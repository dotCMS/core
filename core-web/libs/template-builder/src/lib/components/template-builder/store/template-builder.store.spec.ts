import { expect, jest, describe } from '@jest/globals';
import { of } from 'rxjs';
import { v4 as uuid } from 'uuid';

import { TestBed } from '@angular/core/testing';

import { take } from 'rxjs/operators';

import { containersMock } from '@dotcms/utils-testing';

import { DotTemplateBuilderStore } from './template-builder.store';

import { DotGridStackNode, DotGridStackWidget } from '../models/models';
import { GRIDSTACK_DATA_MOCK, mockTemplateBuilderContainer } from '../utils/mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

describe('DotTemplateBuilderStore', () => {
    let service: DotTemplateBuilderStore;
    let initialState: DotGridStackWidget[];
    const mockContainer = containersMock[0];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotTemplateBuilderStore]
        });
        service = TestBed.inject(DotTemplateBuilderStore);

        // Reset the state because is manipulated by reference
        service.init(GRIDSTACK_DATA_MOCK);

        // Get the initial state
        service.items$.pipe(take(1)).subscribe((items) => {
            initialState = structuredClone(items); // To lose the reference
        });
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should initialize the state', () => {
        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items).toEqual(initialState);
        });
    });

    it('should add a new row', () => {
        const mockRow: DotGridStackWidget = {
            styleClass: ['mock-class'],
            containers: [],
            y: 1
        };

        service.addRow(mockRow);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items.length).toBeGreaterThan(initialState.length);
        });
    });

    it('should move a row', () => {
        const mockAffectedRows: DotGridStackWidget[] = [
            { x: 0, y: 1, w: 12, id: uuid() },
            { x: 0, y: 2, w: 12, id: uuid() },
            {
                x: 0,
                y: 0,
                w: 12,
                id: uuid(),
                subGridOpts: {
                    children: [{ x: 0, y: 0, w: 4, id: uuid() }]
                }
            }
        ];

        service.moveRow(mockAffectedRows);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items[0]).toEqual(mockAffectedRows[0]);
        });
    });

    it('should remove a row', () => {
        const rowToDelete = initialState[0];

        const toDeleteID = rowToDelete.id;

        service.removeRow(toDeleteID as string);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items).not.toContainEqual(rowToDelete);
        });
    });

    it('should update a row', () => {
        const updatedRow: DotGridStackWidget = {
            ...initialState[0],
            styleClass: ['new-class', 'flex-mock'],
            containers: [{ identifier: 'mock-container', uuid: uuid() }]
        };

        service.updateRow(updatedRow);
        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items[0]).toEqual(updatedRow);
        });
    });

    it('should add a column', () => {
        const parentId = initialState[0].id as string;

        const newColumn: unknown = {
            grid: {
                parentGridItem: {
                    id: parentId
                }
            },
            x: 0,
            y: 0,
            w: 4,
            id: uuid()
        };

        service.addColumn(newColumn as DotGridStackNode);

        expect.assertions(1);

        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toContainEqual(newColumn);
        });
    });

    it('should move a column in the Y-axis', () => {
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

        expect.assertions(2);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === newParent);
            const oldRow = items.find((item) => item.id === oldParent);

            expect(row?.subGridOpts?.children.length).toBeGreaterThan(
                toRow.subGridOpts?.children.length || 0
            );
            expect(oldRow?.subGridOpts?.children.length).toBeLessThan(
                fromRow.subGridOpts?.children.length as number
            );
        });
    });

    it('should update gridStack data of a column', () => {
        const parentId = uuid();
        const [firstId, secondId, thirdId, fourthId] = [1, 2, 3, 4].map(() => uuid());

        const GRIDSTACK_DATA_MOCK = [
            {
                x: 0,
                y: 0,
                w: 12,
                id: parentId,
                subGridOpts: {
                    children: [
                        { x: 0, y: 0, w: 1, id: firstId },
                        { x: 1, y: 0, w: 1, id: secondId },
                        { x: 2, y: 0, w: 1, id: thirdId },
                        { x: 3, y: 0, w: 1, id: fourthId }
                    ]
                }
            }
        ];

        service.setState({ items: GRIDSTACK_DATA_MOCK });

        const affectedColumns: DotGridStackNode[] = [
            {
                x: 1,
                y: 0,
                w: 1,
                id: firstId
            },
            { x: 0, y: 0, w: 1, id: secondId },
            { x: 3, y: 0, w: 1, id: thirdId },
            { x: 4, y: 0, w: 5, id: fourthId }
        ];
        const createdWidgets = affectedColumns.map((column) => ({
            ...column,
            parentId
        }));

        service.updateColumnGridStackData(affectedColumns);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toEqual(createdWidgets);
        });
    });
    it('should update styleClass data of a column', () => {
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

        service.setState({ items: GRIDSTACK_DATA_MOCK });

        const affectedColumn: DotGridStackNode = {
            x: 1,
            y: 0,
            w: 1,
            id: firstId,
            styleClass: ['test', 'mock-class'],
            parentId
        };

        service.updateColumnStyleClasses(affectedColumn);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toContainEqual(affectedColumn);
        });
    });

    it('should remove a column', () => {
        const parentRow = initialState[2];

        const columnToDelete: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            parentId: parentRow.id as string
        };

        service.removeColumn(columnToDelete);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children).not.toContain(columnToDelete);
        });
    });

    it('should add a container to specific box', () => {
        const parentRow = initialState[2];

        const columnToAddContainer: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            parentId: parentRow.id as string
        };
        service.addContainer({ affectedColumn: columnToAddContainer, container: mockContainer });
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children[0].containers).toContain(mockContainer);
        });
    });

    it('should delete a container from specific box', () => {
        const parentRow = initialState[2];

        const columnToAddContainer: DotGridStackWidget = {
            ...(parentRow.subGridOpts?.children[0] as DotGridStackWidget),
            containers: [mockTemplateBuilderContainer],
            parentId: parentRow.id as string
        };
        service.deleteContainer({
            affectedColumn: columnToAddContainer,
            containerIndex: 0
        });
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentRow.id);

            expect(row?.subGridOpts?.children[0].containers).not.toContain(
                mockTemplateBuilderContainer
            );
        });
    });

    describe('Util Methods', () => {
        describe('subGridOnDropped', () => {
            it('should execute moveColumnInYAxis when oldNode and newNode exist', () => {
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
            });

            it('should execute addColumnInYAxis when oldNode is undefined', () => {
                jest.spyOn(service, 'addColumn').mockReturnValue(of('').subscribe());

                const newNode: DotGridStackNode = {
                    x: 0,
                    y: 0,
                    w: 1,
                    id: uuid()
                };

                service.subGridOnDropped(undefined, newNode);

                expect(jest.mocked(service.addColumn).mock.calls).toHaveLength(1);
            });
        });
    });
});
