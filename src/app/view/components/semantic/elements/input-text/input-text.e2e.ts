

describe('The input-text demo', function() {
  it('should have a title.', function() {
    browser.get('http://localhost:9000/build/view/components/semantic/elements/input-text.html');
    expect(browser.getTitle()).toEqual('Input-Text Demo');
  });




});