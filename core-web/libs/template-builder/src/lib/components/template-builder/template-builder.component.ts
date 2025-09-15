import {
    GridHTMLElement,
    GridItemHTMLElement,
    GridStack,
    GridStackNode,
    GridStackWidget,
    numberOrString
} from 'gridstack';
import { DDElementHost } from 'gridstack/dist/dd-element';
import { Observable, Subject, combineLatest } from 'rxjs';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    SimpleChanges,
    ViewChild,
    ViewChildren,
    inject
} from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { filter, take, map, takeUntil, skip } from 'rxjs/operators';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import {
    DotContainer,
    DotLayout,
    DotLayoutBody,
    DotTemplateDesigner,
    DotTheme,
    DotContainerMap,
    DotTemplate
} from '@dotcms/dotcms-models';

import { colIcon, rowIcon } from './assets/icons';
import { AddStyleClassesDialogComponent } from './components/add-style-classes-dialog/add-style-classes-dialog.component';
import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { TemplateBuilderThemeSelectorComponent } from './components/template-builder-theme-selector/template-builder-theme-selector.component';
import {
    BOX_WIDTH,
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateBuilderState,
    DotTemplateLayoutProperties,
    SCROLL_DIRECTION
} from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import {
    GRID_STACK_ROW_HEIGHT,
    GRID_STACK_UNIT,
    boxInitialOptions,
    gridOptions,
    rowInitialOptions,
    subGridOptions
} from './utils/gridstack-options';
import {
    parseFromDotObjectToGridStack,
    parseFromGridStackToDotObject
} from './utils/gridstack-utils';

@Component({
    selector: 'dotcms-template-builder-lib',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotTemplateBuilderStore],
    standalone: false
})
export class TemplateBuilderComponent implements OnDestroy, OnChanges, OnInit {
    private store = inject(DotTemplateBuilderStore);
    private dialogService = inject(DialogService);
    private dotMessage = inject(DotMessageService);
    private dotContainersService = inject(DotContainersService);
    private cd = inject(ChangeDetectorRef);

    @Input()
    layout!: DotLayout;

    @Input()
    template!: Partial<DotTemplate>;

    @Input()
    containerMap!: DotContainerMap;

    @Output()
    templateChange: EventEmitter<DotTemplateDesigner> = new EventEmitter<DotTemplateDesigner>();

    @ViewChild('templateContainerRef')
    templateContainerRef!: ElementRef<HTMLDivElement>;

    @ViewChildren('rowElement', {
        emitDistinctChangesOnly: true
    })
    rows!: QueryList<TemplateBuilderRowComponent>;

    @ViewChildren('boxElement', {
        emitDistinctChangesOnly: true
    })
    boxes!: QueryList<ElementRef<GridItemHTMLElement>>;

    @ViewChild('addBox')
    addBox: AddWidgetComponent;

    @ViewChildren(AddWidgetComponent) addWidget: QueryList<AddWidgetComponent>;

    private destroy$: Subject<boolean> = new Subject<boolean>();
    public rows$: Observable<DotLayoutBody>;
    public vm$: Observable<DotTemplateBuilderState> = this.store.vm$;

    private themeId$ = this.store.themeId$;

    private dotLayout: DotLayout;

    public readonly rowIcon = rowIcon;
    public readonly colIcon = colIcon;
    public readonly boxWidth = BOX_WIDTH;
    public readonly rowDisplayHeight = `${GRID_STACK_ROW_HEIGHT - 1}${GRID_STACK_UNIT}`; // setting a lower height to have space between rows
    public readonly rowOptions = rowInitialOptions;
    public readonly boxOptions = boxInitialOptions;
    public customStyles = {
        opacity: '0'
    };

    public draggingElement: HTMLElement | null;
    public scrollDirection: SCROLL_DIRECTION = SCROLL_DIRECTION.NONE;

    grid!: GridStack;

    addBoxIsDragging = false;

    get layoutProperties(): DotTemplateLayoutProperties {
        return {
            header: this.layout.header,
            footer: this.layout.footer,
            sidebar: this.layout.sidebar ?? {
                location: '',
                width: 'medium',
                containers: []
            }
        };
    }

    @HostListener('window:mousemove', ['$event'])
    onMouseMove() {
        if (this.draggingElement) {
            const containerRect = this.templateContaniner.getBoundingClientRect();
            const elementRect = this.draggingElement.getBoundingClientRect();
            const scrollSpeed = 10;
            const scrollThreshold = 100;

            const scrollUp = elementRect.top - containerRect.top - scrollThreshold < 0;
            const scrollDown = elementRect.bottom - containerRect.bottom + scrollThreshold > 0;

            if (scrollUp || scrollDown) {
                const direction = scrollUp ? SCROLL_DIRECTION.UP : SCROLL_DIRECTION.DOWN;

                // Prevents multiple intervals from being created
                if (this.scrollDirection === direction) {
                    return;
                }

                this.scrollDirection = direction;
                const scrollStep = () => {
                    const distance = direction === SCROLL_DIRECTION.UP ? -scrollSpeed : scrollSpeed;
                    this.templateContaniner.scrollBy(0, distance);

                    if (this.scrollDirection === direction) {
                        requestAnimationFrame(scrollStep);
                    }
                };

                requestAnimationFrame(scrollStep);
            } else {
                this.scrollDirection = SCROLL_DIRECTION.NONE;
            }
        }
    }

    get templateContaniner(): HTMLElement {
        return this.templateContainerRef.nativeElement;
    }

    constructor() {
        this.rows$ = this.store.rows$.pipe(
            filter(({ shouldEmit }) => shouldEmit),
            map(({ rows }) => parseFromGridStackToDotObject(rows))
        );

        combineLatest([this.rows$, this.store.layoutProperties$, this.themeId$])
            .pipe(
                filter(([items, layoutProperties]) => !!items && !!layoutProperties),
                skip(1),
                takeUntil(this.destroy$)
            )
            .subscribe(([rows, layoutProperties, themeId]) => {
                this.dotLayout = {
                    ...this.layout,
                    ...layoutProperties,
                    sidebar: layoutProperties?.sidebar?.location?.length // Make it null if it's empty so it doesn't get saved
                        ? layoutProperties.sidebar
                        : null,
                    body: rows,
                    title: this.layout?.title ?? '',
                    width: this.layout?.width ?? ''
                };

                this.templateChange.emit({
                    themeId,
                    layout: { ...this.dotLayout }
                });
            });
    }

    ngOnInit(): void {
        this.dotContainersService.defaultContainer$
            .pipe(takeUntil(this.destroy$))
            .subscribe((defaultContainer) => {
                this.store.setState({
                    rows: parseFromDotObjectToGridStack(this.layout.body, defaultContainer),
                    layoutProperties: this.layoutProperties,
                    resizingRowID: '',
                    containerMap: this.getContainerMap(defaultContainer),
                    themeId: this.template.themeId,
                    templateIdentifier: this.template.identifier,
                    shouldEmit: true,
                    defaultContainer
                });

                console.log('defaultContainer', this.getContainerMap(defaultContainer));
                requestAnimationFrame(() => this.setUpGridStack());
            });
    }

    ngOnChanges(changes: SimpleChanges) {
        if (!changes.layout?.firstChange && changes.layout?.currentValue) {
            const parsedRows = parseFromDotObjectToGridStack(changes.layout.currentValue.body);
            const currentTemplate = changes.template?.currentValue;
            this.store.updateOldRows({
                newRows: parsedRows,
                templateIdentifier: currentTemplate?.identifier || this.template.identifier,
                isAnonymousTemplate: currentTemplate?.anonymous || this.template.anonymous // We createa a custom template for the page
            });
        }
    }

    /**
     * @description This method sets up the gridstack
     *
     * @memberof TemplateBuilderComponent
     */
    setUpGridStack() {
        setTimeout(() => {
            this.customStyles = {
                opacity: '1'
            };
            this.cd.detectChanges();
        }, 350);

        this.grid = GridStack.init(gridOptions).on('change', (_: Event, nodes: GridStackNode[]) => {
            this.store.moveRow(nodes as DotGridStackWidget[]);
        });

        GridStack.setupDragIn('dotcms-add-widget', {
            helper: 'clone'
        });

        // Adding subgrids on load
        Array.from(this.grid.el.querySelectorAll('.grid-stack')).forEach((el) => {
            const subgrid = GridStack.addGrid(el as HTMLElement, subGridOptions);
            this.setSubGridEvent(subgrid);
        });

        this.grid
            .on('dropped', (_: Event, previousNode: GridStackNode, newNode: GridStackNode) => {
                if (!newNode.el || previousNode) return;

                newNode.grid?.removeWidget(newNode.el, true, false);

                this.store.addRow({
                    y: newNode.y
                });
            })
            .on('dragstart', ({ target }) => {
                this.draggingElement = target as HTMLElement;
            })
            .on('dragstop', () => this.onDragStop());

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
                        const subgrid = GridStack.addGrid(newGridElement, subGridOptions);
                        this.setSubGridEvent(subgrid);
                    }

                    layout.push(row);
                }
            });

            this.grid.load(layout); // efficient that does diffs only
        });

        this.addWidget.toArray().forEach(({ nativeElement }) => {
            nativeElement.ddElement
                .on('dragstart', ({ target }) => {
                    const helper = (target as DDElementHost).ddElement.ddDraggable?.helper;
                    this.draggingElement = (helper || target) as HTMLElement;
                    this.setAddBoxIsDragging(true);
                })
                .on('dragstop', () => {
                    this.onDragStop();
                    this.setAddBoxIsDragging(false);
                });
        });
    }

    ngOnDestroy(): void {
        this.grid?.destroy(true);
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
     * @description This method opens the dialog to edit the themeID of the template
     *
     * @memberof TemplateBuilderComponent
     */
    openThemeSelectorDynamicDialog(): void {
        let ref: DynamicDialogRef;

        this.themeId$.pipe(take(1)).subscribe((themeId) => {
            ref = this.dialogService.open(TemplateBuilderThemeSelectorComponent, {
                header: this.dotMessage.get('dot.template.builder.theme.dialog.header.label'),
                resizable: false,
                width: '80%',
                closeOnEscape: true,
                data: {
                    themeId
                }
            });
        });

        ref.onClose
            .pipe(
                take(1),
                filter((theme: DotTheme) => !!theme)
            )
            .subscribe(
                (theme: DotTheme) => {
                    this.store.updateThemeId(theme.identifier);
                },
                () => {
                    /* */
                },
                () => ref.destroy() // Destroy the dialog when it's closed
            );
    }

    /**
     * @description This method sets the box initial values of gridstack when gridstack changes it
     *
     * @private
     * @memberof TemplateBuilderComponent
     */
    fixGridStackNodeOptions() {
        if (this.addBoxIsDragging && this.addBox.nativeElement.gridstackNode?.w !== BOX_WIDTH) {
            this.addBox.nativeElement.gridstackNode = {
                ...this.addBox.nativeElement.gridstackNode,
                ...this.boxOptions
            };
        }
    }
    /**
     * @description Set the current value of dragging add box
     *
     * @param {boolean} isDragging
     * @memberof TemplateBuilderComponent
     */
    setAddBoxIsDragging(isDragging: boolean): void {
        this.addBoxIsDragging = isDragging;
    }

    /**
     * @description This method sets the gridstack options
     *
     * @param {GridStack} subGrid
     * @memberof TemplateBuilderComponent
     */
    setSubGridEvent(subGrid: GridStack): void {
        subGrid
            .on('dropped', (_: Event, oldNode: GridStackNode, newNode: GridStackNode) => {
                this.store.subGridOnDropped(oldNode, newNode);
                this.onDragStop();
            })
            .on('change', (_: Event, nodes: GridStackNode[]) => {
                this.store.updateColumnGridStackData(nodes as DotGridStackWidget[]);
            })
            .on('resizestart', (_: Event, el: GridItemHTMLElement) => {
                this.store.setResizingRowID(el.gridstackNode.grid.parentGridItem.id);
            })
            .on('resizestop', () => {
                this.store.setResizingRowID(null);
            })
            .on('dragstart', ({ target }) => {
                this.draggingElement = target as HTMLElement;
            })
            .on('dragstop', () => this.onDragStop());
    }

    /**
     * @description This method resets the state of the drag on Drag Stop
     *
     * @memberof TemplateBuilderComponent
     */
    onDragStop() {
        this.draggingElement = null;
        this.scrollDirection = SCROLL_DIRECTION.NONE;
    }

    /**
     * @description This method calls the store delete a section from the layout
     *
     * @param {keyof DotTemplateLayoutProperties} section
     * @memberof TemplateBuilderComponent
     */
    deleteSection(section: keyof DotTemplateLayoutProperties) {
        this.store.updateLayoutProperties({
            [section]: false
        } as Partial<DotTemplateLayoutProperties>);
    }

    /**
     * @description This method returns the container map
     *
     * @param {DotContainer | null} defaultContainer
     * @return {*}  {DotContainerMap}
     * @memberof TemplateBuilderComponent
     */
    private getContainerMap(defaultContainer: DotContainer | null): DotContainerMap {
        if (!defaultContainer) {
            return this.containerMap;
        }

        const key = defaultContainer.path ?? defaultContainer.identifier;

        return {
            ...this.containerMap,
            [key]: { ...defaultContainer }
        };
    }
}
