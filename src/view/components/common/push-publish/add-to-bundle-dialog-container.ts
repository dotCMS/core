import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {IBundle, RuleService} from "../../../../api/rule-engine/Rule";
import {PushPublishDialogComponent} from "./add-to-bundle-dialog-component";
@Component({
  selector: 'cw-add-to-bundle-dialog-container',
  directives: [CORE_DIRECTIVES, PushPublishDialogComponent],
  template: `
  <cw-add-to-bundle-dialog-component
  [bundleStores]="ruleService.bundles$ | async"
  [hidden]="hidden"
  [errorMessage]="errorMessage"
  (cancel)="hidden = true; close.emit($event); "
  (addToBundle)="addToBundle($event)"
  ></cw-add-to-bundle-dialog-component>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class PushPublishDialogContainer {
  @Input() assetId:string
  @Input() hidden:boolean = false

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)

  private errorMessage:string

  bundleStores:IBundle[] = []

  constructor(public ruleService:RuleService) {
    this.ruleService.loadBundleStores()
  }

  ngOnChanges(change){
    console.log('ngOnChanges')
    if (change.hidden) {
      console.log("PushPublishDialogContainer", "ngOnChanges", change.hidden.currentValue, change.hidden.previousValue)
    }
  }

  addToBundle(bundle:IBundle) {
    this.ruleService.addRuleToBundle(this.assetId, bundle).subscribe((result:any)=> {
        if (!result.errors) {
          this.close.emit(true)
          this.errorMessage = null
        } else {
          // TODO: error message is not showing the first time
          this.errorMessage = "Sorry there was an error please try again"
        }
    })
  }

}

