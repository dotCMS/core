import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {IPublishEnvironment, RuleService} from "../../../../api/rule-engine/Rule";
import {PushPublishDialogComponent} from "./push-publish-dialog-component";
import {BehaviorSubject} from "rxjs/Rx";

@Component({
  selector: 'cw-push-publish-dialog-container',
  directives: [CORE_DIRECTIVES, PushPublishDialogComponent],
  template: `
  <cw-push-publish-dialog-component
  [environmentStores]="ruleService.environments$ | async"
  [hidden]="hidden"
  [errorMessage]="errorMessage | async"
  (cancel)="hidden = true; close.emit($event); errorMessage = null;"
  (doPushPublish)="doPushPublish($event)"
  ></cw-push-publish-dialog-component>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class PushPublishDialogContainer {
  @Input() assetId:string
  @Input() hidden:boolean = false

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)

  errorMessage:BehaviorSubject<string> = new BehaviorSubject(null)

  environmentStores:IPublishEnvironment[] = []

  constructor(public ruleService:RuleService) {
    this.ruleService.loadPublishEnvironments()
  }

  ngOnChanges(change){
    if (change.hidden) {
      console.log("PushPublishDialogContainer", "ngOnChanges", change.hidden.currentValue, change.hidden.previousValue)
    }
  }

  doPushPublish(environment:IPublishEnvironment) {
    this.ruleService.pushPublishRule(this.assetId, environment.id).subscribe((result:any)=> {
      if (!result.errors) {
        this.close.emit({isCanceled:false})
        this.errorMessage = null
      } else {
        this.errorMessage.next("Sorry there was an error please try again")
      }
    })
  }

}

