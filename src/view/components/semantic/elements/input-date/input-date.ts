import { ChangeDetectionStrategy, Component, View, EventEmitter, ElementRef, Input, Output, Provider, Renderer, forwardRef} from 'angular2/core';
import { CORE_DIRECTIVES, NG_VALUE_ACCESSOR, ControlValueAccessor } from 'angular2/common';

import {isBlank, CONST_EXPR} from 'angular2/src/facade/lang';

const CW_TEXT_VALUE_ACCESSOR = CONST_EXPR(new Provider(
    NG_VALUE_ACCESSOR, {useExisting: forwardRef(() => InputDate), multi: true}));
/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 */

@Component({
  selector: 'cw-input-date',
  changeDetection: ChangeDetectionStrategy.OnPush,
  bindings: [CW_TEXT_VALUE_ACCESSOR]
})
@View({
  template: `
<div class="ui fluid input" [ngClass]="{disabled: disabled, icon: icon, required: required}">
  <input type="datetime-local" [value]="value" [placeholder]="placeholder" [disabled]="disabled"
    [required]="required"
    (input)="onChange($event.target.value)"
    (change)="$event.stopPropagation(); onChange($event.target.value)"
    (blur)="$event.stopPropagation(); onBlur($event.target.value)"
    (focus)="onFocus($event.target.value)">
  <i [ngClass]="icon" *ngIf="icon"></i>
</div>
  `,
  directives: [CORE_DIRECTIVES]
})
export class InputDate implements ControlValueAccessor  {

  onChange = (_) => {
    this.change.emit(_)
  };
  onTouched = () => {
  };

  private static DEFAULT_VALUE:string = InputDate._defaultValue()
  @Input()  value:string = InputDate.DEFAULT_VALUE
  @Input()  placeholder:string = ""
  @Input()  icon:string
  @Input()  disabled:boolean = false
  @Input()  focused:boolean = false
  @Input()  required:boolean = false
  @Input()  errorMessage:string
  @Output() change:EventEmitter<any>
  @Output() blur:EventEmitter<any>
  @Output() focus:EventEmitter<any>

  constructor(private _renderer:Renderer, private _elementRef:ElementRef) {

    this.change = new EventEmitter()
    this.blur = new EventEmitter()
    this.focus = new EventEmitter()
  }

  ngOnChanges(change) {
    if (change.value && change.value.currentValue === null) {
      this.value = InputDate.DEFAULT_VALUE
    }
    if(change.placeholder && !change.placeholder.currentValue){
      this.placeholder = "Enter an ISO DateTime"
    }
    if (change.focused) {
      let f = change.focused.currentValue === true || change.focused.currentValue == 'true'
      if (f) {
        let el = this._elementRef.nativeElement
        el.children[0].children[0].focus()
      }
      this.focused = false;
    }
  }

  onBlur(value) {
    this.onTouched()
    this.blur.emit(value)
  }

  onFocus(value) {
    this.focus.emit(value)
  }

  writeValue(value:string):void {
    this.value = isBlank(value) ? '' : value
    console.log("writing value: ", value, " ==> ", this.value)
  }

  registerOnChange(fn:(_:any) => void):void {
    this.onChange = (_:any) => {
      console.log("Value changed: ", _)
      fn(_)
      this.change.emit(_)
    }
  }

  registerOnTouched(fn:() => void):void {
    this.onTouched = () => {
      console.log("Touched")
      fn()
    }
  }

  private static _defaultValue():string {
    let d = new Date()
    d.setHours(0)
    d.setMinutes(0)
    d.setSeconds(0)
    d.setMilliseconds(0)
    d.setMonth(d.getMonth() + 1)
    d.setDate(1)
    let r = d.toISOString()
    console.log("InputDate", "_defaultValue", r)
    if(r.endsWith('Z'))
    {
      r = r.substring(0, r.length - 1)
    }
    return r
  }
}

