import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "angular2/core";
import {CORE_DIRECTIVES} from "angular2/common";
@Component({
  selector: 'cw-modal-dialog',
  directives: [CORE_DIRECTIVES],
  template: `
  <div class="ui dimmer modals page transition visible active" *ngIf="!hidden" (click)="onCancel($event)">
    <div class="ui modal cw-modal-dialog" style="height:{{height}};width:{{width}};max-height:{{maxHeight}};max-width:{{maxWidth}}" (click)="$event.stopPropagation()">
      <div class="header">{{headerText}}</div>
      <div flex layout-fill layout="column" class="content">
        <div *ngIf="errorMessage != null" class="ui negative message">{{errorMessage}}</div>
        <ng-content></ng-content>
      </div>
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
  @Input() errorMessage:string = null

  @Input() height:string = '60%'
  @Input() width:string= '50%'
  @Input() maxHeight:string = '300em'
  @Input() maxWidth:string = '200em'

  @Output() close:EventEmitter<{isCanceled:boolean}> = new EventEmitter(false)
  @Output() cancel:EventEmitter<boolean> = new EventEmitter(false)
  @Output() ok:EventEmitter<boolean> = new EventEmitter(false)

  private _keyListener:any

  constructor() { }

  ngOnChanges(change) {
    if (change.hidden) {
      if (!this.hidden) {
        this.addEscapeListener()
      } else if (this._keyListener != null) {
        this.removeEscapeListener()
      }
    }
  }

  private addEscapeListener() {
    this._keyListener = document.body.addEventListener('keyup', (e)=> {
      if (e.keyCode == 27) { // escape
        e.preventDefault()
        e.stopPropagation()
        this.cancel.emit(false)
      }else if(e.keyCode == 13){
        e.stopPropagation();
        e.preventDefault();
        this.ok.emit();
      }
    })
  }

  private removeEscapeListener() {
    document.body.removeEventListener('keyup', this._keyListener)
  }

  onCancel(e){
    this.cancel.emit(true)
  }
}