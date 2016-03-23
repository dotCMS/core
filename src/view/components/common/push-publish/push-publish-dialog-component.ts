import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../dialog-component";
import {IBundle, RuleService} from "../../../../api/rule-engine/Rule";
import {Dropdown, InputOption} from "../../semantic/modules/dropdown/dropdown";
@Component({
  selector: 'cw-push-publish-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent, Dropdown, InputOption],
  template: `<cw-modal-dialog
    [headerText]="'Add to Bundle'"
    [okText]="'Add to Bundle'"
    [hidden]="hidden"
    [okEnabled]="selectedBundle != null"
    (ok)="addToBundle.emit(selectedBundle)"
    (cancel)="cancel.emit()">
  <cw-input-dropdown
      flex="90"
      placeholder="Pick a Bundle"
      (click)="$event.stopPropagation()"
      (change)="setSelectedBundle($event)">
    <cw-input-option
        value="new"
        label="Add new bundle"
    ></cw-input-option>
    <cw-input-option
        *ngFor="#opt of bundleStores"
        [value]="opt.id"
        [label]="opt.name"
    ></cw-input-option>
  </cw-input-dropdown>
</cw-modal-dialog>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class PushPublishDialogComponent {
  @Input() hidden:boolean = false

  @Input() bundleStores:IBundle[];
  @Input() addNewBundle:boolean


  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() addToBundle:EventEmitter<IBundle> = new EventEmitter(false)


  public selectedBundle:IBundle;



  constructor() { }



  ngOnChanges(change){
    if(change.bundleStores){
      console.log("PushPublishDialogComponent", "ngOnChanges", change.bundleStores.currentValue)
    }
  }



  setSelectedBundle(bundleId:string) {
    if (bundleId === "new") {
      console.log("Add new bundle")
      // TODO: need to be able to add new bundles
    } else {
      this.selectedBundle = null
      this.bundleStores.forEach((bundle) => {
        if (bundle.id === bundleId) {
          this.selectedBundle = bundle
        }
      })
    }
  }
}

