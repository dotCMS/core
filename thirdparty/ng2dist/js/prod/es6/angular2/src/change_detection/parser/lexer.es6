import {Injectable} from 'angular2/di';
import {List,
  ListWrapper,
  SetWrapper} from "angular2/src/facade/collection";
import {int,
  NumberWrapper,
  StringJoiner,
  StringWrapper} from "angular2/src/facade/lang";
export const TOKEN_TYPE_CHARACTER = 1;
export const TOKEN_TYPE_IDENTIFIER = 2;
export const TOKEN_TYPE_KEYWORD = 3;
export const TOKEN_TYPE_STRING = 4;
export const TOKEN_TYPE_OPERATOR = 5;
export const TOKEN_TYPE_NUMBER = 6;
export class Lexer {
  tokenize(text) {
    var scanner = new _Scanner(text);
    var tokens = [];
    var token = scanner.scanToken();
    while (token != null) {
      ListWrapper.push(tokens, token);
      token = scanner.scanToken();
    }
    return tokens;
  }
}
Object.defineProperty(Lexer, "annotations", {get: function() {
    return [new Injectable()];
  }});
Object.defineProperty(Lexer.prototype.tokenize, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
export class Token {
  constructor(index, type, numValue, strValue) {
    this.index = index;
    this.type = type;
    this._numValue = numValue;
    this._strValue = strValue;
  }
  isCharacter(code) {
    return (this.type == TOKEN_TYPE_CHARACTER && this._numValue == code);
  }
  isNumber() {
    return (this.type == TOKEN_TYPE_NUMBER);
  }
  isString() {
    return (this.type == TOKEN_TYPE_STRING);
  }
  isOperator(operater) {
    return (this.type == TOKEN_TYPE_OPERATOR && this._strValue == operater);
  }
  isIdentifier() {
    return (this.type == TOKEN_TYPE_IDENTIFIER);
  }
  isKeyword() {
    return (this.type == TOKEN_TYPE_KEYWORD);
  }
  isKeywordVar() {
    return (this.type == TOKEN_TYPE_KEYWORD && this._strValue == "var");
  }
  isKeywordNull() {
    return (this.type == TOKEN_TYPE_KEYWORD && this._strValue == "null");
  }
  isKeywordUndefined() {
    return (this.type == TOKEN_TYPE_KEYWORD && this._strValue == "undefined");
  }
  isKeywordTrue() {
    return (this.type == TOKEN_TYPE_KEYWORD && this._strValue == "true");
  }
  isKeywordFalse() {
    return (this.type == TOKEN_TYPE_KEYWORD && this._strValue == "false");
  }
  toNumber() {
    return (this.type == TOKEN_TYPE_NUMBER) ? this._numValue : -1;
  }
  toString() {
    var type = this.type;
    if (type >= TOKEN_TYPE_CHARACTER && type <= TOKEN_TYPE_STRING) {
      return this._strValue;
    } else if (type == TOKEN_TYPE_NUMBER) {
      return this._numValue.toString();
    } else {
      return null;
    }
  }
}
Object.defineProperty(Token, "parameters", {get: function() {
    return [[int], [int], [assert.type.number], [assert.type.string]];
  }});
Object.defineProperty(Token.prototype.isCharacter, "parameters", {get: function() {
    return [[int]];
  }});
Object.defineProperty(Token.prototype.isOperator, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
function newCharacterToken(index, code) {
  return new Token(index, TOKEN_TYPE_CHARACTER, code, StringWrapper.fromCharCode(code));
}
Object.defineProperty(newCharacterToken, "parameters", {get: function() {
    return [[int], [int]];
  }});
function newIdentifierToken(index, text) {
  return new Token(index, TOKEN_TYPE_IDENTIFIER, 0, text);
}
Object.defineProperty(newIdentifierToken, "parameters", {get: function() {
    return [[int], [assert.type.string]];
  }});
function newKeywordToken(index, text) {
  return new Token(index, TOKEN_TYPE_KEYWORD, 0, text);
}
Object.defineProperty(newKeywordToken, "parameters", {get: function() {
    return [[int], [assert.type.string]];
  }});
function newOperatorToken(index, text) {
  return new Token(index, TOKEN_TYPE_OPERATOR, 0, text);
}
Object.defineProperty(newOperatorToken, "parameters", {get: function() {
    return [[int], [assert.type.string]];
  }});
function newStringToken(index, text) {
  return new Token(index, TOKEN_TYPE_STRING, 0, text);
}
Object.defineProperty(newStringToken, "parameters", {get: function() {
    return [[int], [assert.type.string]];
  }});
function newNumberToken(index, n) {
  return new Token(index, TOKEN_TYPE_NUMBER, n, "");
}
Object.defineProperty(newNumberToken, "parameters", {get: function() {
    return [[int], [assert.type.number]];
  }});
export var EOF = new Token(-1, 0, 0, "");
export const $EOF = 0;
export const $TAB = 9;
export const $LF = 10;
export const $VTAB = 11;
export const $FF = 12;
export const $CR = 13;
export const $SPACE = 32;
export const $BANG = 33;
export const $DQ = 34;
export const $HASH = 35;
export const $$ = 36;
export const $PERCENT = 37;
export const $AMPERSAND = 38;
export const $SQ = 39;
export const $LPAREN = 40;
export const $RPAREN = 41;
export const $STAR = 42;
export const $PLUS = 43;
export const $COMMA = 44;
export const $MINUS = 45;
export const $PERIOD = 46;
export const $SLASH = 47;
export const $COLON = 58;
export const $SEMICOLON = 59;
export const $LT = 60;
export const $EQ = 61;
export const $GT = 62;
export const $QUESTION = 63;
const $0 = 48;
const $9 = 57;
const $A = 65,
    $E = 69,
    $Z = 90;
export const $LBRACKET = 91;
export const $BACKSLASH = 92;
export const $RBRACKET = 93;
const $CARET = 94;
const $_ = 95;
const $a = 97,
    $e = 101,
    $f = 102,
    $n = 110,
    $r = 114,
    $t = 116,
    $u = 117,
    $v = 118,
    $z = 122;
export const $LBRACE = 123;
export const $BAR = 124;
export const $RBRACE = 125;
const $NBSP = 160;
export class ScannerError extends Error {
  constructor(message) {
    super();
    this.message = message;
  }
  toString() {
    return this.message;
  }
}
class _Scanner {
  constructor(input) {
    this.input = input;
    this.length = input.length;
    this.peek = 0;
    this.index = -1;
    this.advance();
  }
  advance() {
    this.peek = ++this.index >= this.length ? $EOF : StringWrapper.charCodeAt(this.input, this.index);
  }
  scanToken() {
    var input = this.input,
        length = this.length,
        peek = this.peek,
        index = this.index;
    while (peek <= $SPACE) {
      if (++index >= length) {
        peek = $EOF;
        break;
      } else {
        peek = StringWrapper.charCodeAt(input, index);
      }
    }
    this.peek = peek;
    this.index = index;
    if (index >= length) {
      return null;
    }
    if (isIdentifierStart(peek))
      return this.scanIdentifier();
    if (isDigit(peek))
      return this.scanNumber(index);
    var start = index;
    switch (peek) {
      case $PERIOD:
        this.advance();
        return isDigit(this.peek) ? this.scanNumber(start) : newCharacterToken(start, $PERIOD);
      case $LPAREN:
      case $RPAREN:
      case $LBRACE:
      case $RBRACE:
      case $LBRACKET:
      case $RBRACKET:
      case $COMMA:
      case $COLON:
      case $SEMICOLON:
        return this.scanCharacter(start, peek);
      case $SQ:
      case $DQ:
        return this.scanString();
      case $HASH:
        return this.scanOperator(start, StringWrapper.fromCharCode(peek));
      case $PLUS:
      case $MINUS:
      case $STAR:
      case $SLASH:
      case $PERCENT:
      case $CARET:
      case $QUESTION:
        return this.scanOperator(start, StringWrapper.fromCharCode(peek));
      case $LT:
      case $GT:
      case $BANG:
      case $EQ:
        return this.scanComplexOperator(start, $EQ, StringWrapper.fromCharCode(peek), '=');
      case $AMPERSAND:
        return this.scanComplexOperator(start, $AMPERSAND, '&', '&');
      case $BAR:
        return this.scanComplexOperator(start, $BAR, '|', '|');
      case $NBSP:
        while (isWhitespace(this.peek))
          this.advance();
        return this.scanToken();
    }
    this.error(`Unexpected character [${StringWrapper.fromCharCode(peek)}]`, 0);
    return null;
  }
  scanCharacter(start, code) {
    assert(this.peek == code);
    this.advance();
    return newCharacterToken(start, code);
  }
  scanOperator(start, str) {
    assert(this.peek == StringWrapper.charCodeAt(str, 0));
    assert(SetWrapper.has(OPERATORS, str));
    this.advance();
    return newOperatorToken(start, str);
  }
  scanComplexOperator(start, code, one, two) {
    assert(this.peek == StringWrapper.charCodeAt(one, 0));
    this.advance();
    var str = one;
    while (this.peek == code) {
      this.advance();
      str += two;
    }
    assert(SetWrapper.has(OPERATORS, str));
    return newOperatorToken(start, str);
  }
  scanIdentifier() {
    assert(isIdentifierStart(this.peek));
    var start = this.index;
    this.advance();
    while (isIdentifierPart(this.peek))
      this.advance();
    var str = this.input.substring(start, this.index);
    if (SetWrapper.has(KEYWORDS, str)) {
      return newKeywordToken(start, str);
    } else {
      return newIdentifierToken(start, str);
    }
  }
  scanNumber(start) {
    assert(isDigit(this.peek));
    var simple = (this.index === start);
    this.advance();
    while (true) {
      if (isDigit(this.peek)) {} else if (this.peek == $PERIOD) {
        simple = false;
      } else if (isExponentStart(this.peek)) {
        this.advance();
        if (isExponentSign(this.peek))
          this.advance();
        if (!isDigit(this.peek))
          this.error('Invalid exponent', -1);
        simple = false;
      } else {
        break;
      }
      this.advance();
    }
    var str = this.input.substring(start, this.index);
    var value = simple ? NumberWrapper.parseIntAutoRadix(str) : NumberWrapper.parseFloat(str);
    return newNumberToken(start, value);
  }
  scanString() {
    assert(this.peek == $SQ || this.peek == $DQ);
    var start = this.index;
    var quote = this.peek;
    this.advance();
    var buffer;
    var marker = this.index;
    var input = this.input;
    while (this.peek != quote) {
      if (this.peek == $BACKSLASH) {
        if (buffer == null)
          buffer = new StringJoiner();
        buffer.add(input.substring(marker, this.index));
        this.advance();
        var unescapedCode;
        if (this.peek == $u) {
          var hex = input.substring(this.index + 1, this.index + 5);
          try {
            unescapedCode = NumberWrapper.parseInt(hex, 16);
          } catch (e) {
            this.error(`Invalid unicode escape [\\u${hex}]`, 0);
          }
          for (var i = 0; i < 5; i++) {
            this.advance();
          }
        } else {
          unescapedCode = unescape(this.peek);
          this.advance();
        }
        buffer.add(StringWrapper.fromCharCode(unescapedCode));
        marker = this.index;
      } else if (this.peek == $EOF) {
        this.error('Unterminated quote', 0);
      } else {
        this.advance();
      }
    }
    var last = input.substring(marker, this.index);
    this.advance();
    var unescaped = last;
    if (buffer != null) {
      buffer.add(last);
      unescaped = buffer.toString();
    }
    return newStringToken(start, unescaped);
  }
  error(message, offset) {
    var position = this.index + offset;
    throw new ScannerError(`Lexer Error: ${message} at column ${position} in expression [${this.input}]`);
  }
}
Object.defineProperty(_Scanner, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(_Scanner.prototype.scanCharacter, "parameters", {get: function() {
    return [[int], [int]];
  }});
Object.defineProperty(_Scanner.prototype.scanOperator, "parameters", {get: function() {
    return [[int], [assert.type.string]];
  }});
Object.defineProperty(_Scanner.prototype.scanComplexOperator, "parameters", {get: function() {
    return [[int], [int], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(_Scanner.prototype.scanNumber, "parameters", {get: function() {
    return [[int]];
  }});
Object.defineProperty(_Scanner.prototype.error, "parameters", {get: function() {
    return [[assert.type.string], [int]];
  }});
function isWhitespace(code) {
  return (code >= $TAB && code <= $SPACE) || (code == $NBSP);
}
Object.defineProperty(isWhitespace, "parameters", {get: function() {
    return [[int]];
  }});
function isIdentifierStart(code) {
  return ($a <= code && code <= $z) || ($A <= code && code <= $Z) || (code == $_) || (code == $$);
}
Object.defineProperty(isIdentifierStart, "parameters", {get: function() {
    return [[int]];
  }});
function isIdentifierPart(code) {
  return ($a <= code && code <= $z) || ($A <= code && code <= $Z) || ($0 <= code && code <= $9) || (code == $_) || (code == $$);
}
Object.defineProperty(isIdentifierPart, "parameters", {get: function() {
    return [[int]];
  }});
function isDigit(code) {
  return $0 <= code && code <= $9;
}
Object.defineProperty(isDigit, "parameters", {get: function() {
    return [[int]];
  }});
function isExponentStart(code) {
  return code == $e || code == $E;
}
Object.defineProperty(isExponentStart, "parameters", {get: function() {
    return [[int]];
  }});
function isExponentSign(code) {
  return code == $MINUS || code == $PLUS;
}
Object.defineProperty(isExponentSign, "parameters", {get: function() {
    return [[int]];
  }});
function unescape(code) {
  switch (code) {
    case $n:
      return $LF;
    case $f:
      return $FF;
    case $r:
      return $CR;
    case $t:
      return $TAB;
    case $v:
      return $VTAB;
    default:
      return code;
  }
}
Object.defineProperty(unescape, "parameters", {get: function() {
    return [[int]];
  }});
var OPERATORS = SetWrapper.createFromList(['+', '-', '*', '/', '%', '^', '=', '==', '!=', '===', '!==', '<', '>', '<=', '>=', '&&', '||', '&', '|', '!', '?', '#']);
var KEYWORDS = SetWrapper.createFromList(['var', 'null', 'undefined', 'true', 'false']);
//# sourceMappingURL=lexer.js.map

//# sourceMappingURL=./lexer.map