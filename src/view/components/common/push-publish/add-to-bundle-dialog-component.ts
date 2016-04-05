import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../modal-dialog/dialog-component";
import {Dropdown, InputOption} from "../../semantic/modules/dropdown/dropdown";
import {IBundle} from "../../../../api/services/bundle-service";

@Component({
  selector: 'cw-add-to-bundle-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent, Dropdown, InputOption],
  template: `<cw-modal-dialog
    [headerText]="'Add to Bundle'"
    [okText]="'Add'"
    [hidden]="hidden"
    [okEnabled]="selectedBundle != null"
    [errorMessage]="errorMessage"
    width="25em"
    height="20em"
    (ok)="addToBundle.emit(selectedBundle)"
    (cancel)="cancel.emit()">
  <cw-input-dropdown
      flex
      [value]="bundleStores[0]?.id"
      allowAdditions="true"
      (click)="$event.stopPropagation()"
      (change)="setSelectedBundle($event)">
    <cw-input-option
        *ngFor="#opt of bundleStores"
        [value]="opt.id"
        [label]="opt.name"
    ></cw-input-option>
  </cw-input-dropdown>
</cw-modal-dialog>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddToBundleDialogComponent {
  @Input() hidden:boolean = false
  @Input() bundleStores:IBundle[];
  @Input() errorMessage:string = null

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() addToBundle:EventEmitter<IBundle> = new EventEmitter(false)

  public selectedBundle:IBundle;

  constructor() { }

  ngOnChanges(change){
    if (change.bundleStores) {
      this.selectedBundle = change.bundleStores.currentValue[0];
    }
  }

  setSelectedBundle(bundleId:string) {
    this.selectedBundle = {
      id: bundleId,
      name: bundleId
    }
    this.bundleStores.forEach((bundle) => {
      if (bundle.id === bundleId) {
        this.selectedBundle = bundle
      }
    })
  }
}

