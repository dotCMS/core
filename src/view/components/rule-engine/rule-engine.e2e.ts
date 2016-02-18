import ElementArrayFinder = protractor.ElementArrayFinder;
import ElementFinder = protractor.ElementFinder;
import {Page} from "../../../e2e/CwProtractor";
import {
    RulePage, TestRuleComponent, TestRequestHeaderCondition,
    TestConditionComponent, TestResponseHeaderAction, TestSetRequestAttributeAction, TestPersonaAction
} from "../../../e2e/view/rule-engine/rule-engine-page";

class RobotsTxtPage extends Page {
  private TestUtil

  constructor(TestUtil) {
    super(browser['testLoc']['core'] + '/robots.txt', '')
    this.TestUtil = TestUtil
  }

  getResponseHeader(key:string):protractor.promise.Promise<string> {
    var defer = protractor.promise.defer()
    this.TestUtil.httpGet(this.getFullUrl()).then((result:any) => {
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
      rulePage.suppressAlerts(browser['browserName'] != 'chrome')
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
      rulePage = new RulePage("es")
      rulePage.navigateTo().then(()=>{
        expect(rulePage.filterBox.getAttribute('placeholder')).toEqual('Empieza a escribir para filtrar reglas...');
      })
    });

    it('should have an Add Rule button..', function () {
      expect(rulePage.addRuleButton.el).toBeDefined("Add rule button should exist.");
    });

    it('should add a rule when add rule button is clicked.', function () {
      let count1 = rulePage.ruleCount()
      rulePage.addRuleButton.click()
      rulePage.ruleCount().then(count2 => {
        expect(count1).toEqual(count2 - 1, "Should have added 1 rule.")
        // @todo ggranum: verify that various actions are disabled
      })

    });

    it('should save a rule when a name is typed', function () {
      rulePage.addRule().then((rule:TestRuleComponent) => {
        var name = rule.name;
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=> {
          rule = rulePage.findRule(name)
          expect(rule.getName()).toEqual(name, "Rule should still exist.")
          rule.remove()
        })
      })
    });


    it('should remove a rule with no alert when it has no conditionlets or actionlets.', function (done) {
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
        var name = rule.name
        rule.fireOn.setSearch('every r')
        rulePage.filterBox.click()
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=>{
          rule = rulePage.findRule(name)
          expect(rule.fireOn.getValueText()).toEqual('Every Request', "FireOn value should have been saved and restored.")
          rule.remove()
        })
      })
    });

    it('should save the enabled value when changed.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.toggleEnable.value().then(value=> {
          rule.toggleEnable.toggle().then(()=>{
            browser.sleep(500)
            rulePage.navigateTo().then(()=>{
              rule = rulePage.findRule(name)
              browser.sleep(500)

              expect(rule.toggleEnable.value()).toEqual(!value, "Enabled state should have been toggled.")
              rule.remove()
            })
          })
        })
      })
    });

    it('should expand when the expando icon is clicked.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.expand()
        expect(rule.isShowingBody()).toEqual(true)
        rule.remove()
      })
    })

    it('should expand when the name field is focused.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        rule.expand()
        expect(rule.isShowingBody()).toEqual(true)
        rule.remove()
      })
    })


    it('should save a valid condition.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let conditionDef = rule.createRequestHeaderCondition()
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=>{
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            expect(rule.firstGroup().first().typeSelect.getValueText()).toEqual("Request Header Value", "Should have persisted.")
            rule.remove()
          })
        })

      })
    })

    it('should allow a comparison change on a valid Request Header Condition.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let conditionDef = rule.createRequestHeaderCondition()
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=>{
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            conditionDef = <TestRequestHeaderCondition>rule.firstGroup().first()
            conditionDef.setComparison(TestConditionComponent.COMPARE_IS_NOT, rule.nameEl.el)
            rulePage.waitForSave()
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              var cond3 = <TestRequestHeaderCondition>rule.firstGroup().first()
              expect(cond3.compareDD.getValueText()).toEqual("Is not", "Should have persisted.")
              rule.remove()
            })
          })

        })

      })
    })

    it('should save a valid Response Header action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue("key-AbcDef")
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.el.click()

            browser.sleep(500)
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              expect(rule.firstAction().typeSelect.getValueText()).toEqual("Set Response Header", "Should have persisted.")
              rule.remove()
            })

          })
        })
      })
    })

    it('should fire when enabled with no condition and one Set Response Header action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.setSearch("Every Req")
            rule.toggleEnable.setValue(true)
            rule.fireOn.el.click()
            rulePage.waitForSave().then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'bummer')
              expect(respName).toEqual("value-AbcDef")
              rule.remove()
            })
          })
        })
      })
    })


    it('should fire when enabled with a Request Header "Connection Is Not" condition and one Set Response Header action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let condDef = rule.createRequestHeaderCondition(TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.setSearch("Every Req")
            rule.toggleEnable.setValue(true)
            rule.fireOn.el.click()
            rulePage.waitForSave().then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'bummer')
              expect(respName).toEqual("value-AbcDef")
              rule.remove()
            })
          })
        })
      })
    })

    it('should create a second condition.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let condDef = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )

        let condDef2 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is not",
            "fake value"
        )
        rule.fireOn.el.click().then(()=> {
          rulePage.waitForSave()
          rulePage.navigateTo()
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            expect(rule.firstGroup().conditionEls.count()).toBe(2)
            rule.remove()
          })
        })
      })
    })

    it('should save group logical condition changes on create.', function (done) {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is not",
            "fake value A"
        )
        rule.addGroup(false).then(()=> {
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value B"
          )
          rule.addGroup(false).then(()=> {
            let condDef2 = rule.createRequestHeaderCondition(
                TestRequestHeaderCondition.HEADER_KEYS.Accept,
                "is",
                "fake value C"
            )
            rulePage.waitForSave()
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              expect(rule.getGroup(1).getLogicalOperator()).toBe("OR")
              expect(rule.getGroup(2).getLogicalOperator()).toBe("OR")
              rule.remove()
              done()
            })
          })
        })
      })
    })


    it('should save group logical condition changes on edit', function (done) {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is not",
            "fake value A"
        )
        rule.addGroup().then(()=> {
          let condDef2 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value B"
          )
          rule.addGroup().then(()=> {
            let condDef3 = rule.createRequestHeaderCondition(
                TestRequestHeaderCondition.HEADER_KEYS.Accept,
                "is",
                "fake value C"
            )
            rulePage.waitForSave()
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              let condDef1 = rule.getGroup(1)
              let condDef2 = rule.getGroup(2)
              expect(condDef1.getLogicalOperator()).toBe("AND")
              expect(condDef2.getLogicalOperator()).toBe("AND")
              condDef1.toggleLogicalOperator()
              condDef2.toggleLogicalOperator()
              rulePage.waitForSave()
              rulePage.navigateTo()
              rule = rulePage.findRule(name)
              rule.expand().then(()=> {
                let condDef1 = rule.getGroup(1)
                let condDef2 = rule.getGroup(2)
                expect(condDef1.getLogicalOperator()).toBe("OR")
                expect(condDef2.getLogicalOperator()).toBe("OR")
                rule.remove()
                done()
              })
            })
          })
        })
      })
    })

    it('should fire action for two true conditions ANDed together.', function (done) {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        let condDef2 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is not",
            "fake value"
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toEqual("value-AbcDef")
              rule.remove()
              done()
            })
          })
        })
      })
    })


    it('should fire action for one true and one false condition ORed together.', function (done) {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        let condDef1 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is",
            "fake value",
            false
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toEqual("value-AbcDef")
              rule.remove()
              done()
            })
          })
        })
      })
    })

    it('should not fire action for one true and one false condition ANDed together.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        let condDef1 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is",
            "fake value"
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toBeUndefined()
              rule.remove()
            })
          })
        })
      })
    })

    it('should not fire action for two false condition ORed together.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is",
            "fake value"
        )
        let condDef1 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is",
            "fake value",
            false
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue(name)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(name)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toBeUndefined()
              rule.remove()
            })
          })
        })
      })
    })


    it('should fire action for two true condition groups ANDed together.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        rule.addGroup().then(()=>{
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value"
          )
          let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
          actionDef.typeSelect.setSearch("Set Response").then(()=> {
            rule.fireOn.el.click().then(() => {
              actionDef.headerKeyTF.setValue(name)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(name)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove()
              })
            })
          })
        })
      })
    })

    it('should fire action for group logic (true || false).', function () {
      rulePage.addRule("group - false || true").then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        rule.addGroup(false).then(()=>{
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is",
              "fake value"
          )
          let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
          actionDef.typeSelect.setSearch("Set Response").then(()=> {
            rule.fireOn.el.click().then(() => {
              actionDef.headerKeyTF.setValue(name)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(name)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove()
              })
            })
          })
        })
      })
    })

    xit('should fire action for group logic (false || true).', function () {
      rulePage.addRule("group - false || true").then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is",
            "fake value"
        )
        rule.addGroup(false).then(()=>{
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value"
          )
          let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
          actionDef.typeSelect.setSearch("Set Response").then(()=> {
            rule.fireOn.el.click().then(() => {
              actionDef.headerKeyTF.setValue(name)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(name)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove()
              })
            })
          })
        })
      })
    })

    it('should not fire action for one false and one true condition groups ANDed together.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is",
            "fake value A"
        )
        rule.addGroup().then(()=>{
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value B"
          )
          let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
          actionDef.typeSelect.setSearch("Set Response").then(()=> {
            rule.fireOn.el.click().then(() => {
              actionDef.headerKeyTF.setValue(name)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(name)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toBeUndefined()
                rule.remove()
              })
            })
          })
        })
      })
    })

    it('should fire action for when group logic is: (true || true && false).', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.fireOn.setSearch("Every Req")
        let condDef0 = rule.createRequestHeaderCondition(
            TestRequestHeaderCondition.HEADER_KEYS.Accept,
            "is not",
            "fake value A"
        )
        rule.addGroup(false).then(()=>{
          let condDef1 = rule.createRequestHeaderCondition(
              TestRequestHeaderCondition.HEADER_KEYS.Accept,
              "is not",
              "fake value B"
          )
          rule.addGroup(false).then(()=>{
            let condDef2 = rule.createRequestHeaderCondition(
                TestRequestHeaderCondition.HEADER_KEYS.Accept,
                "is",
                "fake value C"
            )
          let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
          actionDef.typeSelect.setSearch("Set Response").then(()=> {
            rule.fireOn.el.click().then(() => {
              actionDef.headerKeyTF.setValue(name)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(name)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toBe("value-AbcDef")
                //rule.remove()
              })
            })
            })
          })
        })
      })
    })




  })


  describe('Rule Engine Actions', function () {
    var rulePage:RulePage

    beforeEach(()=> {
      rulePage = new RulePage()
      rulePage.suppressAlerts(browser['browserName'] != 'chrome')
      rulePage.navigateTo()
    })


    it('should allow a value change on a valid Set Request Header Action.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = rule.newSetResponseHeaderAction("someHeaderKey", "someHeaderValue")
        rulePage.waitForSave()
        rulePage.navigateTo()
        rule = rulePage.findRule(name)
        rule.expand().then(()=> {
          actionDef = new TestResponseHeaderAction(rule.firstAction().el)
          expect(actionDef.getKey()).toEqual("someHeaderKey", "Key should have persisted.")
          expect(actionDef.getValue()).toEqual("someHeaderValue", "Value should have persisted.")
          actionDef.setHeaderKey("someHeaderKey2")
          actionDef.setHeaderValue("someHeaderValue2")
          rulePage.waitForSave()
          rulePage.navigateTo()
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            let actionDef = new TestResponseHeaderAction(rule.firstAction().el)
            expect(actionDef.getKey()).toEqual("someHeaderKey2", "Key change should have persisted.")
            expect(actionDef.getValue()).toEqual("someHeaderValue2", "Value change should have persisted.")
            rule.remove()
          })
        })
      })
    })


    it('should allow action type to be changed to a Set Attribute Header from Set Response Header.', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let headerActionDef = rule.newSetResponseHeaderAction("someHeaderKey", "someHeaderValue")
        rulePage.waitForSave()
        rulePage.navigateTo()
        rule = rulePage.findRule(name)
        rule.expand().then(()=> {
          let headerActionDef = new TestResponseHeaderAction(rule.firstAction().el)
          headerActionDef.setType(TestSetRequestAttributeAction.TYPE_NAME)
          let attributeActionDef = new TestSetRequestAttributeAction(headerActionDef.el)
          attributeActionDef.setAttributeKey("someAttributeKey")
          attributeActionDef.setAttributeValue("someAttributeValue")
          rulePage.waitForSave()
          rulePage.navigateTo()
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            let attributeActionDef = new TestSetRequestAttributeAction(rule.firstAction().el)
            expect(attributeActionDef.getKey()).toEqual("someAttributeKey", "Key change should have persisted.")
            expect(attributeActionDef.getValue()).toEqual("someAttributeValue", "Value change should have persisted.")
            rule.remove()
          })
        })
      })
    })

      it('should allow multiple actions to be added if all existing actions are valid', function () {
        rulePage.addRule().then((rule:TestRuleComponent)=> {
          let name = rule.name
          rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.Retiree.label)
          rule.addAction()
          let actionDef = rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.GlobalInvestor.label)
          expect(actionDef.el.isPresent()).toBe(true)
          expect(rule.actionEls.count()).toBe(2)
          rule.remove()
        })
      })
  })

  describe('Rule Engine - Persona Action Type', function () {
    var rulePage:RulePage

    beforeEach(()=> {
      rulePage = new RulePage()
      rulePage.suppressAlerts(browser['browserName'] != 'chrome')
      rulePage.navigateTo()
    })


    it('should have four values', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = rule.newSetPersonaAction()
        expect(actionDef.personaDD.items.count()).toEqual(4)
        rule.remove()
      })
    })

    it('should save when a value is selected', function () {
      rulePage.addRule().then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.Retiree.label)
        rulePage.waitForSave()
        rulePage.navigateTo()
        rule = rulePage.findRule(name)
        rule.expand().then(()=> {
          actionDef = new TestPersonaAction(rule.firstAction().el)
          browser.sleep(2500)
          expect(actionDef.el.isPresent()).toBe(true)
          expect(actionDef.getPersonaName()).toBe(TestPersonaAction.STARTER_VALUES.Retiree.label)
          rule.remove()
        })
      })
    })
  })
}