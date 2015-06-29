import XDebug from 'debug';
let log = XDebug('Check.specs');
import {Check} from './Check.js';

describe('Check.notEmpty', function () {
  beforeEach(function () {
  });

  it('returns error result when provided null value.', function () {
    expect(() => {
        Check.notEmpty(null, "The message")
      }
    ).toThrowError(/.*The message.*/)

  });

  it('returns error result when provided a number value.', function () {
    expect(() => {
        Check.notEmpty(1, "The message")
      }
    ).toThrowError(/.*The message.*/)

  });

});


describe('Check.Exists', function () {
  beforeEach(function () {
  });

  it('returns error result when provided null value.', function () {
    expect(() => {
        Check.exists(null, "The message") }
    ).toThrowError(/.*The message.*/)

  });

  it('does not throw error provided empty string.', function () {
    expect(() => {
        Check.exists("", "The message") }
    ).not.toThrow(/.*The message.*/)
  });


});