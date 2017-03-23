import ElementFinder = protractor.ElementFinder;

export class Page {

  baseUrl:string
  title:string
  queryParams:{[key:string]:string}


  constructor(baseUrl:string, title:string, queryParams:{[key:string]:string} = {}) {
    this.baseUrl = baseUrl;
    this.title = title;
    this.queryParams = queryParams
  }

  getFullUrl(){
    var sep = '?'
    let v = Object.keys(this.queryParams).reduce((url, param)=>{
      let next = sep + url + param + '=' + this.queryParams[param] + '&'
      sep = '&'
      return next
    }, "")
    return this.baseUrl + v
  }

  navigateTo():webdriver.promise.Promise<Page>{
    let result = browser.get(this.getFullUrl())
    expect(browser.getTitle()).toEqual(this.title);
    return result.then((  ) =>  this )
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

  click():webdriver.promise.Promise<void>{
    return this.el.click()
  }

  /**
   * Alt/Opt + Shift + Click is an undocumented convenience for supressing alerts that would
   * otherwise be displayed. Note that this method of 'clicking' does not work on Safari.
   * @returns {webdriver.promise.Promise<void>}
   */
  optShiftClick():webdriver.promise.Promise<void> {
      return this.el.sendKeys(protractor.Key.chord(protractor.Key.SHIFT, protractor.Key.ALT, ' '))
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

  placeholder():webdriver.promise.Promise<string>{
    return this.valueInput.getAttribute('placeholder')
  }

  getValue():webdriver.promise.Promise<string>{
    return this.valueInput.getAttribute('value')
  }

  setValue(value:string, el?:protractor.ElementFinder):webdriver.promise.Promise<void>{
    this.valueInput.clear()
    let p = this.valueInput.sendKeys(value)
    if(el){
      p = el.click()
    }
    return p
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
    this.search.clear()
    let p = this.search.sendKeys(value)
    p = browser.element(by.css('body')).click()
    return p
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
    this.button = root.element(by.css('.ui.toggle')).element(by.tagName('input'))
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
