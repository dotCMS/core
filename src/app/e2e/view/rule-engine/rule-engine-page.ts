import ElementArrayFinder = protractor.ElementArrayFinder;
import ElementFinder = protractor.ElementFinder;
import {
    Page,
    TestButton,
    TestInputDropdown,
    TestInputText,
    TestInputToggle,
    TestInputComponent
} from "../../../e2e/CwProtractor";


export class RulePage extends Page {
  filterBox:ElementFinder
  private _addRuleButton:TestButton
  ruleEls:ElementArrayFinder

  constructor(locale:string = null) {
    super(browser['testLoc']['coreWeb'] + '/build/index.html', '(Dev) dotCMS Core-Web');
    if(locale){
      this.queryParams['locale'] = locale
    }
    this.filterBox = element(by.css('.filter.icon + INPUT'))
    this._addRuleButton = new TestButton(element(by.css('.cw-button-add')))
    this.ruleEls = element.all(by.tagName('rule'))
  }

  setFilter(text:string){
    return this.filterBox.sendKeys(text);
  }

  getFilter():webdriver.promise.Promise<string>{
    return this.filterBox.getAttribute('value')
  }

  getFilterPlaceholder():webdriver.promise.Promise<string>{
    return this.filterBox.getAttribute('placeholder')
  }
  suppressAlerts(value:boolean){
    this.queryParams['suppressAlerts'] = value ? 'true' : 'false'
  }

  waitForSave(timeout:number = 250):webdriver.promise.Promise<void> {
    return browser.sleep(timeout)
  }

  saveAndReload():webdriver.promise.Promise<Page>{
    return this.waitForSave().then(() => this.navigateTo() )
  }

  addRule(nameSuffix:string=null):webdriver.promise.Promise<TestRuleComponent> {
    var name = "e2e-" + browser['browserName'] + '-' + new Date().getTime()
    if(nameSuffix){
      name = name + '-' + nameSuffix
    }
    return this._addRuleButton.click().then(()=> {
      let rule = this.firstRule()
      rule.setName(name)
      rule.fireOn.el.click()
      return rule
    })
  }

  ruleCount():webdriver.promise.Promise<number> {
    return this.ruleEls.count()
  }

  firstRule():TestRuleComponent {
    return new TestRuleComponent(this.ruleEls.first())
  }

  findRule(name:string):TestRuleComponent {
    let rule = element(by.xpath("//input[@value='"+ name + "']/ancestor::rule"))
    return new TestRuleComponent(rule)
  }

  rules():Promise<TestRuleComponent[]> {
    return new Promise<TestRuleComponent[]>((accept, reject) => {
      let ary:TestRuleComponent[] = []

      this.ruleEls.then((resolvedRuleEls:ElementFinder[])=> {
        resolvedRuleEls.forEach((ruleEl:ElementFinder)=> {
          ary.push(new TestRuleComponent(ruleEl))
        })
        accept(ary)
      })
    })
  }

  removeAllRules():Promise<any[]> {
    return new Promise((a, r)=> {
      this.ruleEls.each((ruleEl) => {
        new TestRuleComponent(ruleEl).remove()
      })
      a()
    })
  }
}

export class TestRuleComponent {
  el:ElementFinder
  header:ElementFinder
  mainBody:ElementFinder
  name:string
  nameEl:TestInputText
  fireOn:TestInputDropdown
  toggleEnable:TestInputToggle
  removeBtn:TestButton
  addGroupBtn:TestButton
  expandoCaret:ElementFinder
  conditionGroupEls:ElementArrayFinder
  actionEls:ElementArrayFinder
  addActionButtonEl:TestButton


  constructor(root:ElementFinder) {
    this.el = root;
    this.mainBody = root.element(by.css('.cw-accordion-body'))
    this.header = root.element(by.css('.cw-header'))
    this.expandoCaret = this.header.element(by.css('.cw-rule-caret'))
    this.nameEl = new TestInputText(root.element(by.css('.cw-rule-name-input')))
    this.fireOn = new TestInputDropdown(root.element(by.css('.cw-fire-on-dropdown')))
    this.toggleEnable = new TestInputToggle(root.element(by.tagName('cw-toggle-input')))
    this.removeBtn = new TestButton(root.element(by.css('.cw-delete-rule')))
    this.addGroupBtn = new TestButton(root.element(by.css('.cw-add-group')))
    this.conditionGroupEls = root.all(by.tagName('condition-group'))
    this.actionEls = root.all(by.tagName('rule-action'))
    this.addActionButtonEl = new TestButton(this.el.element(by.css(".cw-action-row .cw-button-add-item")))

  }

  setName(name:string):webdriver.promise.Promise<void>{
    this.name = name
    return this.nameEl.setValue(name)
  }

  getName():webdriver.promise.Promise<string>{
    let v = this.nameEl.getValue()
    v = v.then( name => this.name = name)
    return v
  }

  getFireOn():webdriver.promise.Promise<string>{
    return this.fireOn.getValueText()
  }

  setFireOn(value:string):webdriver.promise.Promise<void>{
    return this.fireOn.setSearch(value)
  }

  isShowingBody():webdriver.promise.Promise<boolean> {
    return this.mainBody.isPresent()
  }

  expand():webdriver.promise.Promise<void> {
    return this.isShowingBody().then((yup)=> {
      if (!yup) {
        return this.expandoCaret.click()
      }
    })
  }

  firstGroup():TestConditionGroupComponent {
    return new TestConditionGroupComponent(this.conditionGroupEls.first())
  }

  lastGroup():TestConditionGroupComponent {
    return new TestConditionGroupComponent(this.conditionGroupEls.last())
  }


  getGroup(index:number):TestConditionGroupComponent {
    return new TestConditionGroupComponent(this.conditionGroupEls.get(index))
  }

  addAction():webdriver.promise.Promise<void> {
    return this.addActionButtonEl.click()
  }

  firstAction():TestActionComponent {
    return new TestActionComponent(this.actionEls.first())
  }

  lastAction():TestActionComponent {
    return new TestActionComponent(this.actionEls.last())
  }


  addGroup(isAnd:boolean=true):protractor.promise.Promise<void>{
    let p = this.addGroupBtn.click()
    if(!isAnd){
      p = p.then( () => this.lastGroup().toggleLogicalOperator() )
    }
    return p
  }



  createRequestHeaderCondition(key:string = "Accept",
                               condition:string = TestConditionComponent.COMPARE_IS,
                               value:string = "AbcDef",
                               isAnd:boolean =true
  ):TestRequestHeaderCondition {
    let condGroup = this.lastGroup()
    let conditionDef:TestRequestHeaderCondition = new TestRequestHeaderCondition(condGroup.last().el)
    conditionDef.typeSelect.setSearch("Request Hea")
    conditionDef.setHeaderKey(key)
    conditionDef.setComparison(condition)
    conditionDef.setHeaderValue(value)
    if(!isAnd){
      conditionDef.toggleLogicalTest()
    }
    condGroup.addCondition()
    return conditionDef
  }


  newSetResponseHeaderAction(key?:string, value?:string):TestResponseHeaderAction {
    let actionDef:TestResponseHeaderAction= new TestResponseHeaderAction(this.firstAction().el)
    actionDef.typeSelect.setSearch("Set Response Header")
    this.fireOn.el.click()
    if(key){
       actionDef.setHeaderKey(key)
    }
    if(value){
      actionDef.setHeaderValue(value)
    }
    this.fireOn.el.click()
    return actionDef
  }

  newSetPersonaAction(value?:string):TestPersonaAction {
    let actionDef:TestPersonaAction= new TestPersonaAction(this.actionEls.last())
    actionDef.typeSelect.setSearch(TestPersonaAction.TYPE_NAME)
    this.fireOn.el.click()
    if(value){
      actionDef.setPersona(value)
    }
    this.fireOn.el.click()
    return actionDef
  }

  newSetRequestAttributeAction(key?:string, value?:string):TestSetRequestAttributeAction {
    let actionDef:TestSetRequestAttributeAction= new TestSetRequestAttributeAction(this.firstAction().el)
    actionDef.typeSelect.setSearch("Set Request Attribute")
    this.fireOn.el.click()
    if(key){
      actionDef.setAttributeKey(key)
    }
    if(value){
      actionDef.setAttributeValue(value)
    }
    this.fireOn.el.click()
    return actionDef
  }

  remove():webdriver.promise.Promise<any> {
    let result
    if ((browser['browserName'].indexOf('safari') !== -1) || (browser['browserName'].indexOf('internet explorer') !== -1)) {
      result = this.removeBtn.click()
    } else {
      result = this.removeBtn.optShiftClick()
    }
    result.then(( ) => {
      return browser.sleep(250)
    })
    return result
  }

}


export class TestConditionGroupComponent {
  el:ElementFinder
  conditionEls:ElementArrayFinder
  addConditionBtn:TestButton
  logicalTestBtn:TestButton


  constructor(el:protractor.ElementFinder) {
    this.el = el;
    this.conditionEls = el.all(by.tagName('rule-condition'))
    this.addConditionBtn = new TestButton(el.element(by.css('.cw-condition-row .cw-button-add-item')))
    this.logicalTestBtn = new TestButton(el.element(by.css('.cw-group-operator')))
  }

  first():TestConditionComponent{
    return new TestRequestHeaderCondition(this.conditionEls.first())
  }

  last():TestConditionComponent{
    return new TestRequestHeaderCondition(this.conditionEls.last())
  }


  addCondition():protractor.promise.Promise<void>{
    return this.addConditionBtn.click()
  }

  toggleLogicalOperator():protractor.promise.Promise<void>{
    return this.logicalTestBtn.click()
  }

  getLogicalOperator():protractor.promise.Promise<string> {
    return this.logicalTestBtn.el.element(by.tagName('div')).getText()
  }


}

export class TestRuleInputRow {
  el:ElementFinder
  typeSelect:TestInputDropdown
  parameterEls:ElementArrayFinder


  constructor(el:protractor.ElementFinder) {
    this.el = el;
    this.typeSelect = new TestInputDropdown(el.element(by.css('.cw-type-dropdown')))
    this.parameterEls = this.el.all(by.css('.cw-input'))
  }

  parameters():Promise<TestInputComponent[]> {
    return new Promise<TestInputComponent[]>((accept, reject) => {
      let ary:TestInputComponent[] = []
      let proms:Array<Promise<TestInputComponent>> = []

      this.parameterEls.each((el:ElementFinder, idx:number)=> {
        let promise:Promise<TestInputComponent> = new Promise((a, r) => {
          el.getTagName().then((name)=> {
            let comp = TestRuleInputRow.inputFromElName(el, name)
            ary.push(comp)
            a(comp)
          })
        })
        proms.push(promise)
      })

      Promise.all(proms).then((x)=> {
        accept(ary)
      })

    })
  }

  setType(typeName:string):void {
    this.typeSelect.setSearch(typeName + '\t')
   }

  static inputFromElName(el:ElementFinder, name:string) {
    let component:TestInputComponent
    if (name == 'cw-input-text') {
      component = new TestInputText(el)
    } else if (name == 'cw-input-dropdown') {
      component = new TestInputDropdown(el)
    }
    return component
  }

}

export class TestParameter {
  el:ElementFinder


  constructor(el:protractor.ElementFinder) {
    this.el = el;
  }
}

export class TestConditionComponent extends TestRuleInputRow {

  static COMPARE_IS:string= "Is"
  static COMPARE_IS_NOT:string= "Is not"
  compareDD:TestInputDropdown
  logicalTestBtn:TestButton

  constructor(el:protractor.ElementFinder) {
    super(el)
    this.compareDD = new TestInputDropdown(el.element(by.css('.cw-comparator-selector')))
    this.logicalTestBtn = new TestButton(el.element(by.css('.cw-button-toggle-operator')))
  }

  /**
   *
   * @param to
   * @param next The target to navigate to after setting the seach, in order to trigger a change event.
   * @returns {webdriver.promise.Promise<void>}
   */
  setComparison(to:string):webdriver.promise.Promise<void>{
    return this.compareDD.setSearch(to)
  }

  toggleLogicalTest():protractor.promise.Promise<void>{
    return this.logicalTestBtn.click()
  }
}

export class TestRequestHeaderCondition extends TestConditionComponent {

  static HEADER_KEYS = {
    Accept: 'Accept',
    Connection: 'Connection',
    ContentLength: 'content-length',

  }

  headerKeyDD:TestInputDropdown
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyDD = new TestInputDropdown(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }

  setHeaderValue(val:string):webdriver.promise.Promise<void>{
    let p = this.headerValueTF.setValue(val)
    this.headerKeyDD.el.click()
    return p
  }

  setHeaderKey(key:string):webdriver.promise.Promise<void>{
    let p = this.headerKeyDD.setSearch(key)
    this.headerValueTF.el.click()
    return p
  }
}


export class TestActionComponent extends TestRuleInputRow {

  constructor(el:protractor.ElementFinder) {
    super(el)
  }
}


export class TestResponseHeaderAction extends TestActionComponent {

  static TYPE_NAME:string = "Set Response Header"
  headerKeyTF:TestInputText
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyTF = new TestInputText(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }

  getKey():webdriver.promise.Promise<string>{
    return this.headerKeyTF.getValue()
  }

  getValue():webdriver.promise.Promise<string>{
    return this.headerValueTF.getValue()
  }
  setHeaderKey(val:string):webdriver.promise.Promise<void>{
    return this.headerKeyTF.setValue(val, this.headerValueTF.el)
  }

  setHeaderValue(val:string):webdriver.promise.Promise<void>{
    return this.headerValueTF.setValue(val, this.headerKeyTF.el)
  }
}

export class TestSetRequestAttributeAction extends TestActionComponent {

  static TYPE_NAME:string = "Set Request Attribute"
  attributeKeyTF:TestInputText
  attributeValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.attributeKeyTF = new TestInputText(this.parameterEls.first())
    this.attributeValueTF = new TestInputText(this.parameterEls.last())
  }

  getKey():webdriver.promise.Promise<string>{
    return this.attributeKeyTF.getValue()
  }

  getValue():webdriver.promise.Promise<string>{
    return this.attributeValueTF.getValue()
  }
  setAttributeKey(val:string):webdriver.promise.Promise<void>{
    return this.attributeKeyTF.setValue(val)
  }

  setAttributeValue(val:string):webdriver.promise.Promise<void>{
    return this.attributeValueTF.setValue(val, this.attributeKeyTF.el)
  }

}

export class TestPersonaAction extends TestActionComponent {

  static TYPE_NAME:string = "Set Persona"
  static STARTER_VALUES = {
    "FirstTimeInvestor": {label: "First Time Investor", value: "34c720cd-4b46-4a67-9e4b-2117071d01f1"},
    "Retiree": {label: "Retiree", value: "914c93c2-800a-4638-8832-349c221cc87a"},
    "WealthyProspect": {label: "Wealthy Prospect", value: "d4ffa84f-8746-46f8-ac29-1f8ca2c7eaeb"},
    "GlobalInvestor": {label: "Global Investor", value: "1c56ba62-1f41-4b81-bd62-b6eacff3ad23"},
  }
  personaDD:TestInputDropdown

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.personaDD = new TestInputDropdown(el.element(by.css('.cw-condition-component-body cw-input-dropdown')))

  }

  getPersonaName():webdriver.promise.Promise<string>{
    return this.personaDD.getValueText()
  }

  getPersonaValue():webdriver.promise.Promise<string>{
    return this.personaDD.getValueText()
  }

  setPersona(value:string):webdriver.promise.Promise<void> {
    return this.personaDD.setSearch(value)
  }
}
