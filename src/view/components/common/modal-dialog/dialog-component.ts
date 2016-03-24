import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
@Component({
  selector: 'cw-modal-dialog',
  directives: [CORE_DIRECTIVES],
  template: `
  <div class="ui dimmer modals page transition visible active" *ngIf="!hidden">
    <div class="ui modal cw-modal-dialog">
      <i class="close icon" (click)="cancel.emit({isCanceled:true})"></i>
      <div class="header">{{headerText}}</div>
      <ng-content></ng-content>
      <div class="actions">
        <div class="ui black deny button" (click)="cancel.emit(true)">Cancel</div>
        <div class="ui positive right labeled icon button" (click)="ok.emit()">{{okButtonText}}
          <i class="checkmark icon"></i>
        </div>
      </div>
    </div>
  </div>
`, changeDetection: ChangeDetectionStrategy.OnPush
})
export class ModalDialogComponent {
  
  @Input() okEnabled:boolean = true
  @Input() hidden:boolean = false
  @Input() headerText:string = ""
  @Input() okButtonText:string = "Ok"

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() ok:EventEmitter<boolean> = new EventEmitter(false)



  constructor() { }
}