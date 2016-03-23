import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {IBundle, RuleService} from "../../../../api/rule-engine/Rule";
import {GoogleMapDialogComponent} from "./google-map-dialog.component";
@Component({
  selector: 'cw-google-map-dialog-container',
  directives: [CORE_DIRECTIVES, GoogleMapDialogComponent],
  template: `
  <cw-push-publish-dialog-component
  [bundleStores]="ruleService.bundles$ | async"
  [hidden]="hidden"
  (cancel)="hidden = true; close.emit($event); "
  (addToBundle)="addToBundle($event)"
  ></cw-push-publish-dialog-component>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class GoogleMapDialogContainer {
  @Input() assetId:string
  @Input() hidden:boolean = false

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)


  private bundleStoreSelected:IBundle;
  private addNewBundle:Boolean

  bundleStores:IBundle[] = []

  constructor(public ruleService:RuleService) {
    this.ruleService.loadBundleStores()
  }

  ngOnChanges(change){
    if(change.hidden) {
      console.log("GoogleMapDialogContainer", "ngOnChanges", change.hidden.currentValue, change.hidden.previousValue)
    }
  }


  addToBundle(bundle:IBundle) {
    debugger
    this.ruleService.addRuleToBundle(this.assetId, event).subscribe((result:any)=> {
      if (!result.errors) {
        this.hidden = false;
      } else {
        console.log("Show error to user") //TODO: need to show error to use
      }
    })
  }

}

