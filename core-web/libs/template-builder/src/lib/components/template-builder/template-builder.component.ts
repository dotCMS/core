import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackNode,
    GridStackWidget
} from 'gridstack';
import { Observable } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Input,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChildren
} from '@angular/core';

import { DotLayout } from '@dotcms/dotcms-models';

import { colIcon, rowIcon } from './assets/icons';
import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import {
    GRID_STACK_ROW_HEIGHT,
    GRID_STACK_UNIT,
    gridOptions,
    subGridOptions
} from './utils/gridstack-options';
import { parseFromDotObjectToGridStack } from './utils/gridstack-utils';

@Component({
    selector: 'dotcms-template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
    @Input()
    templateLayout!: DotLayout;

    public items$: Observable<DotGridStackWidget[]>;

    @ViewChildren('rows', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<ElementRef<GridItemHTMLElement>>;

    @ViewChildren('boxes', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<GridItemHTMLElement>>;

    grid!: GridStack;

    public readonly rowIcon = rowIcon;
    public readonly colIcon = colIcon;
    public readonly rowDisplayHeight = `${GRID_STACK_ROW_HEIGHT - 1}${GRID_STACK_UNIT}`; // setting a lower height to have space between rows

    constructor(private store: DotTemplateBuilderStore) {
        this.items$ = this.store.items$;
    }

    ngOnInit(): void {
        this.store.init(parseFromDotObjectToGridStack(this.templateLayout.body));
    }

    ngAfterViewInit() {
        this.grid = GridStack.init(gridOptions).on('change', (_: Event, nodes: GridStackNode[]) => {
            this.store.moveRow(nodes as DotGridStackWidget[]);
        });

        GridStack.setupDragIn('dotcms-add-widget', {
            appendTo: 'body',
            helper: 'clone'
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
                        GridStack.addGrid(newGridElement, subGridOptions)
                            .on(
                                'dropped',
                                (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                                    this.store.subGridOnDropped(oldNode, newNode);
                                }
                            )
                            .on('change', (_: Event, nodes: GridStackNode[]) => {
                                this.store.updateColumn(nodes as DotGridStackWidget[]);
                            });
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

    deleteRow(id: string): void {
        this.store.removeRow(id);
    }
}
