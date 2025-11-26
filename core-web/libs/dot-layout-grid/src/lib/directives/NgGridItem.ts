import {
    Directive,
    ElementRef,
    EventEmitter,
    KeyValueDiffer,
    KeyValueDiffers,
    OnInit,
    OnDestroy,
    ViewContainerRef,
    Output,
    DoCheck,
    Renderer2,
    inject
} from '@angular/core';

import { NgGrid } from './NgGrid';

import {
    NgGridItemConfig,
    NgGridItemEvent,
    NgGridItemPosition,
    NgGridItemSize,
    NgGridRawPosition,
    NgGridItemDimensions,
    ResizeHandle
} from '../interfaces/INgGrid';

@Directive({
    inputs: ['config: ngGridItem'],
    selector: '[ngGridItem]'
})
export class NgGridItem implements OnInit, OnDestroy, DoCheck {
    private _differs = inject(KeyValueDiffers);
    private _ngEl = inject(ElementRef);
    private _renderer = inject(Renderer2);
    private _ngGrid = inject(NgGrid);
    containerRef = inject(ViewContainerRef);

    // 	Event Emitters
    @Output()
    public onItemChange: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>(false);
    @Output()
    public onDragStart: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onDrag: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onDragStop: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onDragAny: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onResizeStart: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onResize: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onResizeStop: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onResizeAny: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onChangeStart: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onChange: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onChangeStop: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public onChangeAny: EventEmitter<NgGridItemEvent> = new EventEmitter<NgGridItemEvent>();
    @Output()
    public ngGridItemChange: EventEmitter<NgGridItemConfig> = new EventEmitter<NgGridItemConfig>();

    // 	Default config
    // tslint:disable:object-literal-sort-keys
    // tslint:disable-next-line:member-ordering
    private static CONST_DEFAULT_CONFIG: NgGridItemConfig = {
        uid: null,
        col: 1,
        row: 1,
        sizex: 1,
        sizey: 1,
        dragHandle: null,
        resizeHandle: null,
        fixed: false,
        draggable: true,
        resizable: true,
        borderSize: 25,
        resizeDirections: null
    };
    // tslint:enable:object-literal-sort-keys
    public isFixed = false;
    public isDraggable = true;
    public isResizable = true;
    public minWidth = 0;
    public minHeight = 0;
    public uid: string = null;

    // 	Private variables
    private _payload: any;
    private _currentPosition: NgGridItemPosition = { col: 1, row: 1 };
    private _size: NgGridItemSize = { x: 1, y: 1 };
    private _config = NgGridItem.CONST_DEFAULT_CONFIG;
    private _userConfig: any = null;
    private _dragHandle: string = null;
    private _resizeHandle: ResizeHandle = null;
    private _borderSize = 25;
    private _elemWidth: number;
    private _elemHeight: number;
    private _elemLeft: number;
    private _elemTop: number;
    private _added = false;
    private _differ: KeyValueDiffer<string, any>;
    private _cascadeMode: string;
    private _maxCols = 0;
    private _minCols = 0;
    private _maxRows = 0;
    private _minRows = 0;
    private _resizeDirections: string[] = [];
    private _zIndex = 0;

    set zIndex(zIndex: number) {
        this._renderer.setStyle(this._ngEl.nativeElement, 'z-index', zIndex.toString());
        this._zIndex = zIndex;
    }

    get zIndex(): number {
        return this._zIndex;
    }

    // 	[ng-grid-item] handler
    set config(v: NgGridItemConfig) {
        this._userConfig = v;

        const configObject = Object.assign({}, NgGridItem.CONST_DEFAULT_CONFIG, v);
        for (const x in NgGridItem.CONST_DEFAULT_CONFIG)
            if (configObject[x] == null) configObject[x] = NgGridItem.CONST_DEFAULT_CONFIG[x];

        this.setConfig(configObject);

        if (this._userConfig != null) {
            if (this._differ == null) {
                this._differ = this._differs.find(this._userConfig).create();
            }

            this._differ.diff(this._userConfig);
        }

        if (!this._added) {
            this._added = true;
            this._ngGrid.addItem(this);
        }

        this._recalculateDimensions();
        this._recalculatePosition();
    }

    get sizex(): number {
        return this._size.x;
    }

    get sizey(): number {
        return this._size.y;
    }

    get col(): number {
        return this._currentPosition.col;
    }

    get row(): number {
        return this._currentPosition.row;
    }

    get currentCol(): number {
        return this._currentPosition.col;
    }

    get currentRow(): number {
        return this._currentPosition.row;
    }

    public onResizeStartEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onResizeStart.emit(event);
        this.onResizeAny.emit(event);
        this.onChangeStart.emit(event);
        this.onChangeAny.emit(event);
    }
    public onResizeEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onResize.emit(event);
        this.onResizeAny.emit(event);
        this.onChange.emit(event);
        this.onChangeAny.emit(event);
    }
    public onResizeStopEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onResizeStop.emit(event);
        this.onResizeAny.emit(event);
        this.onChangeStop.emit(event);
        this.onChangeAny.emit(event);

        this.onConfigChangeEvent();
    }
    public onDragStartEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onDragStart.emit(event);
        this.onDragAny.emit(event);
        this.onChangeStart.emit(event);
        this.onChangeAny.emit(event);
    }
    public onDragEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onDrag.emit(event);
        this.onDragAny.emit(event);
        this.onChange.emit(event);
        this.onChangeAny.emit(event);
    }
    public onDragStopEvent(): void {
        const event: NgGridItemEvent = this.getEventOutput();
        this.onDragStop.emit(event);
        this.onDragAny.emit(event);
        this.onChangeStop.emit(event);
        this.onChangeAny.emit(event);

        this.onConfigChangeEvent();
    }
    public onCascadeEvent(): void {
        this.onConfigChangeEvent();
    }

    public ngOnInit(): void {
        this._renderer.addClass(this._ngEl.nativeElement, 'grid-item');
        if (this._ngGrid.autoStyle)
            this._renderer.setStyle(this._ngEl.nativeElement, 'position', 'absolute');
        this._recalculateDimensions();
        this._recalculatePosition();

        if (!this._added) {
            this._added = true;
            this._ngGrid.addItem(this);
        }
    }

    // 	Public methods
    public canDrag(e: any): boolean {
        if (!this.isDraggable) return false;

        if (this._dragHandle) {
            return this.findHandle(this._dragHandle, e.target);
        }

        return true;
    }

    public findHandle(handleSelector: string, startElement: HTMLElement): boolean {
        try {
            let targetElem: any = startElement;

            while (targetElem && targetElem !== this._ngEl.nativeElement) {
                if (this.elementMatches(targetElem, handleSelector)) return true;

                targetElem = targetElem.parentElement;
            }
        } catch (err) {} // tslint:disable-line:no-empty

        return false;
    }

    public canResize(e: any): string {
        if (!this.isResizable) return null;

        if (this._resizeHandle) {
            if (typeof this._resizeHandle === 'string') {
                return this.findHandle(this._resizeHandle, e.target) ? 'bottomright' : null;
            }

            if (typeof this._resizeHandle !== 'object') return null;

            for (const direction of this._resizeDirections) {
                if (direction in this._resizeHandle) {
                    if (this.findHandle(this._resizeHandle[direction], e.target)) {
                        return direction;
                    }
                }
            }

            return null;
        }

        if (this._borderSize <= 0) return null;

        const mousePos: NgGridRawPosition = this._getMousePosition(e);

        for (const direction of this._resizeDirections) {
            if (this.canResizeInDirection(direction, mousePos)) {
                return direction;
            }
        }

        return null;
    }

    public onMouseMove(e: any): void {
        if (this._ngGrid.autoStyle) {
            if (this._ngGrid.resizeEnable) {
                const resizeDirection = this.canResize(e);

                let cursor = 'default';
                switch (resizeDirection) {
                    case 'bottomright':

                    case 'topleft':
                        cursor = 'nwse-resize';
                        break;

                    case 'topright':

                    case 'bottomleft':
                        cursor = 'nesw-resize';
                        break;

                    case 'top':

                    case 'bottom':
                        cursor = 'ns-resize';
                        break;

                    case 'left':

                    case 'right':
                        cursor = 'ew-resize';
                        break;

                    default:
                        if (this._ngGrid.dragEnable && this.canDrag(e)) {
                            cursor = 'move';
                        }

                        break;
                }

                this._renderer.setStyle(this._ngEl.nativeElement, 'cursor', cursor);
            } else if (this._ngGrid.dragEnable && this.canDrag(e)) {
                this._renderer.setStyle(this._ngEl.nativeElement, 'cursor', 'move');
            } else {
                this._renderer.setStyle(this._ngEl.nativeElement, 'cursor', 'default');
            }
        }
    }

    public ngOnDestroy(): void {
        if (this._added) this._ngGrid.removeItem(this);
    }

    // 	Getters
    public getElement(): ElementRef {
        return this._ngEl;
    }

    public getDragHandle(): string {
        return this._dragHandle;
    }

    public getResizeHandle(): ResizeHandle {
        return this._resizeHandle;
    }

    public getDimensions(): NgGridItemDimensions {
        return { width: this._elemWidth, height: this._elemHeight };
    }

    public getSize(): NgGridItemSize {
        return this._size;
    }

    public getPosition(): NgGridRawPosition {
        return { left: this._elemLeft, top: this._elemTop };
    }

    public getGridPosition(): NgGridItemPosition {
        return this._currentPosition;
    }

    // 	Setters
    public setConfig(config: NgGridItemConfig): void {
        this._config = config;

        this._payload = config.payload;
        this._currentPosition.col = config.col ? config.col : NgGridItem.CONST_DEFAULT_CONFIG.col;
        this._currentPosition.row = config.row ? config.row : NgGridItem.CONST_DEFAULT_CONFIG.row;
        this._size.x = config.sizex ? config.sizex : NgGridItem.CONST_DEFAULT_CONFIG.sizex;
        this._size.y = config.sizey ? config.sizey : NgGridItem.CONST_DEFAULT_CONFIG.sizey;
        this._dragHandle = config.dragHandle;
        this._resizeHandle = config.resizeHandle;
        this._borderSize = config.borderSize;
        this.isDraggable = config.draggable ? true : false;
        this.isResizable = config.resizable ? true : false;
        this.isFixed = config.fixed ? true : false;
        this._resizeDirections = config.resizeDirections || this._ngGrid.resizeDirections;

        this._maxCols = !isNaN(config.maxCols) && isFinite(config.maxCols) ? config.maxCols : 0;
        this._minCols = !isNaN(config.minCols) && isFinite(config.minCols) ? config.minCols : 0;
        this._maxRows = !isNaN(config.maxRows) && isFinite(config.maxRows) ? config.maxRows : 0;
        this._minRows = !isNaN(config.minRows) && isFinite(config.minRows) ? config.minRows : 0;

        this.minWidth = !isNaN(config.minWidth) && isFinite(config.minWidth) ? config.minWidth : 0;
        this.minHeight =
            !isNaN(config.minHeight) && isFinite(config.minHeight) ? config.minHeight : 0;

        if (this._minCols > 0 && this._maxCols > 0 && this._minCols > this._maxCols)
            this._minCols = 0;
        if (this._minRows > 0 && this._maxRows > 0 && this._minRows > this._maxRows)
            this._minRows = 0;

        if (this._added) {
            this._ngGrid.updateItem(this);
        }

        this._size = this.fixResize(this._size);

        this._recalculatePosition();
        this._recalculateDimensions();
    }

    public ngDoCheck(): boolean {
        if (this._differ != null) {
            const changes: any = this._differ.diff(this._userConfig);

            if (changes != null) {
                return this._applyChanges(changes);
            }
        }

        return false;
    }

    public setSize(newSize: NgGridItemSize, update = true): void {
        newSize = this.fixResize(newSize);
        this._size = newSize;
        if (update) this._recalculateDimensions();

        this.onItemChange.emit(this.getEventOutput());
    }

    public setGridPosition(gridPosition: NgGridItemPosition, update = true): void {
        this._currentPosition = gridPosition;
        if (update) this._recalculatePosition();

        this.onItemChange.emit(this.getEventOutput());
    }

    public getEventOutput(): NgGridItemEvent {
        return {
            col: this._currentPosition.col,
            height: this._elemHeight,
            left: this._elemLeft,
            payload: this._payload,
            row: this._currentPosition.row,
            sizex: this._size.x,
            sizey: this._size.y,
            top: this._elemTop,
            uid: this.uid,
            width: this._elemWidth
        } as NgGridItemEvent;
    }

    public setPosition(x: number, y: number): void {
        switch (this._cascadeMode) {
            case 'up':

            case 'left':

            default:
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', x + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', y + 'px');
                break;

            case 'right':
                this._renderer.setStyle(this._ngEl.nativeElement, 'right', x + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', y + 'px');
                break;

            case 'down':
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', x + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'bottom', y + 'px');
                break;
        }

        this._elemLeft = x;
        this._elemTop = y;
    }

    public setCascadeMode(cascade: string): void {
        this._cascadeMode = cascade;
        switch (cascade) {
            case 'up':

            case 'left':

            default:
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', this._elemLeft + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', this._elemTop + 'px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'right');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'bottom');
                break;

            case 'right':
                this._renderer.setStyle(this._ngEl.nativeElement, 'right', this._elemLeft + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', this._elemTop + 'px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'left');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'bottom');
                break;

            case 'down':
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', this._elemLeft + 'px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'bottom', this._elemTop + 'px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'right');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'top');
                break;
        }
    }

    public setDimensions(w: number, h: number): void {
        if (w < this.minWidth) w = this.minWidth;
        if (h < this.minHeight) h = this.minHeight;

        this._renderer.setStyle(this._ngEl.nativeElement, 'width', w + 'px');
        this._renderer.setStyle(this._ngEl.nativeElement, 'height', h + 'px');

        this._elemWidth = w;
        this._elemHeight = h;
    }

    public startMoving(): void {
        this._renderer.addClass(this._ngEl.nativeElement, 'moving');
        const style: any = window.getComputedStyle(this._ngEl.nativeElement);
        if (this._ngGrid.autoStyle)
            this._renderer.setStyle(
                this._ngEl.nativeElement,
                'z-index',
                (parseInt(style.getPropertyValue('z-index'), 10) + 1).toString()
            );
    }

    public stopMoving(): void {
        this._renderer.removeClass(this._ngEl.nativeElement, 'moving');
        const style: any = window.getComputedStyle(this._ngEl.nativeElement);
        if (this._ngGrid.autoStyle)
            this._renderer.setStyle(
                this._ngEl.nativeElement,
                'z-index',
                (parseInt(style.getPropertyValue('z-index'), 10) - 1).toString()
            );
    }

    public recalculateSelf(): void {
        this._recalculatePosition();
        this._recalculateDimensions();
    }

    public fixResize(newSize: NgGridItemSize): NgGridItemSize {
        if (this._maxCols > 0 && newSize.x > this._maxCols) newSize.x = this._maxCols;
        if (this._maxRows > 0 && newSize.y > this._maxRows) newSize.y = this._maxRows;

        if (this._minCols > 0 && newSize.x < this._minCols) newSize.x = this._minCols;
        if (this._minRows > 0 && newSize.y < this._minRows) newSize.y = this._minRows;

        const itemWidth =
            newSize.x * this._ngGrid.colWidth +
            (this._ngGrid.marginLeft + this._ngGrid.marginRight) * (newSize.x - 1);
        if (itemWidth < this.minWidth)
            newSize.x = Math.ceil(
                (this.minWidth + this._ngGrid.marginRight + this._ngGrid.marginLeft) /
                    (this._ngGrid.colWidth + this._ngGrid.marginRight + this._ngGrid.marginLeft)
            );

        const itemHeight =
            newSize.y * this._ngGrid.rowHeight +
            (this._ngGrid.marginTop + this._ngGrid.marginBottom) * (newSize.y - 1);
        if (itemHeight < this.minHeight)
            newSize.y = Math.ceil(
                (this.minHeight + this._ngGrid.marginBottom + this._ngGrid.marginTop) /
                    (this._ngGrid.rowHeight + this._ngGrid.marginBottom + this._ngGrid.marginTop)
            );

        return newSize;
    }

    // 	Private methods
    private elementMatches(element: any, selector: string): boolean {
        if (!element) return false;
        if (element.matches) return element.matches(selector);
        if (element.oMatchesSelector) return element.oMatchesSelector(selector);
        if (element.msMatchesSelector) return element.msMatchesSelector(selector);
        if (element.mozMatchesSelector) return element.mozMatchesSelector(selector);
        if (element.webkitMatchesSelector) return element.webkitMatchesSelector(selector);

        if (!element.document || !element.ownerDocument) return false;

        const matches: NodeListOf<any> = (
            element.document || element.ownerDocument
        ).querySelectorAll(selector);
        for (let i = matches.length - 1; i >= 0; i--) {
            if (matches.item(i) === element) {
                return true;
            }
        }

        return false;
    }

    private _recalculatePosition(): void {
        const x: number =
            (this._ngGrid.colWidth + this._ngGrid.marginLeft + this._ngGrid.marginRight) *
                (this._currentPosition.col - 1) +
            this._ngGrid.marginLeft +
            this._ngGrid.screenMargin;
        const y: number =
            (this._ngGrid.rowHeight + this._ngGrid.marginTop + this._ngGrid.marginBottom) *
                (this._currentPosition.row - 1) +
            this._ngGrid.marginTop;

        this.setPosition(x, y);
    }

    private _recalculateDimensions(): void {
        if (this._size.x < this._ngGrid.minCols) this._size.x = this._ngGrid.minCols;
        if (this._size.y < this._ngGrid.minRows) this._size.y = this._ngGrid.minRows;

        const newWidth: number =
            this._ngGrid.colWidth * this._size.x +
            (this._ngGrid.marginLeft + this._ngGrid.marginRight) * (this._size.x - 1);
        const newHeight: number =
            this._ngGrid.rowHeight * this._size.y +
            (this._ngGrid.marginTop + this._ngGrid.marginBottom) * (this._size.y - 1);

        const w: number = Math.max(this.minWidth, this._ngGrid.minWidth, newWidth);
        const h: number = Math.max(this.minHeight, this._ngGrid.minHeight, newHeight);

        this.setDimensions(w, h);
    }

    private _getMousePosition(e: any): NgGridRawPosition {
        if (e.originalEvent && e.originalEvent.touches) {
            const oe: any = e.originalEvent;
            e = oe.touches.length
                ? oe.touches[0]
                : oe.changedTouches.length
                  ? oe.changedTouches[0]
                  : e;
        } else if (e.touches) {
            e = e.touches.length ? e.touches[0] : e.changedTouches.length ? e.changedTouches[0] : e;
        }

        const refPos: NgGridRawPosition = this._ngEl.nativeElement.getBoundingClientRect();

        return {
            left: e.clientX - refPos.left,
            top: e.clientY - refPos.top
        };
    }

    private _applyChanges(changes: any): boolean {
        let changed = false;
        const changeCheck = (record: any) => {
            if (this._config[record.key] !== record.currentValue) {
                this._config[record.key] = record.currentValue;
                changed = true;
            }
        };

        changes.forEachAddedItem(changeCheck);
        changes.forEachChangedItem(changeCheck);
        changes.forEachRemovedItem((record: any) => {
            changed = true;
            delete this._config[record.key];
        });

        if (changed) {
            this.setConfig(this._config);
        }

        return changed;
    }

    private onConfigChangeEvent() {
        if (this._userConfig === null) return;

        this._config.sizex = this._userConfig.sizex = this._size.x;
        this._config.sizey = this._userConfig.sizey = this._size.y;
        this._config.col = this._userConfig.col = this._currentPosition.col;
        this._config.row = this._userConfig.row = this._currentPosition.row;
        this.ngGridItemChange.emit(this._userConfig);
    }

    private canResizeInDirection(direction: string, mousePos: NgGridRawPosition): boolean {
        switch (direction) {
            case 'bottomright':
                return (
                    mousePos.left < this._elemWidth &&
                    mousePos.left > this._elemWidth - this._borderSize &&
                    mousePos.top < this._elemHeight &&
                    mousePos.top > this._elemHeight - this._borderSize
                ); // tslint:disable-line:indent

            case 'bottomleft':
                return (
                    mousePos.left < this._borderSize &&
                    mousePos.top < this._elemHeight &&
                    mousePos.top > this._elemHeight - this._borderSize
                ); // tslint:disable-line:indent

            case 'topright':
                return (
                    mousePos.left < this._elemWidth &&
                    mousePos.left > this._elemWidth - this._borderSize &&
                    mousePos.top < this._borderSize
                ); // tslint:disable-line:indent

            case 'topleft':
                return mousePos.left < this._borderSize && mousePos.top < this._borderSize;

            case 'right':
                return (
                    mousePos.left < this._elemWidth &&
                    mousePos.left > this._elemWidth - this._borderSize
                );

            case 'left':
                return mousePos.left < this._borderSize;

            case 'bottom':
                return (
                    mousePos.top < this._elemHeight &&
                    mousePos.top > this._elemHeight - this._borderSize
                );

            case 'top':
                return mousePos.top < this._borderSize;

            default:
                return false;
        }
    }
}
