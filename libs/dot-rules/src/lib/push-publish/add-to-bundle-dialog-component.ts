import {
    Component,
    ChangeDetectionStrategy,
    Input,
    Output,
    EventEmitter,
    OnChanges
} from '@angular/core';
import { IBundle } from '../services/bundle-service';
import { MenuItem } from 'primeng/api';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-add-to-bundle-dialog-component',
    template: ` <p-dialog
        width="700"
        header="Add to Bundle"
        [visible]="!hidden"
        [modal]="true"
        [dismissableMask]="true"
        [closable]="false"
        appendTo="body"
        [focusOnShow]="false"
        [draggable]="false"
    >
        <p-message
            *ngIf="errorMessage"
            style="margin-bottom: 16px; display: block;"
            severity="error"
            [text]="errorMessage"
        ></p-message>
        <cw-input-dropdown
            flex
            [focus]="!hidden"
            [options]="options"
            [value]="bundleStores ? bundleStores[0]?.id : null"
            allowAdditions="true"
            (onDropDownChange)="setSelectedBundle($event)"
            (keyup.enter)="addToBundle.emit(selectedBundle)"
        >
        </cw-input-dropdown>
        <p-footer>
            <button
                type="button"
                pButton
                secondary
                (click)="cancel.emit()"
                label="Cancel"
                class="ui-button-secondary"
            ></button>
            <button
                type="button"
                pButton
                (click)="addToBundle.emit(selectedBundle)"
                label="Add"
                [disabled]="!selectedBundle"
            ></button>
        </p-footer>
    </p-dialog>`
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
