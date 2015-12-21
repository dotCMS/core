import {Page} from "../../../api/test/CwProtractor";
import {TestInputText} from "../../../api/test/CwProtractor";
import {TestButton, TestInputDropdown} from "../../../api/test/CwProtractor";
import ElementArrayFinder = protractor.ElementArrayFinder;
import ElementFinder = protractor.ElementFinder;
import {TestInputToggle} from "../../../api/test/CwProtractor";
import {TestInputComponent} from "../../../api/test/CwProtractor";
import {TestUtil} from "../../../api/test/CwProtractor";

class RulePage extends Page {
  filterBox:ElementFinder
  addRuleButton:TestButton
  rules:ElementArrayFinder

  constructor(locale:string=null) {
    super('http://localhost:9000/build/index.html' + (locale != null ? '?locale=' + locale : ''), '(Dev) dotCMS Core-Web');
    this.filterBox = element(by.css('.filter.icon + INPUT'))
    this.addRuleButton = new TestButton(element(by.css('.cw-button-add')))
    this.rules = element.all(by.tagName('rule'))
  }

  ruleCount() {
    return this.rules.count()
  }
}


class RobotsTxtPage extends Page {
  constructor() {
    super('http://localhost:8080/robots.txt', '')
  }

  getResponseHeader(key:string):protractor.promise.Promise<string> {
    var defer = protractor.promise.defer()
    TestUtil.httpGet(this.url).then((result) => {
      let resp = result.response
      let headers = resp.headers
      console.log('Header ', key, ' is ', headers[key])
      defer.fulfill(headers[key])
    })
    return defer
  }
}

class TestRuleComponent {
  el:ElementFinder
  header:ElementFinder
  mainBody:ElementFinder
  name:TestInputText
  fireOn:TestInputDropdown
  toggleEnable:TestInputToggle
  remove:TestButton
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
    this.remove = new TestButton(root.element(by.css('.cw-delete-rule')))
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
}


class TestConditionGroupComponent {
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

class TestRuleInputRow {
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
            console.log("TestRuleInputRow", "effff", comp)
            ary.push(comp)
            a(comp)
          })
        })
        proms.push(promise)
      })

      Promise.all(proms).then((x)=> {
        console.log('yay', ary)
        accept(ary)
      })

    })
  }

  static inputFromElName(el:ElementFinder, name:string) {
    console.log("TestRuleInputRow", "inputFromElName", name)
    let component:TestInputComponent
    if (name == 'cw-input-text') {
      component = new TestInputText(el)
    } else if (name == 'cw-input-dropdown') {
      component = new TestInputDropdown(el)
    }
    return component
  }

}

class TestParameter {
  el:ElementFinder


  constructor(el:protractor.ElementFinder) {
    this.el = el;
  }
}

class TestConditionComponent extends TestRuleInputRow {

  compareDD:TestInputDropdown

  constructor(el:protractor.ElementFinder) {
    super(el)
    this.compareDD = new TestInputDropdown(el.element(by.css('.cw-comparator-selector')))
  }
}

class TestRequestHeaderCondition extends TestConditionComponent {

  headerKeyTF:TestInputText
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyTF = new TestInputText(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }
}


class TestActionComponent extends TestRuleInputRow {

  constructor(el:protractor.ElementFinder) {
    super(el)
  }
}


class TestResponseHeaderAction extends TestActionComponent {

  headerKeyDD:TestInputDropdown
  headerValueTF:TestInputText

  constructor(el:protractor.ElementFinder) {
    super(el);
    this.headerKeyDD = new TestInputDropdown(this.parameterEls.first())
    this.headerValueTF = new TestInputText(this.parameterEls.last())
  }
}


describe('The Rules Engine', function () {
  var rulePage:RulePage

  beforeEach(()=> {
    rulePage = new RulePage()
    rulePage.navigateTo()
  })

  it('should have a title.', function () {
    expect(browser.getTitle()).toEqual(rulePage.title);
  });
  
  it('should have a filter box.', function () {
    rulePage.filterBox.sendKeys("Hello");
    expect(rulePage.filterBox.getAttribute('value')).toEqual('Hello');
  });

  it('should have translations for the filter box placeholder text.', function () {
    rulePage = new RulePage("es").navigateTo()
    expect(rulePage.filterBox.getAttribute('placeholder')).toEqual('Empieza a escribir para filtrar reglas...');
  });
  
  it('should have an Add Rule button..', function () {
    expect(rulePage.addRuleButton.el).toBeDefined("Add rule button should exist.");
  });
  
  it('should have an Add Rule button..', function () {
    expect(rulePage.addRuleButton.el).toBeDefined("Add rule button should exist.");
  });
  
  it('should add a rule when add rule button is clicked.', function () {
    let count1 = rulePage.ruleCount()
    rulePage.addRuleButton.click()
    rulePage.ruleCount().then(count2 => {
      expect(count1).toEqual(count2 - 1, "Should have added 1 rule.")
    })
    rulePage.navigateTo()
    rulePage.ruleCount().then(count3 => {
      expect(count1).toEqual(count3, "The rule should not be persisted unless a name was typed.")
    })
  });
  
  it('should save a rule when a name is typed', function () {
    let count1 = rulePage.ruleCount()
    rulePage.addRuleButton.click()
    let rule = new TestRuleComponent(rulePage.rules.first())
    expect(rule.name.getValue()).toEqual('', "Rule should have been added to the top of the list.")
    var name = "e2e-" + new Date().getTime();
    rule.name.setValue(name)
    rule.fireOn.el.click()
    browser.sleep(250) // async save
    rulePage.navigateTo() // reload the page
    rulePage.ruleCount().then(count2 => {
      expect(count1).toEqual(count2 - 1, "The rule should still exist.")
    })
    expect(rule.name.getValue()).toEqual(name, "Rule should still exist, and should still be first.")
  });
  
  
  it('should save the fire-on value when changed.', function () {
    let rule = new TestRuleComponent(rulePage.rules.first())
    rule.fireOn.setSearch('every r')
    rule.name.el.click()
    browser.sleep(250) // async save
    rulePage.navigateTo() // reload the page
    expect(rule.fireOn.getValueText()).toEqual('Every Request', "FireOn value should have been saved and restored.")
  });
  
  it('should save the enabled value when changed.', function () {
    let rule = new TestRuleComponent(rulePage.rules.first())
    rule.toggleEnable.value().then(value=> {
      rule.toggleEnable.toggle()
      browser.sleep(500) // async save
      rulePage.navigateTo() // reload the page
      expect(rule.toggleEnable.value()).toEqual(!value, "Enabled state should have been toggled.")
    })
  
  });
  
  it('should expand when the expando icon is clicked.', function () {
    let rule = new TestRuleComponent(rulePage.rules.first())
    rule.expand()
    expect(rule.isShowingBody()).toEqual(true)
  });

  it('should expand when the name field is focused.', function () {
    let rule = new TestRuleComponent(rulePage.rules.first())
    rule.expand()
    expect(rule.isShowingBody()).toEqual(true)
  });


  it('should save a valid condition.', function () {
    rulePage.addRuleButton.click()
    let rule = new TestRuleComponent(rulePage.rules.first())
    var name = "e2e-" + new Date().getTime();
    rule.name.setValue(name)
    rule.fireOn.el.click()
    let conditionDef = new TestRequestHeaderCondition(rule.firstGroup().conditionEls.first())
    conditionDef.typeSelect.setSearch("Request Hea").then(()=> {
      rule.fireOn.el.click().then(() => {
        conditionDef.compareDD.setSearch("Is")
        conditionDef.headerValueTF.setValue("AbcDef")
        rule.fireOn.el.click()

        browser.sleep(250) // async save
        rulePage.navigateTo() // reload the page

        rule.expand().then(()=> {
          expect(rule.firstGroup().first().typeSelect.getValueText()).toEqual("Request Header Value", "Should have persisted.")
        })

      })
    })

    //expect(rule.toggleEnable.value()).toEqual(!value, "Enabled state should have been toggled.")

  });

  it('should save a valid Response Header action.', function () {
    rulePage.addRuleButton.click()
    let rule = new TestRuleComponent(rulePage.rules.first())
    var name = "e2e-" + new Date().getTime();
    rule.name.setValue(name)
    rule.fireOn.el.click()
    let actionDef = new TestRequestHeaderCondition(rule.actionEls.first())
    actionDef.typeSelect.setSearch("Set Response").then(()=> {
      rule.fireOn.el.click().then(() => {
        actionDef.headerKeyTF.setValue("key-AbcDef")
        actionDef.headerValueTF.setValue("value-AbcDef")
        rule.fireOn.el.click()

        browser.sleep(500) // async save
        rulePage.navigateTo() // reload the page

        rule.expand().then(()=> {
          expect(rule.firstAction().typeSelect.getValueText()).toEqual("Set Response Header", "Should have persisted.")
        })

      })
    })

    //expect(rule.toggleEnable.value()).toEqual(!value, "Enabled state should have been toggled.")

  });


  it('should fire when enabled with no condition and one Set Response Header action.', function () {
    rulePage.addRuleButton.click()
    let rule = new TestRuleComponent(rulePage.rules.first())
    var name = "e2e-" + new Date().getTime();
    rule.name.setValue(name)
    rule.fireOn.el.click()
    let actionDef = new TestRequestHeaderCondition(rule.actionEls.first())
    actionDef.typeSelect.setSearch("Set Response").then(()=> {
      rule.fireOn.el.click().then(() => {
        let name = 'e2e-header-key-' + new Date().getTime()
        actionDef.headerKeyTF.setValue(name)
        actionDef.headerValueTF.setValue("value-AbcDef")
        rule.fireOn.setSearch("Every Req")
        rule.toggleEnable.setValue(true)
        rule.fireOn.el.click()
        browser.sleep(250).then(()=> {
          let robots = new RobotsTxtPage()
          let respName:any = robots.getResponseHeader(name)
          browser.driver.wait(respName, 1000, 'bummer')
          expect(respName).toEqual("value-AbcDef")
        })
      })
    })
  });


});