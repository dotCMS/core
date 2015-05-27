var _global = typeof window === 'undefined' ? global : window;
export { _global as global };
// HACK: workaround for Traceur behavior.
// It expects all transpiled modules to contain this marker.
// TODO: remove this when we no longer use traceur
export var __esModule = true;
export var Type = Function;
export var Math = _global.Math;
export var Date = _global.Date;
var assertionsEnabled_ = typeof assert !== 'undefined';
var int;
// global assert support, as Dart has it...
// TODO: `assert` calls need to be removed in production code!
if (assertionsEnabled_) {
    _global.assert = assert;
    // `int` is not a valid JS type
    int = assert.define('int', function (value) {
        return typeof value === 'number' && value % 1 === 0;
    });
}
else {
    int = {};
    _global.assert = function () {
    };
}
export { int };
export class CONST {
}
export class ABSTRACT {
}
export class IMPLEMENTS {
}
export function isPresent(obj) {
    return obj !== undefined && obj !== null;
}
export function isBlank(obj) {
    return obj === undefined || obj === null;
}
export function isString(obj) {
    return typeof obj === "string";
}
export function isFunction(obj) {
    return typeof obj === "function";
}
export function isType(obj) {
    return isFunction(obj);
}
export function stringify(token) {
    if (typeof token === 'string') {
        return token;
    }
    if (token === undefined || token === null) {
        return '' + token;
    }
    if (token.name) {
        return token.name;
    }
    return token.toString();
}
export class StringWrapper {
    static fromCharCode(code) {
        return String.fromCharCode(code);
    }
    static charCodeAt(s, index) {
        return s.charCodeAt(index);
    }
    static split(s, regExp) {
        return s.split(regExp);
    }
    static equals(s, s2) {
        return s === s2;
    }
    static replace(s, from, replace) {
        return s.replace(from, replace);
    }
    static replaceAll(s, from, replace) {
        return s.replace(from, replace);
    }
    static startsWith(s, start) {
        return s.startsWith(start);
    }
    static substring(s, start, end = null) {
        return s.substring(start, end === null ? undefined : end);
    }
    static replaceAllMapped(s, from, cb) {
        return s.replace(from, function (...matches) {
            // Remove offset & string from the result array
            matches.splice(-2, 2);
            // The callback receives match, p1, ..., pn
            return cb(matches);
        });
    }
    static contains(s, substr) {
        return s.indexOf(substr) != -1;
    }
}
export class StringJoiner {
    constructor(parts = []) {
        this.parts = parts;
    }
    add(part) {
        this.parts.push(part);
    }
    toString() {
        return this.parts.join("");
    }
}
export class NumberParseError {
    constructor(message) {
        this.message = message;
    }
    toString() {
        return this.message;
    }
}
export class NumberWrapper {
    static toFixed(n, fractionDigits) {
        return n.toFixed(fractionDigits);
    }
    static equal(a, b) {
        return a === b;
    }
    static parseIntAutoRadix(text) {
        var result = parseInt(text);
        if (isNaN(result)) {
            throw new NumberParseError("Invalid integer literal when parsing " + text);
        }
        return result;
    }
    static parseInt(text, radix) {
        if (radix == 10) {
            if (/^(\-|\+)?[0-9]+$/.test(text)) {
                return parseInt(text, radix);
            }
        }
        else if (radix == 16) {
            if (/^(\-|\+)?[0-9ABCDEFabcdef]+$/.test(text)) {
                return parseInt(text, radix);
            }
        }
        else {
            var result = parseInt(text, radix);
            if (!isNaN(result)) {
                return result;
            }
        }
        throw new NumberParseError("Invalid integer literal when parsing " + text + " in base " + radix);
    }
    // TODO: NaN is a valid literal but is returned by parseFloat to indicate an error.
    static parseFloat(text) {
        return parseFloat(text);
    }
    static get NaN() {
        return NaN;
    }
    static isNaN(value) {
        return isNaN(value);
    }
    static isInteger(value) {
        return Number.isInteger(value);
    }
}
export var RegExp = _global.RegExp;
export class RegExpWrapper {
    static create(regExpStr, flags = '') {
        flags = flags.replace(/g/g, '');
        return new _global.RegExp(regExpStr, flags + 'g');
    }
    static firstMatch(regExp, input) {
        // Reset multimatch regex state
        regExp.lastIndex = 0;
        return regExp.exec(input);
    }
    static matcher(regExp, input) {
        // Reset regex state for the case
        // someone did not loop over all matches
        // last time.
        regExp.lastIndex = 0;
        return {
            re: regExp,
            input: input
        };
    }
}
export class RegExpMatcherWrapper {
    static next(matcher) {
        return matcher.re.exec(matcher.input);
    }
}
export class FunctionWrapper {
    static apply(fn, posArgs) {
        return fn.apply(null, posArgs);
    }
}
// No subclass so that we preserve error stack.
export var BaseException = Error;
// JS has NaN !== NaN
export function looseIdentical(a, b) {
    return a === b || typeof a === "number" && typeof b === "number" && isNaN(a) && isNaN(b);
}
// JS considers NaN is the same as NaN for map Key (while NaN !== NaN otherwise)
// see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map
export function getMapKey(value) {
    return value;
}
export function normalizeBlank(obj) {
    return isBlank(obj) ? null : obj;
}
export function isJsObject(o) {
    return o !== null && (typeof o === "function" || typeof o === "object");
}
export function assertionsEnabled() {
    return assertionsEnabled_;
}
export function print(obj) {
    if (obj instanceof Error) {
        console.log(obj.stack);
    }
    else {
        console.log(obj);
    }
}
// Can't be all uppercase as our transpiler would think it is a special directive...
export var Json = _global.JSON;
export class DateWrapper {
    static fromMillis(ms) {
        return new Date(ms);
    }
    static toMillis(date) {
        return date.getTime();
    }
    static now() {
        return new Date();
    }
    static toJson(date) {
        return date.toJSON();
    }
}
//# sourceMappingURL=lang.js.map