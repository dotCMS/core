import { expect, jest, describe } from '@jest/globals';
import { of } from 'rxjs';
import { v4 as uuid } from 'uuid';

import { TestBed } from '@angular/core/testing';

import { DotTemplateBuilderStore } from './template-builder.store';

import { DotGridStackNode, DotGridStackWidget } from '../models/models';
import {
    createDotGridStackWidgetFromNode,
    createDotGridStackWidgets,
    getColumnByID,
    getIndexRowInItems,
    parseMovedNodeToWidget,
    removeColumnByID
} from '../utils/gridstack-utils';

jest.mock('../utils/gridstack-utils');
jest.mock('uuid', () => ({
    v4: jest.fn().mockReturnValue(Math.random().toString(36).slice(2, 9))
}));

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

const initialState: DotGridStackWidget[] = [
    { x: 0, y: 0, w: 12, id: uuid() },
    { x: 0, y: 1, w: 12, id: uuid() },
    {
        x: 0,
        y: 2,
        w: 12,
        id: uuid(),
        subGridOpts: {
            children: [{ x: 0, y: 0, w: 4, id: uuid() }]
        }
    }
];

describe('DotTemplateBuilderStore', () => {
    let service: DotTemplateBuilderStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotTemplateBuilderStore]
        });
        service = TestBed.inject(DotTemplateBuilderStore);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should initialize the state', () => {
        service.init(initialState);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items).toEqual(initialState);
        });
    });

    it('should add a new row', () => {
        const mockRow: DotGridStackWidget = {
            styleClass: ['mock-class'],
            containers: []
        };
        (uuid as jest.Mock).mockReturnValue('mock-uuid');
        service.addRow(mockRow);

        service.items$.subscribe((items) => {
            expect(items).toContainEqual({
                ...mockRow,
                h: 1,
                w: 12,
                x: 0,
                id: 'mock-uuid',
                subGridOpts: { children: [] }
            });
        });
    });

    it('should move a row', () => {
        service.init(initialState);

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
        const toDeleteID = initialState[0].id;

        service.init(initialState);

        service.removeRow(toDeleteID as string);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            expect(items).not.toContainEqual(initialState[0]);
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

        service.init(initialState);

        const newColumn: DotGridStackNode = {
            parentId,
            x: 0,
            y: 0,
            w: 4,
            id: uuid()
        };

        (createDotGridStackWidgetFromNode as jest.Mock).mockReturnValue(newColumn);

        service.addColumn(newColumn);

        expect.assertions(1);

        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toContainEqual(newColumn);
        });
    });

    it('should move a column in the Y-axis', () => {
        const oldParent = initialState[2].id as string;
        const newParent = initialState[0].id as string;

        const node: DotGridStackNode = initialState[2].subGridOpts?.children[0] as DotGridStackNode;

        const columnToDelete = {
            ...node,
            parentId: oldParent
        };
        const columnToAdd = {
            ...node,
            parentId: newParent,
            id: uuid()
        };

        service.init(initialState);

        (parseMovedNodeToWidget as jest.Mock).mockReturnValue([columnToDelete, columnToAdd]);
        (getIndexRowInItems as jest.Mock).mockReturnValue(2);
        (getColumnByID as jest.Mock).mockReturnValue(columnToDelete);
        (removeColumnByID as jest.Mock).mockReturnValue(
            initialState[2].subGridOpts?.children.filter((item) => item.id !== columnToDelete.id)
        );

        service.moveColumnInYAxis([node, node]);

        expect.assertions(2);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === newParent);
            expect(row?.subGridOpts?.children).toContainEqual(columnToAdd);
            expect(row?.subGridOpts?.children).not.toContainEqual(columnToDelete);
        });
    });

    it('should update a column', () => {
        const parentId = uuid();
        const [firstId, secondId, thirdId, fourthId] = [1, 2, 3, 4].map(() => uuid());

        const mockInitialState = [
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

        service.init(mockInitialState);

        const affectedColumns: DotGridStackNode[] = [
            {
                x: 1,
                y: 0,
                w: 1,
                id: firstId,
                containers: [{ identifier: 'mock-container', uuid: uuid() }],
                styleClass: ['mock-styles']
            },
            { x: 0, y: 0, w: 1, id: secondId },
            { x: 3, y: 0, w: 1, id: thirdId },
            { x: 4, y: 0, w: 5, id: fourthId }
        ];
        const createdWidgets = affectedColumns.map((column) => ({
            ...column,
            parentId
        }));

        (createDotGridStackWidgets as jest.Mock).mockReturnValue(createdWidgets);
        service.updateColumn(affectedColumns);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === parentId);
            expect(row?.subGridOpts?.children).toEqual(createdWidgets);
        });
    });

    it('should remove a column', () => {
        const columnToDelete: DotGridStackWidget = {
            ...(initialState[2].subGridOpts?.children[0] as DotGridStackWidget),
            parentId: initialState[2].id as string
        };

        (removeColumnByID as jest.Mock).mockReturnValue(
            initialState[2].subGridOpts?.children.filter((item) => item.id !== columnToDelete.id)
        );

        service.init(initialState);

        service.removeColumn(columnToDelete);

        expect.assertions(1);
        service.items$.subscribe((items) => {
            const row = items.find((item) => item.id === initialState[2].id);

            expect(row?.subGridOpts?.children).not.toContain(columnToDelete);
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
