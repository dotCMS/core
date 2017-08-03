import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DragulaService } from 'ng2-dragula';
import { Subject } from 'rxjs/Rx';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldDragDropService {
    private static readonly FIELD_BAG_NAME = 'fields-bag';
    private static readonly FIELD_ROW_BAG_NAME = 'fields-row-bag';

    private _fieldDrop: Subject<any> = new Subject();

    constructor(private dragulaService: DragulaService) {
        dragulaService.dropModel.subscribe(value => {
            this._fieldDrop.next(value);
        });

        dragulaService.removeModel.subscribe(value => {
            this._fieldDrop.next();
        });
    }

    /**
     * Set the options for the 'fields-bag' dragula group
     * @memberof FieldDragDropService
     */
    setFieldBagOptions(): void {
        let fieldBagOpts = this.dragulaService.find(FieldDragDropService.FIELD_BAG_NAME);

        if (!fieldBagOpts) {
            this.dragulaService.setOptions(FieldDragDropService.FIELD_BAG_NAME, {
                copy: this.shouldCopy
            });
        }
    }

    /**
     * Set the options for the 'fields-row-bag' dragula group
     * @memberof FieldDragDropService
     */
    setFieldRowBagOptions(): void {
        let fieldRowBagOpts = this.dragulaService.find(FieldDragDropService.FIELD_ROW_BAG_NAME);

        if (!fieldRowBagOpts) {
            this.dragulaService.setOptions(FieldDragDropService.FIELD_ROW_BAG_NAME, {
                copy: this.shouldCopy,
                moves: this.shouldMoveRow
            });
        }
    }

    get fieldDrop$(): Observable<any> {
        return this._fieldDrop.asObservable();
    }

    private shouldCopy(
        el: HTMLElement,
        source: HTMLElement,
        handle: HTMLElement,
        sibling: HTMLElement
    ): boolean {
        return source.dataset.dragType === 'source';
    }

    private shouldMoveRow(
        el: HTMLElement,
        source: HTMLElement,
        handle: HTMLElement,
        sibling: HTMLElement
    ): boolean {
        return source.dataset.dragType === 'source' || handle.classList.contains('row-drag-button');
    }
}
