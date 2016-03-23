import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
import {ModalDialogComponent} from "../dialog-component";
import {IPublishEnvironment, RuleService} from "../../../../api/rule-engine/Rule";
import {Dropdown, InputOption} from "../../semantic/modules/dropdown/dropdown";

@Component({
  selector: 'cw-push-publish-dialog-component',
  directives: [CORE_DIRECTIVES, ModalDialogComponent, Dropdown, InputOption],
  template: `<cw-modal-dialog
    [headerText]="'Push Publish'"
    [okText]="'Push'"
    [hidden]="hidden"
    [okEnabled]="selectedEnvironment != null"
    [errorMessage]="errorMessage"
    (ok)="doPushPublish.emit(selectedEnvironment)"
    (cancel)="cancel.emit()">
  <cw-input-dropdown
      flex
      [value]="environmentStores[0]?.id"
      (click)="$event.stopPropagation()"
      (change)="setselectedEnvironment($event)">
    <cw-input-option
        *ngFor="#opt of environmentStores"
        [value]="opt.id"
        [label]="opt.name"
    ></cw-input-option>
  </cw-input-dropdown>
</cw-modal-dialog>`
  , changeDetection: ChangeDetectionStrategy.OnPush
})
export class PushPublishDialogComponent {
  @Input() hidden:boolean = false
  @Input() environmentStores:IPublishEnvironment[];
  @Input() errorMessage:string = null

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() doPushPublish:EventEmitter<IPublishEnvironment> = new EventEmitter(false)

  public selectedEnvironmentId:string;

  constructor() { }

  ngOnChanges(change){
    if (change.environmentStores) {
      this.selectedEnvironment = change.environmentStores.currentValue[0];
      console.log("PushPublishDialogComponent", "ngOnChanges", change.environmentStores.currentValue)
    }
  }

  setselectedEnvironment(environmentId:string) {
    this.selectedEnvironmentId = environmentId
  }
}

