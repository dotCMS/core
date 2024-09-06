import { Check } from './Check';
const errorMessage = 'The message';
let errorRegex;

describe('Unit.validation.Check.notEmpty', function () {
    const validCases = [
        { args: ' ', description: 'a single space.' },
        { args: 'some text', description: 'a couple of words' },
        { args: 'null', description: 'the literal string `null`' },
        { args: 'undefined', description: 'The literal string `undefined`' },
        { args: '1', description: 'The quoted number 1' },
        { args: '0', description: 'The quoted number zero' },
        { args: new String(' '), description: 'a single space as an instance of String.' }
    ];

    const invalidCases = [
        { args: '', description: 'an empty string literal' },
        { args: 1, description: 'the number value 1' },
        { args: 0, description: 'the number value zero' },
        { args: null, description: 'a literal null' },
        { args: undefined, description: 'a literal undefined' },
        { args: /foo/, description: 'a regex' }
    ];

    beforeEach(function () {
        errorRegex = RegExp(errorMessage);
    });

    validCases.forEach((testCase) => {
        it('to pass when provided ' + testCase.description + '.', function () {
            expect(Check.notEmpty(testCase.args, errorMessage)).toEqual(testCase.args);
        });
    });

    invalidCases.forEach((testCase) => {
        it('to throw an error when provided ' + testCase.description + '.', function () {
            expect(() => {
                Check.notEmpty(testCase.args, errorMessage);
            }).toThrowError(errorRegex);
        });
    });

    it('to return error result when provided a custom type.', function () {
        class Foo {
            afield;

            constructor() {
                this.afield = 'something';
            }
        }
        expect(() => {
            Check.notEmpty(new Foo(), errorMessage);
        }).toThrowError(errorRegex);
    });
});

describe('Unit.validation.Check.Exists', function () {
    const validCases = [
        { args: ' ', description: 'a single space.' },
        { args: '', description: 'an empty string' },
        { args: 1, description: 'a number' },
        { args: 'null', description: 'the literal string `null`' },
        { args: 'undefined', description: 'The literal string `undefined`' },
        { args: '1', description: 'The quoted number 1' },
        { args: '0', description: 'The quoted number zero' },
        { args: new String(' '), description: 'a single space as an instance of String.' },
        { args: /bob/, description: 'A regex.' },
        { args: {}, description: 'An object literal (`{}`)' }
    ];

    const invalidCases = [
        { args: null, description: 'a literal null' },
        { args: undefined, description: 'a literal undefined' }
    ];

    beforeEach(function () {
        errorRegex = RegExp(errorMessage, 'ig');
    });

    validCases.forEach((testCase) => {
        it('to pass when provided ' + testCase.description + '.', function () {
            expect(Check.exists(testCase.args, errorMessage)).toEqual(testCase.args);
        });
    });

    invalidCases.forEach((testCase) => {
        it('to throw an error when provided ' + testCase.description + '.', function () {
            expect(() => {
                Check.exists(testCase.args, errorMessage);
            }).toThrowError(errorRegex);
        });
    });
});

describe('Unit.validation.Check.isString', function () {
    const validCases = [
        { args: '', description: 'an empty string literal' },
        { args: ' ', description: 'a single space.' },
        { args: 'some text', description: 'a couple of words' },
        { args: 'null', description: 'the literal string `null`' },
        { args: 'undefined', description: 'The literal string `undefined`' },
        { args: '1', description: 'The quoted number 1' },
        { args: '0', description: 'The quoted number zero' },
        { args: new String(' '), description: 'a single space as an instance of String.' }
    ];

    const invalidCases = [
        { args: 1, description: 'the number value 1' },
        { args: 0, description: 'the number value zero' },
        { args: null, description: 'a literal null' },
        { args: undefined, description: 'a literal undefined' },
        { args: /foo/, description: 'a regex' }
    ];

    beforeEach(function () {
        errorRegex = RegExp(errorMessage);
    });

    validCases.forEach((testCase) => {
        it('to pass when provided ' + testCase.description + '.', function () {
            expect(Check.isString(testCase.args, errorMessage)).toEqual(testCase.args);
        });
    });

    invalidCases.forEach((testCase) => {
        it('to throw an error when provided ' + testCase.description + '.', function () {
            expect(() => {
                Check.isString(testCase.args, errorMessage);
            }).toThrowError(errorRegex);
        });
    });

    it('to return error result when provided a custom type.', function () {
        class Foo {
            afield;

            constructor() {
                this.afield = 'something';
            }
        }
        expect(() => {
            Check.isString(new Foo(), errorMessage);
        }).toThrowError(errorRegex);
    });
});
