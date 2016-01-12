import { ChangeDetectionStrategy, Component, View, TemplateRef, EventEmitter, ElementRef, Input, Output, Provider, Renderer, forwardRef} from 'angular2/core';
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
<div class="ui fluid input" [ngClass]="{disabled: disabled, error: errorMessage, icon: icon, required: required}">
  <input [type]="type" [name]="name" [value]="value" [placeholder]="placeholder" [disabled]="disabled"
    class="ng-valid"
    [required]="required"
    (input)="onChange($event.target.value)"
    (change)="$event.stopPropagation(); onChange($event.target.value)"
    (blur)="onBlur($event.target.value)"
    (focus)="onFocus($event.target.value)">
  <i [ngClass]="icon" *ngIf="icon"></i>
  <div class="ui small red message" *ngIf="errorMessage">{{errorMessage}}</div>
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

  @Input()  name:string = ""
  @Input()  placeholder:string = ""
  @Input()  value:string = ""
  @Input()  disabled:boolean = false
  @Input()  icon:string
  @Input()  type:string
  @Input()  focused:boolean = false

  //private _model:InputDateModel
  private errorMessage:String

  @Output() change:EventEmitter<any>
  @Output() blur:EventEmitter<any>
  @Output() focus:EventEmitter<any>
  //private elementRef:ElementRef

  constructor(private _renderer:Renderer, private _elementRef:ElementRef) {
    //this.elementRef = elementRef
    this.change = new EventEmitter()
    this.blur = new EventEmitter()
    this.focus = new EventEmitter()
    //this._model = new InputDateModel()
    this.errorMessage = null
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
}

