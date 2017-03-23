import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from "@angular/core";

@Component({
  selector: 'cw-modal-dialog',
  directives: [],
  template: `
  <div class="ui dimmer modals page transition visible active" *ngIf="!hidden" (click)="onCancel($event)">
    <div class="ui modal cw-modal-dialog" style="height:{{height}};width:{{width}};max-height:{{maxHeight}};max-width:{{maxWidth}}" (click)="$event.stopPropagation()">
      <div class="header">{{headerText}}</div>
      <div flex layout-fill layout="column" class="content">
        <div *ngIf="errorMessage != null" class="ui negative message">{{errorMessage}}</div>
        <ng-content></ng-content>
      </div>
      <div class="actions">
        <div class="ui positive right labeled icon button" [class.disabled]="!okEnabled" (click)="ok.emit()">{{okButtonText}}
          <i class="checkmark icon"></i>
        </div>
        <div class="ui black deny button" (click)="cancel.emit(true)">Cancel</div>
      </div>
    </div>
  </div>
`, changeDetection: ChangeDetectionStrategy.OnPush
})
export class ModalDialogComponent {

  @Input() okEnabled:boolean = true
  @Input() hidden:boolean = true
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
  @Output() open:EventEmitter<boolean> = new EventEmitter(false)

  private _keyListener:any

  constructor() { }

  ngOnChanges(change) {
    if (change.hidden) {
      if (!this.hidden) {
        this.addEscapeListener()

        //wait until the dialog is really show up
        setTimeout( () => this.open.emit(false), 2);
      } else {
        this.removeEscapeListener()
      }
    }
  }

  private addEscapeListener() {
    if (!this._keyListener) {
      this._keyListener = (e)=> {
        if (e.keyCode == 27) { // escape
          e.preventDefault()
          e.stopPropagation()
          this.cancel.emit(false)
        } else if (e.keyCode == 13) { //enter
          e.stopPropagation();
          e.preventDefault();
          this.ok.emit(true);
        }
      }
      document.body.addEventListener('keyup', this._keyListener)
    }
  }

  private removeEscapeListener() {
    if(this._keyListener){
      document.body.removeEventListener('keyup', this._keyListener)
      this._keyListener = null
    }
  }

  onCancel(e){
    this.cancel.emit(true)
  }
}