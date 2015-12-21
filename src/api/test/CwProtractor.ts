import ElementFinder = protractor.ElementFinder;


export class TestUtil {


// A Protracterized httpGet() promise [curtesy of Leo Galluci ,
// http://stackoverflow.com/questions/25137881/how-to-use-protractor-to-get-the-response-status-code-and-response-text
  static httpGet(siteUrl):protractor.promise.Promise<any> {
    //noinspection TypeScriptUnresolvedFunction
    var http = require('http');
    var defer = protractor.promise.defer();

    http.get(siteUrl, function (response) {

      var bodyString = '';

      response.setEncoding('utf8');

      response.on("data", function (chunk) {
        bodyString += chunk;
      });

      response.on('end', function () {
        defer.fulfill({
          response: response,
          statusCode: response.statusCode,
          bodyString: bodyString
        });
      });

    }).on('error', function (e) {
      defer.reject("Got http.get error: " + e.message);
    });

    return defer.promise;
  }

// // Example:
//it('should return 200 and contain proper body', function() {
//  httpGet("http://localhost:80").then(function(result) {
//    expect(result.statusCode).toBe(200);
//    expect(result.bodyString).toContain('Apache');
//  });
//});
}
export class Page {

  url:string
  title:string


  constructor(url:string, title:string) {
    this.url = url;
    this.title = title;
  }

  navigateTo():any{
    browser.get(this.url)
    expect(browser.getTitle()).toEqual(this.title);
    return this
  }

  static logBrowserConsole(){
    browser.manage().logs().get('browser').then(function(browserLog) {
      //noinspection TypeScriptUnresolvedFunction
      console.log('log: ' + require('util').inspect(browserLog));
    });
  }

}


export class TestButton {

  el:ElementFinder

  constructor(el:protractor.ElementFinder) {
    this.el = el;
  }

  click(){
    this.el.click()
  }
}

export class TestInputComponent {
  el:ElementFinder

  constructor(el:protractor.ElementFinder) {
    this.el = el;
  }
}

export class TestInputText extends TestInputComponent{

  valueInput:ElementFinder
  icon:ElementFinder

  constructor(el:ElementFinder) {
    super(el)
    this.valueInput = this.el.element(by.tagName('INPUT'))
    this.icon = this.el.element(by.tagName('I'))
  }

  focus():webdriver.promise.Promise<void>{
    return this.valueInput.click()
  }

  placeholder(){
    return this.valueInput.getAttribute('placeholder')
  }

  getValue(){
    return this.valueInput.getAttribute('value')
  }

  setValue(value:string):webdriver.promise.Promise<void>{
    return this.valueInput.sendKeys(value)
  }
}

export class TestInputDropdown extends TestInputComponent {
  valueInput:protractor.ElementFinder
  valueDisplay:protractor.ElementFinder
  search:protractor.ElementFinder
  menu:protractor.ElementFinder
  items:protractor.ElementArrayFinder

  constructor(root:protractor.ElementFinder) {
    super(root)
    this.search = root.element(by.css('cw-input-dropdown INPUT.search'))
    this.valueInput = root.element(by.css('cw-input-dropdown INPUT[type="hidden"]'))
    this.valueDisplay = root.element(by.css('cw-input-dropdown DIV.text'))
    this.menu = root.element(by.css('[class~="menu"]'))
    this.items = this.menu.all(by.css('[class~="item"]'))
  }

  setSearch(value:string):webdriver.promise.Promise<void>{
    return this.search.sendKeys(value)
  }

  getValueText():webdriver.promise.Promise<string> {
    return this.valueDisplay.getText()
  }
}

export class TestInputToggle extends TestInputComponent {
  el:protractor.ElementFinder
  button:protractor.ElementFinder
  valueInput:protractor.ElementFinder

  constructor(root:protractor.ElementFinder) {
    super(root)
    this.valueInput = root.element(by.tagName('INPUT'))
    this.button = root.element(by.css('.ui.toggle'))
  }

  toggle():webdriver.promise.Promise<void>{
    return this.button.click()
  }

  setValue(enabled:boolean):webdriver.promise.Promise<void>{
    return this.value().then((b:boolean)=>{
      if(b !== enabled){
        return this.toggle()
      }
    })
  }

  value():webdriver.promise.Promise<boolean>{
    return this.valueInput.getAttribute('value').then((v)=>{
      return v === 'true'
    })
  }

  getValueText():webdriver.promise.Promise<string> {
    return this.valueInput.getText()
  }
}
