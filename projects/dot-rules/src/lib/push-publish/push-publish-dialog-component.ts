import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from '@angular/core';
import {IPublishEnvironment} from '../services/bundle-service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-push-publish-dialog-component',
  template: `<p-dialog width="700" header="Push Publish" [visible]="!hidden" [modal]="true" [dismissableMask]="true" [closable]="false" appendTo="body" [draggable]="false" >
      <p-message  *ngIf="errorMessage" style="margin-bottom: 16px; display: block;" severity="error" [text]="errorMessage"></p-message>
      <cw-input-dropdown
        flex
        [value]="environmentStores[0]?.id"
        (onDropDownChange)="setSelectedEnvironment($event)">
        <cw-input-option
          *ngFor="let opt of environmentStores"
          [value]="opt.id"
          [label]="opt.name"
        ></cw-input-option>
      </cw-input-dropdown>
      <p-footer>
        <button type="button" pButton (click)="doPushPublish.emit(selectedEnvironmentId)" label="Push" [disabled]="!selectedEnvironmentId"></button>
        <button type="button" pButton (click)="cancel.emit()" label="Cancel" class="ui-button-secondary"></button>
      </p-footer>
    </p-dialog>`
})
export class PushPublishDialogComponent {
  @Input() hidden = false;
  @Input() environmentStores: IPublishEnvironment[];
  @Input() errorMessage: string = null;

  @Output() close: EventEmitter<{isCanceled: boolean}> = new EventEmitter(false);
  @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
  @Output() doPushPublish: EventEmitter<string> = new EventEmitter(false);

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
