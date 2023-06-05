import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackNode,
    GridStackWidget,
    numberOrString
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
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { gridOptions, subGridOptions } from './utils/gridstack-options';
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

    @ViewChildren('rowElement', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<ElementRef<TemplateBuilderRowComponent>>;

    @ViewChildren('boxElement', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<TemplateBuilderBoxComponent>>;

    grid!: GridStack;

    public readonly rowIcon = rowIcon;
    public readonly colIcon = colIcon;

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
                this.store.updateColumnGridStackData(nodes as DotGridStackWidget[]);
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
                // ref.nativeElement still says that is the Template Builder Box Component
                const nativeElement = ref.nativeElement as unknown as GridItemHTMLElement;

                if (!nativeElement.gridstackNode) {
                    const parentGrid = nativeElement.closest('.grid-stack') as GridHTMLElement;
                    const grid = parentGrid.gridstack as GridStack;
                    grid.makeWidget(nativeElement);
                }
            });
        });

        this.rows.changes.subscribe(() => {
            const layout: GridStackWidget[] = [];

            this.rows.forEach((ref) => {
                // ref.nativeElement still says that is the Template Builder Row Component
                const nativeElement = ref.nativeElement as unknown as GridItemHTMLElement;

                const isNew = !nativeElement.gridstackNode;

                const row =
                    nativeElement.gridstackNode ||
                    this.grid.makeWidget(nativeElement).gridstackNode;

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
                                this.store.updateColumnGridStackData(nodes as DotGridStackWidget[]);
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

    /**
     * @description This function maintains the GridStack Model in sync with the store when you delete a column
     *
     * @param {DotGridStackWidget} column
     * @param {numberOrString} rowID
     * @memberof TemplateBuilderComponent
     */
    removeColumn(
        column: DotGridStackWidget,
        element: GridHTMLElement,
        rowID: numberOrString
    ): void {
        // The gridstack model is polutted with the subgrid data
        // So we need to delete the node from the GridStack Model
        this.grid.engine.nodes.find((node) => node.id === rowID).subGrid?.removeWidget(element);

        this.store.removeColumn({ ...column, parentId: rowID as string });
    }
}
