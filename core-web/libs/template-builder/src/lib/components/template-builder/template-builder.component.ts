import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackElement,
    GridStackNode,
    GridStackOptions,
    GridStackWidget
} from 'gridstack';
import { Observable, Subject } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnInit,
    QueryList,
    ViewChildren
} from '@angular/core';

import { scan, startWith } from 'rxjs/operators';

type AddAction = (model: GridStackWidget[], payload: ActionPayload) => GridStackWidget[];
type MoveAction = (model: GridStackWidget[], payload: ActionPayload[]) => GridStackWidget[];

type WidgetType = {
    col: ActionType;
    row: ActionType;
};

type ActionType = {
    add: AddAction;
    yMove: MoveAction;
    change?: MoveAction;
};

const WidgetActions: WidgetType = {
    col: {
        add: function (model: GridStackWidget[], payload: ActionPayload): GridStackWidget[] {
            return model.map((item) => {
                if (
                    payload &&
                    payload.parentId !== undefined &&
                    item &&
                    item.id !== undefined &&
                    item.id === payload.parentId
                ) {
                    return {
                        ...item,
                        subGridOpts: {
                            ...item.subGridOpts,
                            children: [...(item.subGridOpts?.children || []), payload]
                        }
                    };
                }

                return item;
            });
        },
        yMove: function (model: GridStackWidget[], changes: ActionPayload[]): GridStackWidget[] {
            const [nodeToDelete, nodeToAdd] = changes;

            return model.map((item) => {
                if (item.id === nodeToDelete.parentId) {
                    return {
                        ...item,
                        subGridOpts: {
                            ...item.subGridOpts,
                            children: item.subGridOpts?.children?.filter(
                                (child) => child.id !== nodeToDelete.id
                            ) // Filter the moved node
                        }
                    };
                } else if (item.id === nodeToAdd.parentId) {
                    return {
                        ...item,
                        subGridOpts: {
                            ...item.subGridOpts,
                            children: [...(item.subGridOpts?.children || []), nodeToAdd] // Add the node
                        }
                    };
                }

                return item;
            });
        },
        change: function (model: GridStackWidget[], changes: ActionPayload[]): GridStackWidget[] {
            return model.map((item) => {
                if (item.id == changes[0].parentId)
                    return {
                        ...item,
                        subGridOpts: {
                            ...item.subGridOpts,
                            children: item.subGridOpts?.children?.map((child) => {
                                const newChild = changes.find((change) => change.id === child.id);

                                if (newChild) {
                                    return {
                                        ...child,
                                        ...newChild
                                    };
                                }

                                return child;
                            })
                        }
                    };

                return item;
            });
        }
    },
    row: {
        add: function (model: GridStackWidget[], payload: ActionPayload): GridStackWidget[] {
            // When you add a widget, you add it to the grid and move the other ones
            return [...model, payload];
        },
        yMove: function (model: GridStackWidget[], changes: ActionPayload[]): GridStackWidget[] {
            changes.forEach(({ y, id }) => {
                const rowIndex = model.findIndex((item) => item.id === id);
                // So here I update the positions of the changed ones
                if (rowIndex > -1) model[rowIndex] = { ...model[rowIndex], y };
            });

            return model;
        }
    }
};

/**
 * Check if the element is a column widget by checking the data-widget-type attribute
 *
 * @param {Element} el
 * @return {*}  {boolean}
 */
function isAColumnWidget(el: Element): boolean {
    return el.getAttribute('data-widget-type') === 'col' || el.classList.contains('sub');
}

/**
 * Check if the element is a row widget by checking the data-widget-type attribute
 *
 * @param {Element} el
 * @return {*}  {boolean}
 */
function isARowWidget(el: Element): boolean {
    return el.getAttribute('data-widget-type') === 'row';
}

let ids = 4;

const subOptions: GridStackOptions = {
    cellHeight: 85,
    column: 'auto',
    margin: 10,
    minRow: 1,
    maxRow: 1,
    acceptWidgets: isAColumnWidget
};

interface ActionPayload extends GridStackWidget {
    parentId?: string;
}

type Action = {
    widgetType: 'row' | 'col';
    actionType: 'add' | 'yMove' | 'change';
    payload: ActionPayload | ActionPayload[];
};

@Component({
    selector: 'template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderComponent implements OnInit, AfterViewInit {
    private mySub$ = new Subject<Action>();

    public items$: Observable<GridStackWidget[]>;

    @ViewChildren('rows', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<ElementRef<GridItemHTMLElement>>;

    @ViewChildren('boxes', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<GridItemHTMLElement>>;

    grid!: GridStack;

    constructor(private cd: ChangeDetectorRef) {
        const starter: GridStackWidget[] = [
            { x: 0, y: 0, w: 12, id: '1' },
            { x: 0, y: 1, w: 12, id: '2' },
            {
                x: 0,
                y: 2,
                w: 12,
                id: '3',
                subGridOpts: {
                    children: [{ x: 0, y: 0, w: 4, id: String(ids++) }]
                }
            }
        ];

        // HERE ARE CHANGES
        this.items$ = this.mySub$.pipe(
            scan((acc: GridStackWidget[], { widgetType, actionType, payload }: Action) => {
                const newAcc = structuredClone(acc);

                let widgetAction: AddAction | MoveAction;

                if (actionType == 'add') {
                    widgetAction = WidgetActions[widgetType].add;

                    return widgetAction(newAcc, payload as ActionPayload);
                } else if (actionType == 'yMove') {
                    widgetAction = WidgetActions[widgetType].yMove;

                    return widgetAction(newAcc, payload as ActionPayload[]);
                } else if (actionType == 'change') {
                    widgetAction = WidgetActions[widgetType].change as MoveAction;

                    return widgetAction(newAcc, payload as ActionPayload[]);
                }

                return newAcc;
            }, starter),
            startWith(starter)
        );
    }

    ngOnInit() {
        GridStack.setupDragIn('.add', { appendTo: 'body', helper: 'clone' });
    }

    ngAfterViewInit() {
        this.grid = GridStack.init({
            disableResize: true,
            cellHeight: 100,
            margin: 10,
            minRow: 1,
            acceptWidgets: isARowWidget
        }).on('change', (_: Event, nodes: GridStackNode[]) => {
            const action: Action = {
                widgetType: 'row',
                actionType: 'yMove',
                payload: nodes.map((node) => ({
                    y: node.y,
                    id: node.id as string,
                    parentId: node.grid?.parentGridItem?.id as string
                }))
            };
            this.mySub$.next(action);
        });

        // Adding subgrids on load
        // HERE ARE CHANGES
        Array.from(this.grid.el.querySelectorAll('.grid-stack')).forEach((el) => {
            const subgrid = GridStack.addGrid(el as HTMLElement, subOptions);

            subgrid.on('change', (event: Event, nodes: GridStackNode[]) => {
                const action: Action = {
                    widgetType: 'col',
                    actionType: 'change',
                    payload: nodes.map((node) => ({
                        x: node.x,
                        id: node.id as string,
                        parentId: node.grid?.parentGridItem?.id as string,
                        w: node.w
                    }))
                };
                this.mySub$.next(action);
            });
            subgrid.on('dropped', (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                // If there's an old one and a new one, then that means that a node moved
                if (oldNode && newNode) {
                    const action: Action = {
                        widgetType: 'col',
                        actionType: 'yMove',
                        payload: [oldNode, newNode].map((node, i) => ({
                            parentId: node.grid?.parentGridItem?.id as string,
                            w: node.w,
                            h: node.h,
                            x: node.x,
                            y: node.y,
                            id: i ? String(ids++) : node.id
                        }))
                    };
                    this.mySub$.next(action);
                } else {
                    const action: Action = {
                        widgetType: 'col',
                        actionType: 'add',
                        payload: {
                            parentId: newNode.grid?.parentGridItem?.id as string,
                            w: newNode.w,
                            h: newNode.h,
                            x: newNode.x,
                            y: newNode.y,
                            id: String(ids++)
                        }
                    };
                    this.mySub$.next(action);

                    newNode.grid?.removeWidget(newNode.el as GridStackElement, true);
                }
            });
        });
        this.grid.on('dropped', (_: Event, previousNode: GridStackNode, newNode: GridStackNode) => {
            // console.log('grid dropped', previousNode);
            if (!newNode.el || previousNode) return;

            newNode.grid?.removeWidget(newNode.el, true, false);

            // HERE ARE CHANGES
            this.mySub$.next({
                widgetType: 'row',
                actionType: 'add',
                payload: {
                    w: 12,
                    h: 1,
                    x: 1,
                    y: newNode.y,
                    id: String(ids++),
                    subGridOpts: {
                        oneColumnSize: 320,
                        maxRow: 1,
                        cellHeight: 78,
                        children: [{ x: 0, y: 0, w: 4, id: String(ids++) }]
                    }
                }
            });
        });

        this.boxes.changes.subscribe(() => {
            this.boxes.forEach((ref) => {
                if (!ref.nativeElement.gridstackNode) {
                    const parentGrid = ref.nativeElement.closest('.grid-stack') as GridHTMLElement;
                    const grid = parentGrid.gridstack as GridStack;
                    grid.makeWidget(ref.nativeElement);
                }
            });
        });

        this.rows.changes.subscribe(() => {
            // console.log('rows changed');
            const layout: GridStackWidget[] = [];

            this.rows.forEach((ref) => {
                const isNew = !ref.nativeElement.gridstackNode;

                const row =
                    ref.nativeElement.gridstackNode ||
                    this.grid.makeWidget(ref.nativeElement).gridstackNode;

                if (row && row.el) {
                    if (isNew) {
                        const newGridElement = row.el.querySelector('.grid-stack') as HTMLElement;

                        // Adding subgrids on drop row
                        GridStack.addGrid(newGridElement, subOptions).on(
                            'dropped',
                            (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                                // If there's an old one and a new one, then that means that a node moved
                                if (oldNode && newNode) {
                                    const action: Action = {
                                        widgetType: 'col',
                                        actionType: 'yMove',
                                        payload: [oldNode, newNode].map((node, i) => ({
                                            parentId: node.grid?.parentGridItem?.id as string,
                                            w: node.w,
                                            h: node.h,
                                            x: node.x,
                                            y: node.y,
                                            id: i ? String(ids++) : node.id
                                        }))
                                    };
                                    this.mySub$.next(action);
                                } else {
                                    const action: Action = {
                                        widgetType: 'col',
                                        actionType: 'add',
                                        payload: {
                                            parentId: newNode.grid?.parentGridItem?.id as string,
                                            w: newNode.w,
                                            h: newNode.h,
                                            x: newNode.x,
                                            y: newNode.y,
                                            id: String(ids++)
                                        }
                                    };
                                    this.mySub$.next(action);

                                    newNode.grid?.removeWidget(
                                        newNode.el as GridStackElement,
                                        true
                                    );
                                }
                            }
                        );
                    }

                    layout.push(row);
                }
            });

            this.grid.load(layout); // efficient that does diffs only
        });
    }

    identify(index: number, w: GridStackWidget) {
        return w.id;
    }
}
