import { ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute} from 'angular2/angular2';
import { AfterViewInit, AfterViewChecked} from 'angular2/angular2';
import { CORE_DIRECTIVES } from 'angular2/angular2';
import { Output, Input, ChangeDetectionStrategy } from 'angular2/angular2';
import * as Rx from 'rxjs/Rx.KitchenSink'
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwInputDefinition} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ParameterDefinition} from "../../../../../api/util/CwInputModel";
import {ParameterModel} from "../../../../../api/rule-engine/Condition";


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

/**
 *
 */
export class DropdownOption {
  id:string
  value:any
  label:string
  icon:string

  constructor(valueId:string, value:any = null, label:string = null, icon:string = null) {
    this.id = valueId
    this.value = value || valueId
    this.label = label || valueId
    this.icon = icon || ''
    if (!this.icon.includes(' ') && this.icon.length > 0) {
      this.icon = (this.icon + ' icon').trim()
    }
  }

  static toJson(opt:DropdownOption):any {
    return {id: opt.id, value: opt.value, label: opt.label, icon: opt.icon}
  }

  static fromJson(json):DropdownOption {
    return new DropdownOption(json.id, json.value, json.label, json.icon)
  }
}

export class DropdownModel extends CwComponent {
  name:string
  placeholder:string
  selected:Array<string>
  options:Array<DropdownOption>
  allowAdditions:boolean
  maxSelections:number
  minSelections:number


  constructor(name:string = null,
              placeholder:string = '',
              selected:Array<string> = [],
              options:Array<DropdownOption> = [],
              allowAdditions:boolean = false,
              minSelections:number = 0,
              maxSelections:number = 1) {
    super()
    this.name = name != null ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.selected = selected != null ? selected :[]
    this.options = options != null ? options : []
    this.allowAdditions = allowAdditions
    this.minSelections = minSelections
    this.maxSelections = maxSelections
  }

  static fromParameter(param:ParameterModel, paramDef:ParameterDefinition):DropdownModel {
    try {
      let inputType:CwDropdownInputModel = <CwDropdownInputModel>paramDef.inputType;
      let opts = []
      let options = inputType.options;
      Object.keys(options).forEach((key:any)=> {
        let option = options[key]
        opts.push(new DropdownOption(key, option.value))
      })
      console.log("DD-PARAM:", param.key, param.value, param)
      let dd =  new DropdownModel(param.key, inputType.placeholder, [param.value], opts)
      dd.minSelections = inputType.minSelections
      dd.maxSelections = inputType.maxSelections
      if(!param.value) {
        dd.selected = inputType.selected != null ? inputType.selected : []
      }
      dd['required'] = dd.minSelections > 0
      return dd
    } catch(e){
      console.log("Could not create Dropdown Model from param", param, paramDef,  e)
    }
  }

  addOptions(options:Array<DropdownOption>) {
    options.forEach((option) => {
      this.options.push(option)
    })
  }

  selectedValues():Array<any> {
    return this.selected.map((selectedId)=> {
      var ddOpt = this.options.filter((opt)=> {
        return (opt.id == selectedId)
      })[0]
      if (ddOpt) {
        // if not, then 'selected' is a value that isn't in the list!
        return ddOpt.value
      }
    })
  }


  toJson():any {
    let optsJson = {}
    this.options.forEach((opt:DropdownOption) => {
      optsJson[opt.id] = DropdownOption.toJson(opt)
    })
    return {
      name: this.name,
      placeholder: this.placeholder,
      selected: this.selected,
      options: optsJson,
      allowAdditions: this.allowAdditions,
      minSelections: this.minSelections,
      maxSelections: this.maxSelections
    }
  }

  static fromJson(json:any):DropdownModel {
    let optsJson = json.options != null ? json.options : {}
    let options = []
    Object.keys(optsJson.options).forEach((opt)=> {
      options.push(DropdownOption.fromJson(opt))
    })
    return new DropdownModel(
        json.name,
        json.placeholder,
        json.selected,
        json.options,
        json.allowAdditions,
        json.minSelections,
        json.maxSelections);
  }
}

@Component({
  selector: 'cw-input-dropdown',
  changeDetection: ChangeDetectionStrategy.OnPush
})
@View({
  template: `
<div class="ui fluid selection dropdown search ng-valid" [ngClass]="{required:model.minSelections > 0, multiple: model.maxSelections > 1}" tabindex="0">
  <input type="hidden" [name]="model.name" [value]="getSelected()" >
  <i class="dropdown icon"></i>
  <div class="default text">{{model.placeholder}}</div>
  <div class="menu" tabindex="-1">
    <div *ngFor="var opt of model.options" class="item" [attr.data-value]="opt.id" [attr.data-text]="opt.label">
      <i [ngClass]="opt.icon" ></i>
      {{opt.label}}
    </div>
  </div>
</div>
  `,
  directives: [CORE_DIRECTIVES]
})
export class Dropdown implements AfterViewInit, AfterViewChecked {

  @Input() model:DropdownModel
  @Output() change:EventEmitter<any>

  private elementRef:ElementRef
  private _updateView:boolean
  private _viewIsInitialized:boolean

  constructor(elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._updateView = false
    this._viewIsInitialized = false
    this.model = new DropdownModel();
  }

  ngOnChanges(change) {
    if (change.model) {
      this._updateView = true
      this.initDropdown()
    }
  }

  getSelected(){
    let sel = this.model.selected;
    if(sel != null){
      return sel.join(',');
    }
  }

  ngAfterViewInit() {
    this._viewIsInitialized = true
    if (this.model.options.length > 0) {
      this.initDropdown()
    } // else 'wait for options to be set'

  }

  ngAfterViewChecked() {
    if (this._updateView === true) {
      this.initDropdown()
    }
  }

  initDropdown() {
    if (this._viewIsInitialized) {
      this._updateView = false
      var self = this;
      let config:any = {
        allowAdditions: this.model.allowAdditions,

        onChange: (value, text, $choice)=> {
          return this.onChange(value, text, $choice)
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
          return this.onNoResults(searchValue)
        },
        onShow: ()=> {
          return this.onShow()
        },
        onHide: ()=> {
          return this.onHide()
        }
      }
      if (this.model.maxSelections > 1) {
        config.maxSelections = this.model.maxSelections
      }

      var el = this.elementRef.nativeElement
      var $dropdown = $(el).children('.ui.dropdown');
      $dropdown.dropdown(config)
      this._applyArrowNavFix($dropdown);
    }
  }

  private isMultiSelect():boolean {
    return this.model.maxSelections > 1
  }

  /**
   * Fixes an issue with up and down arrows triggering a search in the dropdown, which auto selects the first result
   * after a short buffering period.
   * @param $dropdown The JQuery dropdown element, after calling #.dropdown(config).
   * @private
   */
  private _applyArrowNavFix($dropdown) {
    let $searchField = $dropdown.children('input.search')
    $searchField.on('keyup', (event)=> {
      if (DO_NOT_SEARCH_ON_THESE_KEY_EVENTS[event.keyCode]) {
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
  onChange(value, text, $choice) {
    this.model.selected = [value]
    if (this.isMultiSelect()) {
      this.change.emit(this.model.selectedValues())
    } else {
      this.change.emit(this.model.selectedValues()[0])
    }

    console.log('dropdown','onChange', value, text, " Selected: ", this.model.selected)
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
  }


}

