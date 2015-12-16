import {Page} from "../../../api/test/CwProtractor";
import {TestTextInput} from "../../../api/test/CwProtractor";
import {TestButton, DDInput} from "../../../api/test/CwProtractor";
import ElementArrayFinder = protractor.ElementArrayFinder;
import ElementFinder = protractor.ElementFinder;

class RulePage extends Page {
  filterBox:ElementFinder
  addRuleButton:TestButton
  rules:ElementArrayFinder

  constructor() {
    super('http://localhost:9000/build/index.html', '(Dev) dotCMS Core-Web');
    this.filterBox = element(by.css('.filter.icon + INPUT'))
    this.addRuleButton = new TestButton(element(by.css('.cw-button-add')))
    this.rules = element.all(by.tagName('rule'))
  }

  ruleCount() {
    return this.rules.count()
  }
}

class RuleTestElement {
  el:ElementFinder
  name:TestTextInput
  fireOn: DDInput


  constructor(root:ElementFinder) {
    this.el = root;
    this.name = new TestTextInput(root.element(by.css('.cw-rule-name-input')))
    this.fireOn = new DDInput(root.element(by.css('.cw-fire-on-dropdown')))
  }


}

var rulePage = new RulePage()

describe('The Rules Engine', function () {
  var page:RulePage

  beforeEach(()=> {
    page = new RulePage()
    page.navigateTo()
  })

  it('should have a title.', function () {
    expect(browser.getTitle()).toEqual(page.title);
  });

  it('should have a filter box.', function () {
    rulePage.filterBox.sendKeys("Hello");
    expect(rulePage.filterBox.getAttribute('value')).toEqual('Hello');
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
    let rule = new RuleTestElement(rulePage.rules.first())
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
    let rule = new RuleTestElement(rulePage.rules.first())
    rule.fireOn.setSearch('every r')
    rule.name.el.click()
    browser.sleep(250) // async save
    rulePage.navigateTo() // reload the page
    expect(rule.fireOn.getValueText()).toEqual('Every Request', "FireOn value should have been saved and restored.")
  });


});