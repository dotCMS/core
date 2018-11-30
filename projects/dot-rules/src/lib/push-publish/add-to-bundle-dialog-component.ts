import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter, OnChanges} from '@angular/core';
import {IBundle} from '../services/bundle-service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-add-to-bundle-dialog-component',
  template: `
    <p-dialog width="700" header="Add to Bundle" [visible]="!hidden" [modal]="true" [dismissableMask]="true" [closable]="false" appendTo="body" [draggable]="false" >
      <p-message  *ngIf="errorMessage" style="margin-bottom: 16px; display: block;" severity="error" [text]="errorMessage"></p-message>
      <cw-input-dropdown
        flex
        [value]="bundleStores ? bundleStores[0]?.id : null"
        allowAdditions="true"
        (onDropDownChange)="setSelectedBundle($event)"
        (enter)="addToBundle.emit(selectedBundle)">
        <cw-input-option
          *ngFor="let opt of bundleStores"
          [value]="opt.id"
          [label]="opt.name"
        ></cw-input-option>
      </cw-input-dropdown>
      <p-footer>
        <button type="button" pButton (click)="addToBundle.emit(selectedBundle)" label="Add" [disabled]="!selectedBundle"></button>
        <button type="button" pButton (click)="cancel.emit()" label="Cancel" class="ui-button-secondary"></button>
      </p-footer>
    </p-dialog>`
})
export class AddToBundleDialogComponent implements OnChanges {
  @Input() hidden = false;
  @Input() bundleStores: IBundle[];
  @Input() errorMessage: string = null;

  @Output() close: EventEmitter<{isCanceled: boolean}> = new EventEmitter(false);
  @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
  @Output() addToBundle: EventEmitter<IBundle> = new EventEmitter(false);

  public selectedBundle: IBundle = null;

  ngOnChanges(change): void {
    if (change.bundleStores && change.bundleStores.currentValue) {
        this.selectedBundle = change.bundleStores.currentValue[0];
    }
  }

  setSelectedBundle(bundleId: string): void {
    this.selectedBundle = {
      id: bundleId,
      name: bundleId
    };
    this.bundleStores.forEach((bundle) => {
      if (bundle.id === bundleId) {
        this.selectedBundle = bundle;
      }
    });
  }
}
