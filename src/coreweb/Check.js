import XDebug from './log-config.js';
let log = XDebug('DC.Check');

import {Verify} from '../entity-forge/index.js'


let createCheckError = function (validation, value,  message) {
  let e = new Error('Check.' + validation + " failed: '" + message + "'.")
  e['validation'] = validation;
  e['validatedValue'] = value;
  return e;
}

export let Check = {

  exists(value, message = 'Value does not exist') {
    if(!Verify.exists(value)){
      throw createCheckError('exists', value, message)
    }
    return value
  },

  isString(value, message = 'Value is not a string') {
    if(!Verify.isString(value)) {
      throw createCheckError('isString', value, message)
    }
    return value
  },

  notEmpty(value, message = 'The value is empty') {
    if(!Verify.minLength(value, 1)){
      throw createCheckError('notEmpty', value, message)
    }
    return value
  }
}



