import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackNode,
    GridStackWidget
} from 'gridstack';
import { Observable } from 'rxjs';
import { v4 as uuid } from 'uuid';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChildren
} from '@angular/core';

import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { gridOptions, subGridOptions } from './utils/gridstack-options';

@Component({
    selector: 'template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
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

    constructor(private store: DotTemplateBuilderStore) {
        const starter: DotGridStackWidget[] = [
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

        this.store.init(starter);

        this.items$ = this.store.items$;
    }

    ngOnInit() {
        GridStack.setupDragIn('.add', {
            appendTo: 'body',
            helper: 'clone'
        });
    }

    ngAfterViewInit() {
        this.grid = GridStack.init(gridOptions).on('change', (_: Event, nodes: GridStackNode[]) => {
            this.store.moveRow(nodes as DotGridStackWidget[]);
        });

        // Adding subgrids on load
        Array.from(this.grid.el.querySelectorAll('.grid-stack')).forEach((el) => {
            const subgrid = GridStack.addGrid(el as HTMLElement, subGridOptions);

            subgrid.on('change', (_: Event, nodes: GridStackNode[]) => {
                this.store.updateColumn(nodes as DotGridStackWidget[]);
            });
            subgrid.on('dropped', (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                this.store.subGridOnDropped(oldNode, newNode);
            });
        });

        this.grid.on('dropped', (_: Event, previousNode: GridStackNode, newNode: GridStackNode) => {
            if (!newNode.el || previousNode) return;

            newNode.grid?.removeWidget(newNode.el, true, false);

            this.store.addRow({
                y: newNode.y
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
                        GridStack.addGrid(newGridElement, subGridOptions).on(
                            'dropped',
                            (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                                this.store.subGridOnDropped(oldNode, newNode);
                            }
                        );
                    }

                    layout.push(row);
                }
            });

            this.grid.load(layout); // efficient that does diffs only
        });
    }

    ngOnDestroy(): void {
        this.grid.destroy(true);
    }

    identify(_: number, w: GridStackWidget) {
        return w.id;
    }
}
