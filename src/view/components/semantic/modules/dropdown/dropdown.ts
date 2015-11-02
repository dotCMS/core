/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import { NgClass, ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute, NgFor} from 'angular2/angular2';

/**
 * Angular 2 wrapper around Semantic UI Dropdown Module.
 * @see http://semantic-ui.com/modules/dropdown.html#/usage
 * Comments for event handlers, etc, copied directly from semantic-ui documentation.
 *
 * @todo ggranum: Extract semantic UI components into a separate github repo and include them via npm.
 */


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
  }
}
export class DropdownModel {
  name:string
  placeholder:string
  selected:Array<string>
  options:Array<DropdownOption>
  settings: { maxSelections?: number }

  constructor(name:string = null,
              placeholder:string = '',
              selected:Array<string> = [],
              options:Array<DropdownOption> = []) {
    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.selected = selected
    this.options = options
    this.settings = {}
  }


}

@Component({
  selector: 'cw-input-dropdown',

  properties: [
    'model',
  ], events: [
    "change"
  ]
})
@View({
  template: `
<div class="ui fluid selection dropdown" [ng-class]="{search: model.settings.maxSelections, multiple: model.settings.maxSelections}" tabindex="0">
  <input type="hidden" [name]="model.name" [value]="model.selected.join(',')" >
  <i class="dropdown icon"></i>
  <div class="default text">{{model.placeholder}}</div>
  <div class="menu" tabindex="-1">
    <div *ng-for="var opt of model.options" class="item" [attr.data-value]="opt.id" [attr.data-text]="opt.label">
      <i [ng-class]="opt.icon + ' icon'" ></i>
      {{opt.label}}
    </div>
  </div>
</div>
  `,
  directives: [NgClass, NgFor]
})
export class Dropdown {

  private _model:DropdownModel

  change:EventEmitter
  private elementRef:ElementRef

  constructor(@ElementRef elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._model = new DropdownModel()
  }

  afterViewInit() {
    console.log("view init")
    var self = this;
    let config:any = {
      onChange: (value, text, $choice)=> {
        return this.onChange(value, text, $choice)
      },
      onAdd: (addedValue, addedText, $addedChoice)=> {
        return this.onAdd(addedValue, addedText, $addedChoice)
      },
      onRemove: (removedValue, removedText, $removedChoice)=> {
        return this.onRemove(removedValue, removedText, $removedChoice)
      },
      onLabelCreate: function(value, text) {
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

    if(this.model.settings.maxSelections){
      config.maxSelections = this.model.settings.maxSelections
    }

    var el:Element = this.elementRef.nativeElement
    //noinspection TypeScriptValidateTypes
    $(el).children('.ui.dropdown').dropdown(config)


  }

  get model():DropdownModel {
    return this._model;
  }

  set model(model:DropdownModel) {
    this._model = model;
  }

  /**
   * Is called after a dropdown value changes. Receives the name and value of selection and the active menu element
   * @param value
   * @param text
   * @param $choice
   */
  onChange(value, text, $choice) {
    this.model.selected = [value]
    this.change.next({type: 'toggle', target: this, value: value})
    console.log('onChange', value, text, " Selected: ", this.model.selected)
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
    console.log('onNoResults', searchValue)
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

