import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from '@angular/core';
import {IBundle} from '../../../api/services/bundle-service';
import {ViewChild} from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-add-to-bundle-dialog-component',
  template: `<cw-modal-dialog
    [headerText]="'Add to Bundle'"
    [okButtonText]="'Add'"
    [hidden]="hidden"
    [okEnabled]="selectedBundle != null"
    [errorMessage]="errorMessage"
    width="25em"
    height="auto"
    (ok)="addToBundle.emit(selectedBundle)"
    (cancel)="cancel.emit()"
    (open)="focusDropDown()">
  <cw-input-dropdown
      flex
      [value]="bundleStores[0]?.id"
      allowAdditions="true"
      (click)="$event.stopPropagation()"
      (onDropDownChange)="setSelectedBundle($event)"
      (enter)="addToBundle.emit(selectedBundle)">
    <cw-input-option
        *ngFor="let opt of bundleStores"
        [value]="opt.id"
        [label]="opt.name"
    ></cw-input-option>
  </cw-input-dropdown>
</cw-modal-dialog>`
})
export class AddToBundleDialogComponent {
  @Input() hidden = false;
  @Input() bundleStores: IBundle[];
  @Input() errorMessage: string = null;

  @Output() close: EventEmitter<{isCanceled: boolean}> = new EventEmitter(false);
  @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
  @Output() addToBundle: EventEmitter<IBundle> = new EventEmitter(false);

//   @ViewChild(Dropdown)
//   dropdown: Dropdown;

  public selectedBundle: IBundle = null;

  ngOnChanges(change): void {
    if (change.bundleStores) {
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

  focusDropDown(): void {
      // TODO: NG bring back this when rules gets in.
    // this.dropdown.focus();
  }
}
