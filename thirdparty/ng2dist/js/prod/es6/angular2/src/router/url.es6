import {RegExpWrapper,
  StringWrapper} from 'angular2/src/facade/lang';
var specialCharacters = ['/', '.', '*', '+', '?', '|', '(', ')', '[', ']', '{', '}', '\\'];
var escapeRe = RegExpWrapper.create('(\\' + specialCharacters.join('|\\') + ')', 'g');
export function escapeRegex(string) {
  return StringWrapper.replaceAllMapped(string, escapeRe, (match) => {
    return "\\" + match;
  });
}
Object.defineProperty(escapeRegex, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
//# sourceMappingURL=url.js.map

//# sourceMappingURL=./url.map