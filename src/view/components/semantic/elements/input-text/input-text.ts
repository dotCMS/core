import {bootstrap} from 'angular2/bootstrap'
import {
    Attribute,
    ChangeDetectionStrategy,
    Component,
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    Optional
} from 'angular2/core'
import {Control, Validators, NgControl, ControlValueAccessor} from 'angular2/common'
import {isBlank} from 'angular2/src/facade/lang';
/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 */
@Component({
  selector: 'cw-input-text',
  host: {'role': 'text'},
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div flex layout="row" layout-wrap class="ui fluid input" [ngClass]="{disabled: disabled, icon: icon, required: required}">
    <input flex
           type="text"
           [value]="_modelValue"
           [disabled]="disabled"
           placeholder="{{placeholder}}"
           (change)="onChange($event.target.value)" />
    <i flex="5" [ngClass]="icon" *ngIf="icon"></i>
</div>
  `,
  directives: []
})
export class InputText implements ControlValueAccessor {

  @Input() control:Control
  @Input() placeholder:string = ""
  @Input() icon:string
  @Input() disabled:boolean = false
  @Input() focused:boolean = false
  @Input() required:boolean = false

  errorMessage:string
  _modelValue:any
  onChange:Function
  onTouched:Function

  constructor( @Optional() control:NgControl, private _elementRef:ElementRef) {
    if(control){
      control.valueAccessor = this;
    }
  }

  ngOnChanges(change) {
    if (change.focused) {
      let f = change.focused.currentValue === true || change.focused.currentValue == 'true'
      if (f) {
        let el = this._elementRef.nativeElement
        el.children[0].children[0].focus()
      }
      this.focused = false;
    }
  }

  writeValue(value:any) {
    this._modelValue = isBlank(value) ? '' : value
  }

  registerOnChange(fn) { this.onChange = fn; }

  registerOnTouched(fn) { this.onTouched = fn; }
}

