import {ElementRef, Component, Directive, EventEmitter, Optional} from '@angular/core';
import { Host, AfterViewInit, OnDestroy, Output, Input, ChangeDetectionStrategy } from '@angular/core';
import {ControlValueAccessor, NgControl} from '@angular/forms';
import {BehaviorSubject, Observable} from 'rxjs/Rx'
import _ from 'lodash';


/**
 * Angular 2 wrapper around Semantic UI Dropdown Module.
 * @see http://semantic-ui.com/modules/dropdown.html#/usage
 * Comments for event handlers, etc, copied directly from semantic-ui documentation.
 *
 * @todo ggranum: Extract semantic UI components into a separate github repo and include them via npm.
 */
var $ = window['$']

const DO_NOT_SEARCH_ON_THESE_KEY_EVENTS = {

  13: 'enter',
  33: 'pageUp',
  34: 'pageDown',
  37: 'leftArrow',
  38: 'upArrow',
  39: 'rightArrow',
  40: 'downArrow',
}


@Component({
  selector: 'cw-input-dropdown',
  template: `<div class="ui fluid selection dropdown search ng-valid"
     [class.required]="minSelections > 0"
     [class.multiple]="maxSelections > 1"
     tabindex="0"
     (change)="$event.stopPropagation()"
     (blur)="$event.stopPropagation()">
  <input type="hidden" [name]="name" [value]="_modelValue" />
  <i class="dropdown icon"></i>
  <div class="default text">{{placeholder}}</div>
  <div class="menu" tabindex="-1">
    <div *ngFor="let opt of _options | async" class="item" [attr.data-value]="opt.value" [attr.data-text]="opt.label">
      <i [ngClass]="opt.icon" ></i>
      {{opt.label}}
    </div>
  </div>
</div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dropdown implements AfterViewInit, OnDestroy, ControlValueAccessor  {

  @Input() name:string
  @Input() placeholder:string
  @Input() allowAdditions:boolean
  @Input() minSelections:number
  @Input() maxSelections:number

  @Output() change:EventEmitter<any> = new EventEmitter()
  @Output() touch:EventEmitter<any> = new EventEmitter()
  @Output() enter:EventEmitter<boolean> = new EventEmitter(false)

  onChange:Function = (  ) => { }
  onTouched:Function = (  ) => { }
  private _modelValue:string


  private _optionsAry:InputOption[] = []
  private _options:BehaviorSubject<InputOption[]>
  private elementRef:ElementRef
  private _$dropdown:any

  constructor(elementRef:ElementRef, @Optional() control:NgControl) {
    if(control && !control.valueAccessor){
      control.valueAccessor = this;
    }
    this.placeholder = ""
    this.allowAdditions = false
    this.minSelections = 0
    this.maxSelections = 1
    this._options = new BehaviorSubject(this._optionsAry);
    this.elementRef = elementRef
  }

  @Input()
  set value(value: string) {
    this._modelValue = value;
  }

  focus(){
    try{
      this._$dropdown.children('input.search')[0].focus();
    }
    catch(e){
       console.log("Dropdown", "could not focus search element")
    }

  }

  writeValue(value:any) {
    this._modelValue = _.isEmpty(value) ? '' : value;
    this.applyValue(this._modelValue)
  }

  registerOnChange(fn) {
    this.onChange = fn;
  }

  registerOnTouched(fn) {
    this.onTouched = fn;
  }

  fireChange($event){
    if(this.change){
      this.change.emit($event)
      this.onChange($event)
    }
  }

  fireTouch($event){
    this.touch.emit($event)
    this.onTouched($event)
  }

  ngOnChanges(change) {

  }

  private applyValue(value) {
    var count = 0;
    Observable.interval(10).takeWhile(()=> {
      count++
      if (count > 100) {
        throw "Dropdown element not found."
      }
      return this._$dropdown == null
    }).subscribe(()=> {
      // still null!
    }, (e)=> {
      console.log("Dropdown", "Error", e)
    }, ()=> {
      console.log("Dropdown", "onComplete")
      this._$dropdown.dropdown('set selected', value)
    })
  }

  hasOption(option:InputOption):boolean {
    let x = this._optionsAry.filter((opt)=>{
      return option.value == opt.value
    })
    return x.length !== 0
  }

  addOption(option:InputOption) {
    this._optionsAry = this._optionsAry.concat(option)
    this._options.next(this._optionsAry)
    if(option.value === this._modelValue){
      this.refreshDisplayText(option.label)
    }
  }

  updateOption(option:InputOption){
    this._optionsAry = this._optionsAry.filter((opt)=>{
      return opt.value !== option.value
    })
    this.addOption(option)
  }

  ngAfterViewInit() {
    this.initDropdown()
  }

  ngOnDestroy(){
    // remove the change emitter so that we don't fire changes when we clear the dropdown.
    this.change = null;
    this._$dropdown.dropdown('clear')
  }

  refreshDisplayText(label:string) {
    if (this._$dropdown) {
      this._$dropdown.dropdown('set text', label)
    }
  }

  initDropdown() {
    var self = this;
    var badSearch = null
    let config:any = {
      allowAdditions: this.allowAdditions,
      allowTab: true,
      placeholder: 'auto',

      onChange: (value, text, $choice)=> {
        badSearch = null
        return this.onChangeSemantic(value, text, $choice)
      },
      onAdd: (addedValue, addedText, $addedChoice)=> {
        return this.onAdd(addedValue, addedText, $addedChoice)
      },
      onRemove: (removedValue, removedText, $removedChoice)=> {
        return this.onRemove(removedValue, removedText, $removedChoice)
      },
      onLabelCreate: function (value, text) {
        let $label = this;
        return self.onLabelCreate($label, value, text)
      },
      onLabelSelect: ($selectedLabels)=> {
        return this.onLabelSelect($selectedLabels)
      },
      onNoResults: (searchValue)=> {
        if(!this.allowAdditions){
            badSearch = searchValue
        }
        return this.onNoResults(searchValue)
      },
      onShow: ()=> {
        return this.onShow()
      },
      onHide: ()=> {
        if(badSearch !== null){
          badSearch = null
          this._$dropdown.children('input.search')[0].value = ''
          if(!this._modelValue || (this._modelValue && this._modelValue.length === 0)){
            this._$dropdown.dropdown('set text', this.placeholder)
          } else {
            this._$dropdown.dropdown('set selected', this._modelValue)
          }
        }
        return this.onHide()
      }
    }
    if (this.maxSelections > 1) {
      config.maxSelections = this.maxSelections
    }

    var el = this.elementRef.nativeElement
    this._$dropdown = $(el).children('.ui.dropdown');
    this._$dropdown.dropdown(config)
    this._applyArrowNavFix(this._$dropdown);
  }

  private isMultiSelect():boolean {
    return this.maxSelections > 1
  }

  /**
   * Fixes an issue with up and down arrows triggering a search in the dropdown, which auto selects the first result
   * after a short buffering period.
   * @param $dropdown The JQuery dropdown element, after calling #.dropdown(config).
   * @private
   */
  private _applyArrowNavFix($dropdown) {
    let $searchField = $dropdown.children('input.search')
    let enterEvent = this.enter;
    $searchField.on('keyup', (event:any)=> {
      if (DO_NOT_SEARCH_ON_THESE_KEY_EVENTS[event.keyCode]) {
        if (event.keyCode == 13 && enterEvent){
          enterEvent.emit(true);
        }

        event.stopPropagation()
      }
    })
  };


  /**
   * Is called after a dropdown value changes. Receives the name and value of selection and the active menu element
   * @param value
   * @param text
   * @param $choice
   */
  onChangeSemantic(value, text, $choice) {
    this._modelValue = value
    this.fireChange(value)
  }

  /**
   * Is called after a dropdown selection is added using a multiple select dropdown, only receives the added value
   * @param addedValue
   * @param addedText
   * @param $addedChoice
   */
  onAdd(addedValue, addedText, $addedChoice) {
  }

  /**
   * Is called after a dropdown selection is removed using a multiple select dropdown, only receives the removed value
   * @param removedValue
   * @param removedText
   * @param $removedChoice
   */
  onRemove(removedValue, removedText, $removedChoice) {
  }

  /**
   * Allows you to modify a label before it is added. Expects $label to be returned.
   * @param $label
   * @param value
   * @param text
   */
  onLabelCreate($label, value, text) {
    return $label
  }

  /**
   * Is called after a label is selected by a user
   * @param $selectedLabels
   */
  onLabelSelect($selectedLabels) {
  }

  /**
   * Is called after a dropdown is searched with no matching values
   * @param searchValue
   */
  onNoResults(searchValue) {

  }

  /**
   * Is called before a dropdown is shown. If false is returned, dropdown will not be shown.
   */
  onShow() {
  }

  /**
   * Is called before a dropdown is hidden. If false is returned, dropdown will not be hidden.
   */
  onHide() {
    this.touch.emit(this._modelValue)
    this.onTouched()
  }
}


@Directive({
  selector: 'cw-input-option'
})
export class InputOption {
  @Input() value:string;
  @Input() label:string;
  @Input() icon:string;
  private _dropdown:Dropdown
  private _isRegistered:boolean


  constructor(@Host() dropdown:Dropdown) {
    this._dropdown = dropdown
  }

  ngOnChanges(change) {
    if (!this._isRegistered) {
      if(this._dropdown.hasOption(this)){
        this._dropdown.updateOption(this);
      } else{
        this._dropdown.addOption(this);
      }
      this._isRegistered = true;
    } else {
      if(!this.label){
        this.label = this.value
      }
      this._dropdown.updateOption(this)
    }
  }
}


