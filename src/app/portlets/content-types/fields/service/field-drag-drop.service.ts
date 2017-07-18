
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DragulaService } from 'ng2-dragula';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldDragDropService {
    private static readonly FIELD_BAG_NAME = 'fields-bag';
    private static readonly FIELD_ROW_BAG_NAME = 'fields-row-bag';

    constructor(private dragulaService: DragulaService) {

    }

    /**
     * Set the options for the 'fields-bag' dragula group
     * @memberof FieldDragDropService
     */
    setFieldBagOptions(): void {
        let fieldBagOpts = this.dragulaService.find(FieldDragDropService.FIELD_BAG_NAME);

        if (!fieldBagOpts) {
            this.dragulaService.setOptions(FieldDragDropService.FIELD_BAG_NAME, {
                copy: true,
                moves: this.shouldMove
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
                copy: true,
                moves: this.shouldMove
            });
        }
    }

    private shouldMove(el, target): boolean {
        return target.dataset.dragType === 'source';
    }
}