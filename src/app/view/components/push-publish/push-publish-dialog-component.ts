import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from '@angular/core';
import {IPublishEnvironment} from '../../../api/services/bundle-service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-push-publish-dialog-component',
  template: `<cw-modal-dialog
    [headerText]="'Push Publish'"
    [okButtonText]="'Push'"
    [hidden]="hidden"
    [okEnabled]="selectedEnvironmentId != null"
    [errorMessage]="errorMessage"
    width="25em"
    height="auto"
    (ok)="doPushPublish.emit(selectedEnvironmentId)"
    (cancel)="cancel.emit()">
  <cw-input-dropdown
      flex
      [value]="environmentStores[0]?.id"
      (click)="$event.stopPropagation()"
      (change)="setSelectedEnvironment($event)">
    <cw-input-option
        *ngFor="let opt of environmentStores"
        [value]="opt.id"
        [label]="opt.name"
    ></cw-input-option>
  </cw-input-dropdown>
</cw-modal-dialog>`
})
export class PushPublishDialogComponent {
  @Input() hidden = false;
  @Input() environmentStores: IPublishEnvironment[];
  @Input() errorMessage: string = null;

  @Output() close: EventEmitter<{isCanceled: boolean}> = new EventEmitter(false);
  @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
  @Output() doPushPublish: EventEmitter<IPublishEnvironment> = new EventEmitter(false);

  public selectedEnvironmentId: string;

  constructor() { }

  ngOnChanges(change): void {
    if (change.environmentStores) {
      this.selectedEnvironmentId = change.environmentStores.currentValue[0];
    }
  }

  setSelectedEnvironment(environmentId: string): void {
    this.selectedEnvironmentId = environmentId;
  }
}