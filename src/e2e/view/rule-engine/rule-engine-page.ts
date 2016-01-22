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
    super('http://localhost:9000/build/index.html' + (locale != null ? '?locale=' + locale : ''), '(Dev) dotCMS Core-Web');
    this.filterBox = element(by.css('.filter.icon + INPUT'))
    this.addRuleButton = new TestButton(element(by.css('.cw-button-add')))
    this.ruleEls = element.all(by.tagName('rule'))
  }


  addRule():webdriver.promise.Promise<TestRuleComponent> {
    return this.addRuleButton.click().then(()=> {
      let rule = this.firstRule()
      var name = "e2e-" + new Date().getTime();
      rule.name.setValue(name)
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
  name:TestInputText
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
    this.name = new TestInputText(root.element(by.css('.cw-rule-name-input')))
    this.fireOn = new TestInputDropdown(root.element(by.css('.cw-fire-on-dropdown')))
    this.toggleEnable = new TestInputToggle(root.element(by.tagName('cw-toggle-input')))
    this.removeBtn = new TestButton(root.element(by.css('.cw-delete-rule')))
    this.addGroup = new TestButton(root.element(by.css('.cw-add-group')))
    this.conditionGroupEls = root.all(by.tagName('condition-group'))
    this.actionEls = root.all(by.tagName('rule-action'))
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

  remove():Promise<any> {
    return new Promise((a, r)=> {
      this.removeBtn.click()
      try {
        browser.switchTo().alert().accept()
      } catch (e) {
        console.log("No alert shown?", e)
        r(e)
      }
      a()
    })

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

  compareDD:TestInputDropdown

  constructor(el:protractor.ElementFinder) {
    super(el)
    this.compareDD = new TestInputDropdown(el.element(by.css('.cw-comparator-selector')))
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
}


export class TestActionComponent extends TestRuleInputRow {

  constructor(el:protractor.ElementFinder) {
    super(el)
  }
}


export class TestResponseHeaderAction extends TestActionComponent {

  headerKeyDD:TestInputDropdown
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyDD = new TestInputDropdown(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }
}
