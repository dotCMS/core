import ElementFinder = protractor.ElementFinder;
export class Page {

  url:string
  title:string


  constructor(url:string, title:string) {
    this.url = url;
    this.title = title;
  }

  navigateTo(){
    browser.get(this.url)
    expect(browser.getTitle()).toEqual(this.title);
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

export class TestTextInput {
  el:ElementFinder
  valueInput:ElementFinder
  icon:ElementFinder

  constructor(el:ElementFinder) {
    this.el = el;
    this.valueInput = this.el.element(by.tagName('INPUT'))
    this.icon = this.el.element(by.tagName('I'))
  }

  placeholder(){
    return this.valueInput.getAttribute('placeholder')
  }

  getValue(){
    return this.valueInput.getAttribute('value')
  }

  setValue(value:string){
    this.valueInput.sendKeys(value)
  }
}

export class DDInput {
  el:protractor.ElementFinder
  valueInput:protractor.ElementFinder
  valueDisplay:protractor.ElementFinder
  search:protractor.ElementFinder
  menu:protractor.ElementFinder
  items:protractor.ElementArrayFinder

  constructor(root:protractor.ElementFinder) {
    this.el = root
    this.search = root.element(by.css('cw-input-dropdown INPUT.search'))
    this.valueInput = root.element(by.css('cw-input-dropdown INPUT[type="hidden"]'))
    this.valueInput = root.element(by.css('cw-input-dropdown DIV.text'))
    this.menu = root.element(by.css('[class~="menu"]'))
    this.items = this.menu.all(by.css('[class~="item"]'))
  }

  setSearch(value:string){
    this.search.sendKeys(value)
  }

  getValueText():webdriver.promise.Promise<string> {
    return this.valueInput.getText()
  }
}
