class RulePageInputs {
  filterBox:protractor.ElementFinder=element(by.css('[class~="cw-header"] INPUT'))
  addRuleButton:protractor.ElementFinder=element(by.css('[class~="cw-header"] BUTTON'))
}
class RulePage {
  url:string='http://localhost:9000/build/index.html'
  title:string= '(Dev) dotCMS Core-Web'
  inputs:RulePageInputs=new RulePageInputs

}

var rulePage = new RulePage()


describe('The Rules Engine (currently main page)', function() {
  it('should have a title.', function() {
    browser.get('http://localhost:9000/build/index.html');
    expect(browser.getTitle()).toEqual('(Dev) dotCMS Core-Web');
  });

  it('should have a filter box.', function() {
    browser.get('http://localhost:9000/build/index.html');
    rulePage.inputs.filterBox.sendKeys("Hello");
    expect(rulePage.inputs.filterBox.getAttribute('value')).toEqual('Hello');
  });

  it('should have an Add Rule button..', function() {
    browser.get('http://localhost:9000/build/index.html');
    expect(rulePage.inputs.addRuleButton).toBeDefined("Add rule button should exist.");
  });
});