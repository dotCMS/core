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
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    ViewChildren
} from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { filter, take, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotContainer, DotLayout, DotLayoutSideBar } from '@dotcms/dotcms-models';

import { colIcon, rowIcon } from './assets/icons';
import { AddStyleClassesDialogComponent } from './components/add-style-classes-dialog/add-style-classes-dialog.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import {
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateBuilderState,
    DotTemplateLayoutProperties
} from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import {
    GRID_STACK_ROW_HEIGHT,
    GRID_STACK_UNIT,
    gridOptions,
    subGridOptions
} from './utils/gridstack-options';
import {
    parseFromDotObjectToGridStack,
    parseFromGridStackToDotObject
} from './utils/gridstack-utils';

@Component({
    selector: 'dotcms-template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
    @Input()
    templateLayout!: DotLayout;

    get layoutProperties(): DotTemplateLayoutProperties {
        return {
            header: this.templateLayout.header,
            footer: this.templateLayout.footer,
            sidebar: this.templateLayout.sidebar
        };
    }
    get sidebarProperties(): DotLayoutSideBar {
        return this.templateLayout.sidebar;
    }

    @Output()
    layoutChange: EventEmitter<Partial<DotLayout>> = new EventEmitter<DotLayout>();

    public items$: Observable<DotGridStackWidget[]>;
    public vm$: Observable<DotTemplateBuilderState>;

    @ViewChildren('rowElement', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<TemplateBuilderRowComponent>;

    @ViewChildren('boxElement', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<GridItemHTMLElement>>;

    grid!: GridStack;

    ref: DynamicDialogRef;

    public readonly rowIcon = rowIcon;
    public readonly colIcon = colIcon;
    public readonly rowDisplayHeight = `${GRID_STACK_ROW_HEIGHT - 1}${GRID_STACK_UNIT}`; // setting a lower height to have space between rows

    constructor(
        private store: DotTemplateBuilderStore,
        private dialogService: DialogService,
        private dotMessage: DotMessageService
    ) {
        this.vm$ = this.store.vm$.pipe(
            tap(({ items, layoutProperties }) => {
                if (!items.length) {
                    return;
                }

                const body = parseFromGridStackToDotObject(items);
                this.layoutChange.emit({
                    ...layoutProperties,
                    body
                });
            })
        );
    }

    ngOnInit(): void {
        this.store.init({
            items: parseFromDotObjectToGridStack(this.templateLayout.body),
            layoutProperties: this.layoutProperties
        });
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

    /**
     * @description This method is used to identify items by id
     *
     * @param {number} _
     * @param {GridStackWidget} w
     * @return {*}
     * @memberof TemplateBuilderComponent
     */
    identify(_: number, w: GridStackWidget): string {
        return w.id as string;
    }

    /**
     * @description This method maintains the GridStack Model in sync with the store when you delete a column
     *
     * @param {DotGridStackWidget} column
     * @param {numberOrString} rowID
     * @memberof TemplateBuilderComponent
     */
    removeColumn(
        column: DotGridStackWidget,
        element: GridItemHTMLElement,
        rowID: numberOrString
    ): void {
        // The gridstack model is polutted with the subgrid data
        // So we need to delete the node from the GridStack Model
        this.grid.engine.nodes.find((node) => node.id === rowID).subGrid?.removeWidget(element);

        this.store.removeColumn({ ...column, parentId: rowID as string });
    }

    /**
     * @description This method deletes the row from the store
     *
     * @param {numberOrString} id
     * @memberof TemplateBuilderComponent
     */
    deleteRow(id: numberOrString): void {
        this.store.removeRow(id as string);
    }

    /**
     * @description This method is used to update the layout properties
     *
     * @param {DotTemplateLayoutProperties} layoutProperties
     * @memberof TemplateBuilderComponent
     */
    layoutPropertiesChange(layoutProperties: DotTemplateLayoutProperties): void {
        this.store.updateLayoutProperties(layoutProperties);
    }

    /**
     * @description This method is used to update the sidebar width
     *
     * @param {string} width
     * @memberof TemplateBuilderComponent
     */
    sidebarWidthChange(width: string): void {
        this.store.updateSidebarWidth(width);
    }

    /**
     * @description This method calls the store to add a container to a box
     *
     * @param {DotGridStackWidget} box
     * @param {numberOrString} rowId
     * @param {DotContainer} container
     */
    addContainer(box: DotGridStackWidget, rowId: numberOrString, container: DotContainer) {
        this.store.addContainer({
            affectedColumn: { ...box, parentId: rowId as string },
            container
        });
    }

    /**
     * @description This method opens the dialog to edit the row styleclasses
     *
     * @param {numberOrString} rowID
     * @memberof TemplateBuilderComponent
     */
    editRowStyleClasses(rowID: numberOrString, styleClasses: string[]): void {
        this.openDynamicDialog(styleClasses).subscribe((styleClasses: string[]) => {
            this.store.updateRow({ id: rowID as string, styleClass: styleClasses });
        });
    }

    /**
     * @description This method opens the dialog to edit the box styleclasses
     *
     * @param {numberOrString} rowID
     * @memberof TemplateBuilderComponent
     */
    editBoxStyleClasses(rowID: numberOrString, box: DotGridStackNode): void {
        this.openDynamicDialog(box.styleClass).subscribe((styleClasses: string[]) => {
            this.store.updateColumnStyleClasses({
                ...box,
                styleClass: styleClasses,
                parentId: rowID as string
            });
        });
    }
    /**
     * @description This method adds a container to the sidebar
     *
     * @param {DotContainer} container
     * @memberof TemplateBuilderComponent
     */
    addSidebarContainer(container: DotContainer): void {
        this.store.addSidebarContainer(container);
    }

    /**
     * @description This method deletes a container from the sidebar
     *
     * @param {number} index
     * @memberof TemplateBuilderComponent
     */
    deleteSidebarContainer(index: number): void {
        this.store.deleteSidebarContainer(index);
    }

    private openDynamicDialog(selectedClasses = []): Observable<string[]> {
        this.ref = this.dialogService.open(AddStyleClassesDialogComponent, {
            header: this.dotMessage.get('dot.template.builder.classes.dialog.header.label'),
            data: {
                selectedClasses
            },
            resizable: false
        });

        return this.ref.onClose.pipe(
            take(1),
            filter((styleClasses) => styleClasses)
        );
    }
}
