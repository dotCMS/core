import { NgClass, ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute, NgFor} from 'angular2/angular2';
//import * as Rx from '../../../../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'

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
    if(!this.icon.includes(' ') && this.icon.length > 0){
      this.icon = (this.icon + ' icon').trim()
    }
  }
}
export class DropdownModel {
  name:string
  placeholder:string
  selected:Array<string>
  options:Array<DropdownOption>
  settings:{ maxSelections?: number }
  private _optionChange:EventEmitter
  onOptionChange:Rx.Observable<any>
  allowAdditions: boolean


  constructor(name:string = null,
              placeholder:string = '',
              selected:Array<string> = [],
              options:Array<DropdownOption> = [],
              allowAdditions:boolean = false) {
    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.selected = selected
    this.options = options
    this.settings = {}
    this._optionChange = new EventEmitter()
    this.onOptionChange = Rx.Observable.from(this._optionChange.toRx()).share()
    this.allowAdditions = allowAdditions
  }

  addOptions(options:Array<DropdownOption>) {
    options.forEach((option) => {
      this.options.push(option)
    })
    this._optionChange.next({type: 'add', target: this, value: options})
  }

  selectedValues():Array<any>{
    return this.selected.map((selectedId)=>{
      var ddOpt = this.options.filter((opt)=>{ return (opt.id == selectedId) })[0]
      if(ddOpt){
        // if not, then 'selected' is a value that isn't in the list!
        return ddOpt.value
      }
    })
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
<div class="ui fluid selection dropdown search" [ng-class]="{multiple: model.settings.maxSelections}" tabindex="0">
  <input type="hidden" [name]="model.name" [value]="model.selected.join(',')" >
  <i class="dropdown icon"></i>
  <div class="default text">{{model.placeholder}}</div>
  <div class="menu" tabindex="-1">
    <div *ng-for="var opt of model.options" class="item" [attr.data-value]="opt.id" [attr.data-text]="opt.label">
      <i [ng-class]="opt.icon" ></i>
      {{opt.label}}
    </div>
  </div>
</div>
  `,
  directives: [NgClass, NgFor]
})
export class Dropdown {

  change:EventEmitter
  private _model:DropdownModel
  private optionWatch:Rx.Subscription<any>
  private elementRef:ElementRef

  private updateView:boolean

  constructor(@ElementRef elementRef:ElementRef) {

    this.elementRef = elementRef
    this.change = new EventEmitter()
    this.model = new DropdownModel()
    this.updateView = false
  }


  get model():DropdownModel {
    return this._model;
  }

  set model(model:DropdownModel) {
    if (this.optionWatch) {
      this.optionWatch.unsubscribe()
      this.optionWatch = null
    }
    this._model = model;
    this.optionWatch = this._model.onOptionChange.subscribe(()=> {
      this.updateView = true
    })

  }

  afterViewInit() {
    if (this.model.options.length > 0) {
      this.initDropdown()
    } // else 'wait for options to be set'
  }

  afterViewChecked() {
    if (this.updateView === true) {
      this.updateView = false
      this.initDropdown()
    }
  }

  initDropdown() {
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
    if (this.model.settings.maxSelections) {
      config.maxSelections = this.model.settings.maxSelections
    }

    var el = this.elementRef.nativeElement
    var $dropdown = $(el).children('.ui.dropdown');
    $dropdown.dropdown(config)
    this._applyArrowNavFix($dropdown);

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

