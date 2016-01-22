import ElementArrayFinder = protractor.ElementArrayFinder;
import ElementFinder = protractor.ElementFinder;
import {Page} from "../../../e2e/CwProtractor";
import {
    RulePage, TestRuleComponent, TestRequestHeaderCondition,
    TestConditionComponent
} from "../../../e2e/view/rule-engine/rule-engine-page";

class RobotsTxtPage extends Page {
  private TestUtil

  constructor(TestUtil) {
    super('http://localhost:8080/robots.txt', '')
    this.TestUtil = TestUtil
  }

  getResponseHeader(key:string):protractor.promise.Promise<string> {
    var defer = protractor.promise.defer()
    this.TestUtil.httpGet(this.url).then((result:any) => {
      let resp = result.response
      let headers = resp.headers
      defer.fulfill(headers[key])
    })
    return defer
  }
}


export function initSpec(TestUtil) {

  describe('The Rules Engine', function () {
    var rulePage:RulePage

    beforeEach(()=> {
      rulePage = new RulePage()
      rulePage.navigateTo()
    })

    afterEach(()=>{
      rulePage.removeAllRules()
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
      let rule = rulePage.firstRule()
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


    it('should remove a rule with no alert no conditionlets.', function (done) {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.removeBtn.click()
        let hadAlert = null
        browser.switchTo().alert().accept().then(()=> {
          hadAlert = true
          expect(hadAlert).toEqual(false, "No alert should have been displayed.")
          done()
        }, ()=> {
          hadAlert = false
          expect(hadAlert).toEqual(false, "No alert should have been displayed.")
          done()
        })
      })
    });

    it('should save the fire-on value when changed.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.fireOn.setSearch('every r')
        rule.name.el.click()
        browser.sleep(250) // async save
        rulePage.navigateTo() // reload the page
        expect(rule.fireOn.getValueText()).toEqual('Every Request', "FireOn value should have been saved and restored.")
      })
    });

    it('should save the enabled value when changed.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.toggleEnable.value().then(value=> {
          rule.toggleEnable.toggle()
          browser.sleep(500) // async save
          rulePage.navigateTo() // reload the page
          expect(rule.toggleEnable.value()).toEqual(!value, "Enabled state should have been toggled.")
        })
      })
    });

    it('should expand when the expando icon is clicked.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.expand()
        expect(rule.isShowingBody()).toEqual(true)
      })
    })

    it('should expand when the name field is focused.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.expand()
        expect(rule.isShowingBody()).toEqual(true)
      })
    })


    it('should save a valid condition.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let conditionDef = rule.newRequestHeaderCondition()
        browser.sleep(250) // async save
        rulePage.navigateTo() // reload the page
        rule.expand().then(()=> {
          expect(rule.firstGroup().first().typeSelect.getValueText()).toEqual("Request Header Value", "Should have persisted.")
        })
      })
    })

    it('should allow a comparison change on a valid Request Header Condition.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let conditionDef = rule.newRequestHeaderCondition()
        browser.sleep(250) // async save
        rulePage.navigateTo() // reload the page
        rule.expand().then(()=> {
          conditionDef = <TestRequestHeaderCondition>rule.firstCondition()
          conditionDef.setComparison(TestConditionComponent.COMPARE_IS_NOT, rule.name.el)
          browser.sleep(250)
          rulePage.navigateTo()
          rule.expand().then(()=>{
            var cond3 = <TestRequestHeaderCondition>rule.firstCondition()
            expect(cond3.compareDD.getValueText()).toEqual("Is not", "Should have persisted.")
          })
        })
      })
    })

    it('should save a valid Response Header action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
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
      })
    })

    it('should fire when enabled with no condition and one Set Response Header action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
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
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'bummer')
              expect(respName).toEqual("value-AbcDef")
            })
          })
        })
      })
    })

  })
}