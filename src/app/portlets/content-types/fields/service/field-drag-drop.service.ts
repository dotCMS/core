import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DragulaService } from 'ng2-dragula';
import { filter, map } from 'rxjs/operators';
import { FieldDivider, ContentTypeField } from '@portlets/content-types/fields/shared';
import * as _ from 'lodash';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldDragDropService {
    private static readonly FIELD_BAG_NAME = 'fields-bag';
    private static readonly FIELD_ROW_BAG_NAME = 'fields-row-bag';

    private _fieldDropFromSource: Observable<DropFieldData>;
    private _fieldDropFromTarget: Observable<DropFieldData>;
    private _fieldRowDropFromTarget: Observable<FieldDivider[]>;

    constructor(private dragulaService: DragulaService) {
        dragulaService.over().subscribe(this.toggleOverClass);
        dragulaService.out().subscribe(this.toggleOverClass);

        this._fieldRowDropFromTarget = dragulaService
            .dropModel()
            .pipe(
                filter((data: DragulaDropModel) => data.name === FieldDragDropService.FIELD_ROW_BAG_NAME),
                map((data: DragulaDropModel) => data.targetModel)
            );

        this._fieldDropFromTarget = dragulaService
            .dropModel()
            .pipe(
                filter((data: DragulaDropModel) => data.name === FieldDragDropService.FIELD_BAG_NAME &&
                    (<HTMLElement> data.source).dataset.dragType === 'target'),
                map((data: DragulaDropModel) => {
                    return {
                        item: data.item,
                        source: {
                            columnId: (<HTMLElement> data.source).dataset.columnid,
                            model: data.sourceModel
                        },
                        target: {
                            columnId: (<HTMLElement> data.target).dataset.columnid,
                            model: data.targetModel
                        }
                    };
                })
            );


        this._fieldDropFromSource = dragulaService
            .dropModel()
            .pipe(
                filter((data: DragulaDropModel) => data.name === FieldDragDropService.FIELD_BAG_NAME &&
                    (<HTMLElement> data.source).dataset.dragType === 'source'),
                map((data: DragulaDropModel) => {
                    return {
                        item: data.item,
                        target: {
                            columnId: (<HTMLElement> data.target).dataset.columnid,
                            model: data.targetModel
                        }
                    };
                })

            );
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
                copy: this.shouldCopy,
                accepts: this.shouldAccepts,
                moves: this.shouldMovesField,
                copyItem: (item: any) => {
                    return _.cloneDeep(item);
                }
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
                copy: this.shouldCopy,
                moves: this.shouldMoveRow
            });
        }
    }

    get fieldDropFromSource$(): Observable<any> {
        return this._fieldDropFromSource;
    }

    get fieldDropFromTarget$(): Observable<any> {
        return this._fieldDropFromTarget;
    }

    get fieldRowDropFromTarget$(): Observable<any> {
        return this._fieldRowDropFromTarget;
    }

    private toggleOverClass(group: {
        name: string;
        el: Element;
        container: Element;
        source: Element;
    }): void {
        if (group.container.classList.contains('row-columns__item')) {
            group.container.classList.toggle('row-columns__item--over');
        }
    }

    private shouldCopy(_el: HTMLElement, source: HTMLElement): boolean {
        return source.dataset.dragType === 'source';
    }

    private shouldMoveRow(
        _el: HTMLElement,
        source: HTMLElement,
        handle: HTMLElement,
        _sibling: HTMLElement
    ): boolean {
        const isDragButton =
            handle.parentElement.classList.contains('row-header__drag') ||
            handle.classList.contains('row-header__drag');
        return source.dataset.dragType === 'source' || isDragButton;
    }

    private shouldAccepts(
        _el: HTMLElement,
        source: HTMLElement,
        _handle: HTMLElement,
        _sibling: HTMLElement
    ): boolean {
        return source.dataset.dragType !== 'source';
    }

    private shouldMovesField(
        el: HTMLElement,
        _source: HTMLElement,
        _handle: HTMLElement,
        _sibling: HTMLElement
    ): boolean {
        return el.dataset.dragType !== 'not_field';
    }
}

interface DragulaDropModel {
    name: string;
    el: Element;
    target: Element;
    source: Element;
    sibling: Element;
    item: any;
    sourceModel: any[];
    targetModel: any[];
    sourceIndex: number;
    targetIndex: number;

}

export interface DropFieldData {
    item: ContentTypeField;
    source?: {
        columnId: string;
        model: ContentTypeField[]
    };
    target: {
        columnId: string;
        model: ContentTypeField[]
    };
}
