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
  jasmine.DEFAULT_TIMEOUT_INTERVAL = 60000;

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
      rulePage.setFilter("Hello").then((  ) => {
        return rulePage.getFilter()
      }).then(( filterText:string ) => {
        expect(filterText).toEqual('Hello');
      })
      
    });

    it('should have translations for the filter box placeholder text.', function () {
      rulePage = new RulePage("es")
      rulePage.navigateTo().then(()=>{
        expect(rulePage.getFilterPlaceholder()).toEqual('Empieza a escribir para filtrar reglas...');
      })
    });

    it('should save a rule when a name is typed', function (done) {
      rulePage.addRule('should save a rule when a name is typed').then((rule:TestRuleComponent) => {
        var name = rule.name;
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=> {
          rule = rulePage.findRule(name)
          expect(rule.getName()).toEqual(name, "Rule should still exist.")
          rule.remove().then(done)
        })
      })
    });


    it('should remove a rule with no alert when it has no conditionlets or actionlets.', function (done) {
      rulePage.addRule('should remove a rule with no alert when no ').then((rule:TestRuleComponent)=> {
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

    it('should save the fire-on value when changed.', function (done) {
      var name
      rulePage.addRule('should save the fire-on value when changed.').then((rule:TestRuleComponent)=> {
        name = rule.name
        return rule.setFireOn('Every Request')
      }).then(() => rulePage.navigateTo())
          .then((rulePage:RulePage) => rulePage.findRule(name))
          .then((rule:TestRuleComponent) => {
            return rule.getFireOn().then((fireOn:string) => {
              var expected = 'Every Request'
              expect(fireOn).toEqual(expected, "FireOn value should have been saved and restored.")
              if (fireOn == expected) {
                rule.remove().then(done)
              } else {
                done()
              }
            })
          })
    });

    it('should save the enabled value when changed.', function (done) {
      var name, rule
      rulePage.addRule('should save the enabled value when changed.').then((r:TestRuleComponent)=> {
            rule = r
            name = rule.name
            return rule.toggleEnable.toggle()
          })
          .then(()=> rulePage.saveAndReload())
          .then((rulePage:RulePage) => rulePage.findRule(name))
          .then((rule:TestRuleComponent) => rule.toggleEnable.value())
          .then((newValue:boolean)=> {
            expect(newValue).toEqual(true, "New rule should have been toggled to 'on' state.")
            if (newValue) {
              rule.remove().then(done)
            } else {
              done()
            }
          })
    });

    it('should expand when the expando icon is clicked.', function (done) {
      var rule
      rulePage.addRule('should expand when the expando icon is clicked.')
          .then((r:TestRuleComponent)=> { rule = r; return rule.expand()})
          .then(() =>rule.isShowingBody())
          .then((showing:boolean)=> {
            expect(showing).toEqual(true)
            if(showing) {
              rule.remove().then(done)
            }
          })
    })

    it('should expand when the name field is focused.', function (done) {
      rulePage.addRule('should expand when the name field is focused.').then((rule:TestRuleComponent)=> {
        rule.expand()
        expect(rule.isShowingBody()).toEqual(true)
        rule.remove().then(done)
      })
    })


   it('should save a valid condition.', function (done) {
      rulePage.addRule('should save a valid condition.').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let conditionDef = rule.createRequestHeaderCondition()
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=>{
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            rule.firstGroup().first().typeSelect.getValueText().then(( text ) => {
              let expected = "Request Header"
              expect(text).toEqual(expected, "Should have persisted.")
              if(text == expected) {
                rule.remove().then(done)
              }
            })

          })
        })
      })
    })

    it('should allow a comparison change on a valid Request Header Condition.', function (done) {
      rulePage.addRule('should allow a comparison change on a valid Request Header Condition.').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let conditionDef = rule.createRequestHeaderCondition()
        rulePage.waitForSave()
        rulePage.navigateTo().then(()=>{
          rule = rulePage.findRule(name)
          rule.expand().then(()=> {
            conditionDef = <TestRequestHeaderCondition>rule.firstGroup().first()
            conditionDef.setComparison(TestConditionComponent.COMPARE_IS_NOT)
            rulePage.waitForSave()
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              var cond3 = <TestRequestHeaderCondition>rule.firstGroup().first()
              expect(cond3.compareDD.getValueText()).toEqual("Is not", "Should have persisted.")
              rule.remove().then(done)
            })
          })

        })

      })
    })

    it('should save a valid Response Header action.', function (done) {
      rulePage.addRule('should save a valid Response Header action.').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            actionDef.headerKeyTF.setValue("key-AbcDef")
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.el.click()
            rulePage.waitForSave()
            rulePage.navigateTo()
            rule = rulePage.findRule(name)
            rule.expand().then(()=> {
              rule.firstAction().typeSelect.getValueText().then(( text ) => {
                let expected =  "Set Response Header"
                expect(text).toEqual(expected, "Should have persisted.")
                if(text == expected){
                  rule.remove().then(done)
                }
              })
            })
          })
        })
      })
    })

    it('should fire when enabled with no condition and one Set Response Header action.', function (done) {
      var key;
      rulePage.addRule('should fire when enabled with no condition and one Set Response Header action.').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.setSearch("Every Req")
            rule.toggleEnable.setValue(true)
            rule.fireOn.el.click()
            return rulePage.waitForSave()
          }).then(()=> {
            let robots = new RobotsTxtPage(TestUtil)
            return robots.getResponseHeader(key)
          }).then(( respName ) => {
            var expected = "value-AbcDef"
            expect(respName).toEqual(expected)
            if (respName == expected) {
              rule.remove().then(done)
            } else {
              done()
            }
          })
        })
      })
    })


    it('should fire when enabled with a Request Header "Connection Is Not" condition and one Set Response Header action.', function (done) {
      var key
      rulePage.addRule('should fire when enabled with a Request Header ').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let condDef = rule.createRequestHeaderCondition(TestRequestHeaderCondition.HEADER_KEYS.Connection,
            "is not",
            "fake value"
        )
        let actionDef = new TestResponseHeaderAction(rule.actionEls.first())
        actionDef.typeSelect.setSearch("Set Response").then(()=> {
          rule.fireOn.el.click().then(() => {
            key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.fireOn.setSearch("Every Req")
            rule.toggleEnable.setValue(true)
            rule.fireOn.el.click()
            return rulePage.waitForSave()
          }).then(()=> {
            let robots = new RobotsTxtPage(TestUtil)
            return robots.getResponseHeader(key)
          }).then(( respName ) => {
            var expected = "value-AbcDef"
            expect(respName).toEqual(expected)
            if (respName == expected) {
              rule.remove().then(done)
            } else {
              done()
            }
          })
        })
      })
    })


    it('should create a second condition.', function (done) {
      rulePage.addRule('should create a second condition.').then((rule:TestRuleComponent)=> {
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
            rule.remove().then(done)
          })
        })
      })
    })

    it('should save group logical condition changes on create.', function (done) {
      rulePage.addRule('should save group logical condition changes on create.').then((rule:TestRuleComponent)=> {
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
              rule.remove().then(done)
            })
          })
        })
      })
    })


    it('should save group logical condition changes on edit', function (done) {
      rulePage.addRule('should save group logical condition changes on edit').then((rule:TestRuleComponent)=> {
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
                rule.remove().then(done)
              })
            })
          })
        })
      })
    })

    it('should fire action for two true conditions ANDed together.', function (done) {
      rulePage.addRule('should fire action for two true conditions ANDed together.').then((rule:TestRuleComponent)=> {
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
            let key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(key)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toEqual("value-AbcDef")
              rule.remove().then(done)
            })
          })
        })
      })
    })


    it('should fire action for one true and one false condition ORed together.', function (done) {
      rulePage.addRule('should fire action for one true and one false condition ORed together.').then((rule:TestRuleComponent)=> {
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
            let key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(key)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toEqual("value-AbcDef")
              rule.remove().then(done)
            })
          })
        })
      })
    })

    it('should not fire action for one true and one false condition ANDed together.', function (done) {
      rulePage.addRule('should not fire action for one true and one false condition ANDed together.').then((rule:TestRuleComponent)=> {
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
            let key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(key)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toBeUndefined()
              rule.remove().then(done)
            })
          })
        })
      })
    })

    it('should not fire action for two false condition ORed together.', function (done) {
      rulePage.addRule('should not fire action for two false condition ORed together.').then((rule:TestRuleComponent)=> {
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
            let key = name.substring(0, name.indexOf('-should'))
            actionDef.headerKeyTF.setValue(key)
            actionDef.headerValueTF.setValue("value-AbcDef")
            rule.toggleEnable.setValue(true)
            rulePage.waitForSave(500).then(()=> {
              let robots = new RobotsTxtPage(TestUtil)
              let respName:any = robots.getResponseHeader(key)
              browser.driver.wait(respName, 1000, 'Wait failed')
              expect(respName).toBeUndefined()
              rule.remove().then(done)
            })
          })
        })
      })
    })


    it('should fire action for two true condition groups ANDed together.', function (done) {
      rulePage.addRule('should fire action for two true condition groups ANDed together.').then((rule:TestRuleComponent)=> {
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
              let key = name.substring(0, name.indexOf('-should'))
              actionDef.headerKeyTF.setValue(key)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(key)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove().then(done)
              })
            })
          })
        })
      })
    })

    it('should fire action for group logic (true || false).', function (done) {
      rulePage.addRule('should fire action for group logic (true || false).').then((rule:TestRuleComponent)=> {
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
              let key = name.substring(0, name.indexOf('-should'))
              actionDef.headerKeyTF.setValue(key)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(key)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove().then(done)
              })
            })
          })
        })
      })
    })

    it('should fire action for group logic (false || true).', function (done) {
      rulePage.addRule('should fire action for group logic (false || true).').then((rule:TestRuleComponent)=> {
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
              let key = name.substring(0, name.indexOf('-should'))
              actionDef.headerKeyTF.setValue(key)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(key)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toEqual("value-AbcDef")
                rule.remove().then(done)
              })
            })
          })
        })
      })
    })

    it('should not fire action for one false and one true condition groups ANDed together.', function (done) {
      rulePage.addRule('should not fire action for one false and one true condition groups ANDed together.').then((rule:TestRuleComponent)=> {
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
              let key = name.substring(0, name.indexOf('-should'))
              actionDef.headerKeyTF.setValue(key)
              actionDef.headerValueTF.setValue("value-AbcDef")
              rule.toggleEnable.setValue(true)
              rulePage.waitForSave(500).then(()=> {
                let robots = new RobotsTxtPage(TestUtil)
                let respName:any = robots.getResponseHeader(key)
                browser.driver.wait(respName, 1000, 'Wait failed')
                expect(respName).toBeUndefined()
                rule.remove().then(done)
              })
            })
          })
        })
      })
    })

    it('should fire action when group logic is: (true || true && false).', function (done) {
      rulePage.addRule('should fire action when group logic is: (true || true && false).').then((rule:TestRuleComponent)=> {
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
                let key = name.substring(0, name.indexOf('-should'))
                actionDef.headerKeyTF.setValue(key)
                actionDef.headerValueTF.setValue("value-AbcDef")
                rule.toggleEnable.setValue(true)
                rulePage.waitForSave(500).then(()=> {
                  let robots = new RobotsTxtPage(TestUtil)
                  let respName:any = robots.getResponseHeader(key)
                  browser.driver.wait(respName, 1000, 'Wait failed')
                  expect(respName).toBe("value-AbcDef")
                  rule.remove().then(done)
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


    it('should allow a value change on a valid Set Request Header Action.', function (done) {
      rulePage.addRule('should allow a value change on a valid Set Request Header Action.').then((rule:TestRuleComponent)=> {
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
            var expectedKey = "someHeaderKey2"
            var expectedValue = "someHeaderValue2"
            actionDef.getKey().then(( key ) => {
              actionDef.getValue().then((value) => {
                expect(key).toEqual(expectedKey, "Key change should have persisted.")
                expect(value).toEqual(expectedValue, "Value change should have persisted.")
                if(key == expectedKey && value == expectedValue) {
                  rule.remove().then(done)
                }
              })
            })
          })
        })
      })
    })


    it('should allow action type to be changed to a Set Attribute Header from Set Response Header.', function (done) {
      rulePage.addRule('should allow action type to be changed to a Set Attribute Header from Set Response Header.').then((rule:TestRuleComponent)=> {
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
            attributeActionDef.getKey().then((key) => {
              attributeActionDef.getValue().then((value) => {
                let expectedKey = "someAttributeKey"
                let expectedValue = "someAttributeValue"
                expect(key).toEqual(expectedKey, "Key change should have persisted.")
                expect(value).toEqual(expectedValue, "Value change should have persisted.")
                if (key == expectedKey && value == expectedValue) {
                  rule.remove().then(done)
                }
              })
            })
          })
        })
      })
    })

    it('should allow multiple actions to be added if all existing actions are valid', function (done) {
      rulePage.addRule('should allow multiple actions to be added if all existing actions are valid').then((rule:TestRuleComponent)=> {
        let name = rule.name
        rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.Retiree.label)
        rule.addAction()
        let actionDef = rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.GlobalInvestor.label)
        expect(actionDef.el.isPresent()).toBe(true)
        expect(rule.actionEls.count()).toBe(2)
        rule.remove().then(done)
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


    it('should have four values', function (done) {
      rulePage.addRule('should have four values').then((rule:TestRuleComponent)=> {
        let name = rule.name
        let actionDef = rule.newSetPersonaAction()
        expect(actionDef.personaDD.items.count()).toEqual(4)
        rule.remove().then(done)
      })
    })

    it('should save when a value is selected', function (done) {
      rulePage.addRule("should save when a value is selected").then((rule:TestRuleComponent) => {
        let name = rule.name
        let actionDef = rule.newSetPersonaAction(TestPersonaAction.STARTER_VALUES.Retiree.label)
        rulePage.waitForSave()
        rulePage.navigateTo()
        rule = rulePage.findRule(name)
        rule.expand().then(()=> {
          actionDef = new TestPersonaAction(rule.firstAction().el)
          actionDef.el.isPresent().then(( present ) => {
            browser.sleep(250)
            actionDef.getPersonaName().then((personaName ) => {
              var expected = TestPersonaAction.STARTER_VALUES.Retiree.label
              expect(present).toBe(true)
              expect(personaName).toBe(expected)
              if(present && personaName == expected ) {
                rule.remove().then((  ) => {
                  done()
                })
              }
            })
          })
        })
      })
    })
  })
}