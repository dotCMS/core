import {
    Component,
    ChangeDetectionStrategy,
    Input,
    Output,
    EventEmitter,
    OnChanges
} from '@angular/core';

import { MenuItem } from 'primeng/api';

import { IBundle } from '../services/bundle-service';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-add-to-bundle-dialog-component',
    template: `
        <p-dialog
            [visible]="!hidden"
            [modal]="true"
            [dismissableMask]="true"
            [closable]="false"
            [focusOnShow]="false"
            [draggable]="false"
            width="700"
            header="Add to Bundle"
            appendTo="body">
            <p-message
                *ngIf="errorMessage"
                [text]="errorMessage"
                style="margin-bottom: 16px; display: block;"
                severity="error"></p-message>
            <cw-input-dropdown
                (onDropDownChange)="setSelectedBundle($event)"
                (keyup.enter)="addToBundle.emit(selectedBundle)"
                [focus]="!hidden"
                [options]="options"
                [value]="bundleStores ? bundleStores[0]?.id : null"
                flex
                allowAdditions="true"></cw-input-dropdown>
            <p-footer>
                <button
                    (click)="cancel.emit()"
                    type="button"
                    pButton
                    secondary
                    label="Cancel"
                    class="ui-button-secondary"></button>
                <button
                    (click)="addToBundle.emit(selectedBundle)"
                    [disabled]="!selectedBundle"
                    type="button"
                    pButton
                    label="Add"></button>
            </p-footer>
        </p-dialog>
    `
})
export class AddToBundleDialogComponent implements OnChanges {
    @Input() hidden = false;
    @Input() bundleStores: IBundle[];
    @Input() errorMessage: string = null;

    @Output() close: EventEmitter<{ isCanceled: boolean }> = new EventEmitter(false);
    @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
    @Output() addToBundle: EventEmitter<IBundle> = new EventEmitter(false);

    options: MenuItem[];

    public selectedBundle: IBundle = null;

    ngOnChanges(change): void {
        if (change.bundleStores && change.bundleStores.currentValue) {
            this.selectedBundle = change.bundleStores.currentValue[0];
            this.options = this.bundleStores.map((item: IBundle) => {
                return {
                    label: item.name,
                    value: item.id
                };
            });
        }
    }

    setSelectedBundle(bundleId: string): void {
        this.selectedBundle = bundleId
            ? {
                  id: bundleId,
                  name: bundleId
              }
            : null;
        this.bundleStores.forEach((bundle) => {
            if (bundle.id === bundleId) {
                this.selectedBundle = bundle;
            }
        });
    }
}
