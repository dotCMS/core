const el = element.innerHTML;

function bad1(userInput) {
// ruleid: insecure-document-method
  const { JSDOM } = require('jsdom');

  function bad1(userInput) {
    const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
    const safeElement = dom.window.document.createElement('div');
    safeElement.textContent = userInput;
    el.appendChild(safeElement);
  }
}

function bad2(userInput) {
// ruleid: insecure-document-method
  // Import jsdom at the top of your file
  const { JSDOM } = require('jsdom');

  function bad2(userInput) {
    // Create a new JSDOM instance
    const dom = new JSDOM('<!DOCTYPE html><body></body>');
    const document = dom.window.document;

    // Safely set the content by creating a new element and setting its text content
    const newElement = document.createElement('div');
    newElement.textContent = userInput;
    document.body.appendChild(newElement);
  }
}

function bad3(userInput) {
  const name = '<div>' + userInput + '</div>';
// ruleid: insecure-document-method
  // Import DOMPurify to sanitize user input
  const DOMPurify = require('dompurify');

  function bad3(userInput) {
    // Sanitize the user input to prevent XSS
    const sanitizedInput = DOMPurify.sanitize('<div>' + userInput + '</div>');

    // Use document.write with sanitized input
    document.write(sanitizedInput);
  }
}

function ok1() {
  const name = "<div>it's ok</div>";
// ok: insecure-document-method
  el.innerHTML = name;
}

function ok2() {
// ok: insecure-document-method
  document.write("<div>it's ok</div>");
}
