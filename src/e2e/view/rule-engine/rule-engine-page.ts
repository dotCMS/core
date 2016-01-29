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
  addRuleButton:TestButton
  ruleEls:ElementArrayFinder

  constructor(locale:string = null) {
    super('http://localhost:9000/build/index.html', '(Dev) dotCMS Core-Web');
    if(locale){
      this.queryParams['locale'] = locale
    }
    this.filterBox = element(by.css('.filter.icon + INPUT'))
    this.addRuleButton = new TestButton(element(by.css('.cw-button-add')))
    this.ruleEls = element.all(by.tagName('rule'))
  }

  suppressAlerts(value:boolean){
    this.queryParams['suppressAlerts'] = value ? 'true' : 'false'
  }

  waitForSave():webdriver.promise.Promise<void> {
    return browser.sleep(250)
  }

  addRule():webdriver.promise.Promise<TestRuleComponent> {
    return this.addRuleButton.click().then(()=> {
      let rule = this.firstRule()
      var name = "e2e-" + browser['browserName'] + '-' + new Date().getTime();
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
  addGroup:TestButton
  expandoCaret:ElementFinder
  conditionGroupEls:ElementArrayFinder
  actionEls:ElementArrayFinder


  constructor(root:ElementFinder) {
    this.el = root;
    this.mainBody = root.element(by.css('.cw-accordion-body'))
    this.header = root.element(by.css('.cw-header'))
    this.expandoCaret = this.header.element(by.css('.cw-rule-caret'))
    this.nameEl = new TestInputText(root.element(by.css('.cw-rule-name-input')))
    this.fireOn = new TestInputDropdown(root.element(by.css('.cw-fire-on-dropdown')))
    this.toggleEnable = new TestInputToggle(root.element(by.tagName('cw-toggle-input')))
    this.removeBtn = new TestButton(root.element(by.css('.cw-delete-rule')))
    this.addGroup = new TestButton(root.element(by.css('.cw-add-group')))
    this.conditionGroupEls = root.all(by.tagName('condition-group'))
    this.actionEls = root.all(by.tagName('rule-action'))
  }

  setName(name:string):webdriver.promise.Promise<void>{
    this.name = name
    return this.nameEl.setValue(name)
  }

  getName():webdriver.promise.Promise<string>{
    let v = this.nameEl.getValue()
    v.then( name => this.name = name)
    return v
  }

  isShowingBody():webdriver.promise.Promise<boolean> {
    return this.mainBody.getAttribute('class').then(v=> {
      return v == null ? false : v.indexOf('cw-hidden') == -1
    })
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

  firstAction():TestActionComponent {
    return new TestActionComponent(this.actionEls.first())
  }

  firstCondition():TestConditionComponent{
    return new TestRequestHeaderCondition(this.firstGroup().conditionEls.first())
  }

  newRequestHeaderCondition():TestRequestHeaderCondition {
    let conditionDef:TestRequestHeaderCondition = new TestRequestHeaderCondition(this.firstCondition().el)
    conditionDef.typeSelect.setSearch("Request Hea")
    this.fireOn.el.click()
    conditionDef.setComparison(TestConditionComponent.COMPARE_IS, this.fireOn.el)
    conditionDef.setHeaderValue("AbcDef")
    this.fireOn.el.click()
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
    if (browser['browserName'].indexOf('safari') !== -1 ) {
      result = this.removeBtn.click()
    } else {
      result = this.removeBtn.optShiftClick()
    }
    return result
  }
}


export class TestConditionGroupComponent {
  el:ElementFinder
  conditionEls:ElementArrayFinder

  constructor(el:protractor.ElementFinder) {
    this.el = el;
    this.conditionEls = el.all(by.tagName('rule-condition'))
  }

  first() {
    return new TestConditionComponent(this.conditionEls.first())
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

  constructor(el:protractor.ElementFinder) {
    super(el)
    this.compareDD = new TestInputDropdown(el.element(by.css('.cw-comparator-selector')))
  }

  /**
   *
   * @param to
   * @param next The target to navigate to after setting the seach, in order to trigger a change event.
   * @returns {webdriver.promise.Promise<void>}
   */
  setComparison(to:string, next:ElementFinder):webdriver.promise.Promise<void>{
    this.compareDD.setSearch(to)
    return next.click()
  }
}

export class TestRequestHeaderCondition extends TestConditionComponent {

  headerKeyTF:TestInputText
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyTF = new TestInputText(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }

  setHeaderValue(val:string):webdriver.promise.Promise<void>{
    return this.headerValueTF.setValue(val)
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
    return this.headerKeyTF.setValue(val)
  }

  setHeaderValue(val:string):webdriver.promise.Promise<void>{
    return this.headerValueTF.setValue(val)
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
    return this.attributeValueTF.setValue(val)
  }

}
