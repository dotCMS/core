var webdriver = require('selenium-webdriver');
module.exports = {
  verifyNoBrowserErrors: verifyNoBrowserErrors,
  clickAll: clickAll
};
function clickAll(buttonSelectors) {
  buttonSelectors.forEach(function(selector) {
    $(selector).click();
  });
}
function verifyNoBrowserErrors() {
  browser.executeScript('1+1');
  browser.manage().logs().get('browser').then(function(browserLog) {
    var filteredLog = browserLog.filter(function(logEntry) {
      if (logEntry.level.value >= webdriver.logging.Level.INFO.value) {
        console.log('>> ' + logEntry.message);
      }
      return logEntry.level.value > webdriver.logging.Level.WARNING.value;
    });
    expect(filteredLog.length).toEqual(0);
  });
}
//# sourceMappingURL=e2e_util.es6.map

//# sourceMappingURL=./e2e_util.map