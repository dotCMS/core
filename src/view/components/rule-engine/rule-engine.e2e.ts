

describe('The Rules Engine (currently main page)', function() {
  it('should have a title.', function() {
    browser.get('http://localhost:9000/build/index.html');
    expect(browser.getTitle()).toEqual('(Dev) dotCMS Core-Web');
  });

  it('should have a filter box.', function() {
    browser.get('http://localhost:9000/build/index.html');
    element(by.css('[class~="cw-header"] INPUT')).sendKeys("Hello");
    expect(browser.getTitle()).toEqual('(Dev) dotCMS Core-Web');
  });
});