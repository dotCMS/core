import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackNode,
    GridStackWidget,
    numberOrString
} from 'gridstack';
import { Observable, Subject, combineLatest } from 'rxjs';

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

import { filter, startWith, take, tap, map, skip, takeUntil } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotContainer,
    DotLayout,
    DotLayoutBody,
    DotTemplateDesigner,
    DotTheme,
    DotContainerMap
} from '@dotcms/dotcms-models';

import { colIcon, rowIcon } from './assets/icons';
import { AddStyleClassesDialogComponent } from './components/add-style-classes-dialog/add-style-classes-dialog.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { TemplateBuilderThemeSelectorComponent } from './components/template-builder-theme-selector/template-builder-theme-selector.component';
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
    layout!: DotLayout;

    @Input()
    themeId!: string;

    @Input()
    containerMap!: DotContainerMap;

    @Output()
    templateChange: EventEmitter<DotTemplateDesigner> = new EventEmitter<DotTemplateDesigner>();

    @ViewChildren('rowElement', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<TemplateBuilderRowComponent>;

    @ViewChildren('boxElement', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<GridItemHTMLElement>>;

    get layoutProperties(): DotTemplateLayoutProperties {
        return {
            header: this.layout.header,
            footer: this.layout.footer,
            sidebar: this.layout.sidebar ?? {
                location: ''
            }
        };
    }

    public items$: Observable<DotLayoutBody>;
    private destroy$: Subject<boolean>;
    public vm$: Observable<DotTemplateBuilderState> = this.store.vm$;

    public readonly rowIcon = rowIcon;
    public readonly colIcon = colIcon;
    public readonly rowDisplayHeight = `${GRID_STACK_ROW_HEIGHT - 1}${GRID_STACK_UNIT}`; // setting a lower height to have space between rows
    private dotLayout: DotLayout;

    grid!: GridStack;

    constructor(
        private store: DotTemplateBuilderStore,
        private dialogService: DialogService,
        private dotMessage: DotMessageService
    ) {
        this.items$ = this.store.items$.pipe(map((items) => parseFromGridStackToDotObject(items)));

        combineLatest([this.items$, this.store.layoutProperties$])
            .pipe(
                startWith([]),
                filter(([items, layoutProperties]) => !!items && !!layoutProperties),
                skip(1), // Skip the init of the store
                takeUntil(this.destroy$),
                tap(([items, layoutProperties]) => {
                    this.dotLayout = {
                        ...this.layout,
                        sidebar: layoutProperties?.sidebar?.location?.length // Make it null if it's empty so it doesn't get saved
                            ? layoutProperties.sidebar
                            : null,
                        body: items,
                        title: this.layout?.title ?? '',
                        width: this.layout?.width ?? ''
                    };

                    this.templateChange.emit({
                        themeId: this.themeId,
                        layout: { ...this.dotLayout }
                    });
                })
            )
            .subscribe();
    }

    ngOnInit(): void {
        this.store.init({
            items: parseFromDotObjectToGridStack(this.layout.body),
            layoutProperties: this.layoutProperties,
            resizingRowID: '',
            containerMap: this.containerMap
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

            subgrid.on('resizestart', (_: Event, el: GridItemHTMLElement) => {
                this.store.setResizingRowID(el.gridstackNode.grid.parentGridItem.id);
            });
            subgrid.on('resizestop', () => {
                this.store.setResizingRowID(null);
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
                            })
                            .on('resizestart', (_: Event, el: GridItemHTMLElement) => {
                                this.store.setResizingRowID(
                                    el.gridstackNode.grid.parentGridItem.id
                                );
                            })
                            .on('resizestop', () => {
                                this.store.setResizingRowID(null);
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
        this.destroy$.next(true);
        this.destroy$.complete();
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
     * @description This method calls the store to remove a container from a box
     *
     * @param {DotGridStackWidget} box
     * @param {numberOrString} rowId
     * @param {number} containerIndex
     */
    deleteContainer(box: DotGridStackWidget, rowId: numberOrString, containerIndex: number) {
        this.store.deleteContainer({
            affectedColumn: { ...box, parentId: rowId as string },
            containerIndex
        });
    }

    /**
     * @description This method opens the dialog to edit the box styleclasses
     *
     * @param {numberOrString} rowID
     * @memberof TemplateBuilderComponent
     */
    editBoxStyleClasses(rowID: numberOrString, box: DotGridStackNode): void {
        const ref = this.dialogService.open(AddStyleClassesDialogComponent, {
            header: this.dotMessage.get('dot.template.builder.classes.dialog.header.label'),
            data: {
                selectedClasses: box.styleClass || []
            },
            resizable: false
        });

        ref.onClose
            .pipe(
                take(1),
                filter((styleClasses) => styleClasses)
            )
            .subscribe((styleClasses: string[]) => {
                this.store.updateColumnStyleClasses({
                    ...box,
                    styleClass: styleClasses,
                    parentId: rowID as string
                });
            });
    }

    /**
     * @description This method opens the dialog to edit the row styleclasses
     *
     * @memberof TemplateBuilderComponent
     */
    openThemeSelectorDynamicDialog(): void {
        const ref: DynamicDialogRef = this.dialogService.open(
            TemplateBuilderThemeSelectorComponent,
            {
                header: this.dotMessage.get('dot.template.builder.theme.dialog.header.label'),
                resizable: false,
                width: '80%',
                closeOnEscape: true,
                data: {
                    themeId: this.themeId
                }
            }
        );

        ref.onClose
            .pipe(
                take(1),
                filter((theme: DotTheme) => !!theme)
            )
            .subscribe(
                (theme: DotTheme) => {
                    this.themeId = theme.identifier;
                    this.templateChange.emit({
                        themeId: this.themeId,
                        layout: { ...this.dotLayout }
                    });
                },
                () => {
                    /* */
                },
                () => ref.destroy() // Destroy the dialog when it's closed
            );
    }
}
