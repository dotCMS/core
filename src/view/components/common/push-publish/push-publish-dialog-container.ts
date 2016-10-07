import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "@angular/core";
import {PushPublishDialogComponent} from "./push-publish-dialog-component";
import {BehaviorSubject} from "rxjs/Rx";
import {BundleService, IPublishEnvironment} from "../../../../api/services/bundle-service";

@Component({
  selector: 'cw-push-publish-dialog-container',
  directives: [PushPublishDialogComponent],
  template: `
  <cw-push-publish-dialog-component
  [environmentStores]="environmentStores"
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
  @Input() environmentStores:IPublishEnvironment[];

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)

  errorMessage:BehaviorSubject<string> = new BehaviorSubject(null)

  constructor(public bundleService:BundleService) {}

  ngOnChanges(change){
  }

  doPushPublish(environment:IPublishEnvironment) {
    this.bundleService.pushPublishRule(this.assetId, environment.id).subscribe((result:any)=> {
      if (!result.errors) {
        this.close.emit({isCanceled:false})
        this.errorMessage = null
      } else {
        this.errorMessage.next("Sorry there was an error please try again")
      }
    })
  }

}

