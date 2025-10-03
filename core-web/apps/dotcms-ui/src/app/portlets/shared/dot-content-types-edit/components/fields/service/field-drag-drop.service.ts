import { DragulaService } from 'ng2-dragula';
import { merge, Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, map, tap } from 'rxjs/operators';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';
import { FieldUtil } from '@dotcms/utils';

const MAX_COLS_PER_ROW = 4;

/**
 * Provide method to handle with the Field Types
 */

interface DragulaCustomEvent {
    container?: Element;
    el: Element;
    name: string;
    sibling?: Element;
    source: Element;
    target?: Element;
}

interface DragulaDropModel {
    name: string;
    el: Element;
    target: Element;
    source: Element;
    sibling: Element;
    item: DotCMSContentTypeField;
    sourceModel: DotCMSContentTypeField[];
    targetModel: DotCMSContentTypeLayoutRow[] | DotCMSContentTypeField[];
    sourceIndex: number;
    targetIndex: number;
}

export interface DropFieldData {
    item: DotCMSContentTypeField;
    source?: {
        columnId: string;
        model: DotCMSContentTypeField[];
    };
    target: {
        columnId: string;
        model: DotCMSContentTypeField[];
    };
}

@Injectable()
export class FieldDragDropService {
    private dragulaService = inject(DragulaService);
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotMessageService = inject(DotMessageService);

    private static readonly FIELD_BAG_NAME = 'fields-bag';
    private static readonly FIELD_ROW_BAG_NAME = 'fields-row-bag';
    private static readonly FIELD_ROW_BAG_CLASS_OVER = 'row-columns__item--over';

    private _fieldDropFromSource: Observable<DropFieldData>;
    private _fieldDropFromTarget: Observable<DropFieldData>;
    private _fieldRowDropFromTarget: Observable<DotCMSContentTypeLayoutRow[]>;
    private draggedEvent = false;
    private currentFullRowEl: HTMLElement = null;
    private currentColumnOvered: Element;

    constructor() {
        const dragulaOver$ = this.dragulaService.over();
        const dragulaDropModel$ = this.dragulaService.dropModel();

        const isRowFull = () => !!this.currentFullRowEl;
        const wasDrop = (target) => target === null;

        merge(this.dragulaService.drop(), dragulaOver$)
            .pipe(filter(isRowFull))
            .subscribe(({ target }: DragulaCustomEvent) => {
                this.clearCurrentFullRowEl();

                if (wasDrop(target)) {
                    this.dotAlertConfirmService.alert({
                        header: this.dotMessageService.get('contenttypes.fullrow.dialog.header'),
                        message: this.dotMessageService.get('contenttypes.fullrow.dialog.message'),
                        footerLabel: {
                            accept: this.dotMessageService.get('contenttypes.fullrow.dialog.accept')
                        }
                    });
                }
            });

        dragulaOver$
            .pipe(
                filter(
                    (group: { name: string; el: Element; container: Element; source: Element }) =>
                        this.isAColumnContainer(group.container)
                )
            )
            .subscribe(
                (group: { name: string; el: Element; container: Element; source: Element }) => {
                    if (!this.currentColumnOvered) {
                        this.currentColumnOvered = group.container;
                    }

                    if (
                        this.itShouldSetCurrentOveredContainer(
                            <HTMLElement>group.container,
                            <HTMLElement>group.source
                        )
                    ) {
                        this.currentColumnOvered.classList.remove(
                            FieldDragDropService.FIELD_ROW_BAG_CLASS_OVER
                        );

                        this.currentColumnOvered = group.container;

                        this.currentColumnOvered.classList.add(
                            FieldDragDropService.FIELD_ROW_BAG_CLASS_OVER
                        );
                    }
                }
            );

        this.dragulaService
            .dragend()
            .pipe(filter(() => !!this.currentColumnOvered))
            .subscribe(() => {
                this.currentColumnOvered.classList.remove(
                    FieldDragDropService.FIELD_ROW_BAG_CLASS_OVER
                );
            });

        this._fieldRowDropFromTarget = dragulaDropModel$.pipe(
            filter((data: DragulaDropModel) => this.isFieldBeingDragFromColumns(data)),
            map((data: DragulaDropModel) => data.targetModel as DotCMSContentTypeLayoutRow[])
        );

        this._fieldDropFromTarget = dragulaDropModel$.pipe(
            tap(() => {
                this.draggedEvent = true;
                setTimeout(() => {
                    this.draggedEvent = false;
                }, 100);
            }),
            filter((data: DragulaDropModel) => this.isDraggingExistingField(data)),
            map((data: DragulaDropModel) => this.getDroppedFieldData(data))
        );

        this._fieldDropFromSource = dragulaDropModel$.pipe(
            filter((data: DragulaDropModel) => this.isDraggingNewField(data)),
            map((data: DragulaDropModel) => this.getDroppedFieldData(data))
        );
    }

    /**
     * Returns status if a field is being dragged
     * @returns boolean
     *
     * @memberof FieldDragDropService
     */
    isDraggedEventStarted(): boolean {
        return this.draggedEvent;
    }

    /**
     * Set options for fields bag and rows bag
     *
     * @memberof FieldDragDropService
     */
    setBagOptions(): void {
        this.setFieldBagOptions();
        this.setFieldRowBagOptions();
    }

    /**
     * Set the options for the 'fields-bag' dragula group
     * @memberof FieldDragDropService
     */
    setFieldBagOptions(): void {
        const fieldBagOpts = this.dragulaService.find(FieldDragDropService.FIELD_BAG_NAME);
        if (!fieldBagOpts) {
            this.dragulaService.createGroup(FieldDragDropService.FIELD_BAG_NAME, {
                copy: this.shouldCopy.bind(this),
                accepts: this.shouldAccepts.bind(this),
                moves: this.shouldMovesField,
                copyItem: (item) => structuredClone(item)
            });
        }
    }

    /**
     * Set the options for the 'fields-row-bag' dragula group
     * @memberof FieldDragDropService
     */
    setFieldRowBagOptions(): void {
        const fieldRowBagOpts = this.dragulaService.find(FieldDragDropService.FIELD_ROW_BAG_NAME);
        if (!fieldRowBagOpts) {
            this.dragulaService.createGroup(FieldDragDropService.FIELD_ROW_BAG_NAME, {
                copy: this.shouldCopy.bind(this),
                accepts: this.shouldAccepts.bind(this),
                moves: this.shouldMoveRow.bind(this),
                copyItem: (item) => structuredClone(item)
            });
        }
    }

    get fieldDropFromSource$(): Observable<DropFieldData> {
        return this._fieldDropFromSource;
    }

    get fieldDropFromTarget$(): Observable<DropFieldData> {
        return this._fieldDropFromTarget;
    }

    get fieldRowDropFromTarget$(): Observable<DotCMSContentTypeLayoutRow[]> {
        return this._fieldRowDropFromTarget;
    }

    private getDroppedFieldData(data: DragulaDropModel): DropFieldData {
        return {
            item: data.item,
            source: {
                columnId: (<HTMLElement>data.source).dataset.columnid,
                model: data.sourceModel
            },
            target: {
                columnId: (<HTMLElement>data.target).dataset.columnid,
                model: data.targetModel as DotCMSContentTypeField[]
            }
        };
    }

    private isDraggingNewField(data: DragulaDropModel): boolean {
        return (
            data.name === FieldDragDropService.FIELD_BAG_NAME &&
            this.isDraggingFromSource(<HTMLElement>data.source)
        );
    }

    private isDraggingExistingField(data: DragulaDropModel): boolean {
        return (
            data.name === FieldDragDropService.FIELD_BAG_NAME &&
            (<HTMLElement>data.source).dataset.dragType === 'target'
        );
    }

    private isDraggingFromSource(source: HTMLElement): boolean {
        return source.dataset.dragType === 'source';
    }

    private isFieldBeingDragFromColumns(data: DragulaDropModel): boolean {
        return data.name === FieldDragDropService.FIELD_ROW_BAG_NAME;
    }

    private isAColumnContainer(container: Element): boolean {
        return container.classList.contains('row-columns__item');
    }

    private isANewColumnContainer(container: Element): boolean {
        return this.currentColumnOvered && this.currentColumnOvered !== container;
    }

    private shouldCopy(_el: HTMLElement, source: HTMLElement): boolean {
        return this.isDraggingFromSource(source);
    }

    private shouldMoveRow(
        _el: HTMLElement,
        source: HTMLElement,
        handle: HTMLElement,
        _sibling: HTMLElement
    ): boolean {
        const noDrag = !handle.classList.contains('no-drag');
        const isDragButton =
            handle.parentElement.classList.contains('row-header__drag') ||
            handle.classList.contains('row-header__drag');

        return noDrag && this.shouldDrag(source, isDragButton);
    }

    private shouldDrag(source: HTMLElement, isDragButton: boolean): boolean {
        return this.isDraggingFromSource(source) || isDragButton;
    }

    private shouldAccepts(
        el: HTMLElement,
        target: HTMLElement,
        _source: HTMLElement,
        _sibling: HTMLElement
    ): boolean {
        const columnsCount = target.parentElement.querySelectorAll('.row-columns__item').length;
        const isColumnField = FieldUtil.isColumnBreak(el.dataset.clazz);
        const cantAddColumn = isColumnField && columnsCount >= MAX_COLS_PER_ROW;

        if (cantAddColumn) {
            this.clearCurrentFullRowEl();
            this.disableRowElement(target.parentElement.parentElement);

            return false;
        }

        return true;
    }

    private shouldMovesField(
        el: HTMLElement,
        _container: Element,
        _handle: Element,
        _sibling: Element
    ): boolean {
        return el.dataset.dragType !== 'not_field';
    }

    private itShouldSetCurrentOveredContainer(
        container: HTMLElement,
        source: HTMLElement
    ): boolean {
        return this.isANewColumnContainer(container) || this.isDraggingFromSource(source);
    }

    private disableRowElement(el: HTMLElement): void {
        this.currentFullRowEl = el;
        this.currentFullRowEl.style.opacity = '0.4';
        this.currentFullRowEl.style.cursor = 'not-allowed';
    }

    private clearCurrentFullRowEl(): void {
        if (this.currentFullRowEl && this.currentFullRowEl.style.opacity) {
            this.currentFullRowEl.style.opacity = null;
            this.currentFullRowEl.style.cursor = null;
            this.currentFullRowEl = null;
        }
    }
}
