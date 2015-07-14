import  {LazyVerify, Verify} from './Verify.js'
import  {ValidationError} from './ValidationError.js'

class EntityBlank {

  constructor() {
  }

  toJson(validate=true){
    if(validate !== false){
      this.validate()
    }
    return JSON.stringify(this)
  }

}

export {EntityBlank}


let propertyErrorsToString = function () {
  return '[' + this.errors.map((error)=> error.validator.msg).join("],    [") + ']'
}


/**
 * State 1: Defining a new data type:
 *    let EF = EntityForge
 *    let Foo = EF.obj("Foo", { name : EF.string().minLength(1).maxLength(25) }).notNull().asNewable()
 *
 * State 2: Creating an instance of a data type and setting values:
 *    let fooInstance = new Foo()
 *    fooInstance.name = null // throws validation error
 *    fooInstance.name = '' // throws validation error
 *    fooInstance.name = 'Narwhal' // no error
 *
 * State 3: Calling validate on an instance of a data type (e.g. 'Foo', above).
 *    let fooInstance = new Foo()
 *    fooInstance.validate() // throws error, name is null.
 *    fooInstance.name = 'Honey Badger' // no error
 *    fooInstance.validate() // no error
 *
 *  ------
 *
 *  State 2 flow:
 *     - newInstance is called
 *     - - optional config object used as defaultValue.
 *     - - If defaultValue is absent use the value provided by 'defaultValue' getter instead.
 *     - - If defaultValue still undefined , create empty object.
 *    <- - If defaultValue is empty and empty values are allowed, set current value to the empty object and return.
 *     - - pass defaultValue through the 'transform' function and proceed using the resulting value.
 *     - - iterate through defined child fields, performing this process on each.
 *    <- - return the new instance
 *
 *     ======
 *     - set {value} is called
 *     - -
 */
class BaseForge {

  constructor(defaultValue = null) {
    this._defaultValue = defaultValue
    this._v = {}
    // need to iterate by insertion order; can't reuse 'v'
    this._vAry = []
  }

  _applyValidation(cfg) {
    if (!this._v[cfg.name]) {
      this._vAry.push(cfg)
      this._v[cfg.name] = cfg;
    }
    return this
  }

  newInstance(cfg = null) {
    if (cfg === null) {
      cfg = this.defaultValue
    }
    return this.transform(cfg)
  }

  transform(value) {
    return value
  }

  get defaultValue() {
    return Verify.isFunction(this._defaultValue) ? this._defaultValue() : this._defaultValue
  }


  validate(value, fieldName = '') {
    let aborted = false
    let valid = true
    let errors = [];
    this._vAry.forEach((validator)=> {
      if (!aborted) {
        let vResult = validator.fn.apply(null, [value].concat(validator.args || []))
        if (vResult === true || (vResult && vResult.valid === true)) {
          return
        }
        if (vResult === false) {
          valid = false
          aborted = validator.abortOnFail === true
          errors.push({
            value: value,
            fieldName: fieldName,
            validator: validator,
            aborted: aborted
          })
        } else if (vResult && Verify.isArray(vResult.errors)) {
          Array.prototype.push.apply(errors, vResult.errors)
        }
      }
    })
    return {valid: errors.length === 0, errors: errors, toString: propertyErrorsToString}
  }

  /**
   * Use Object.defineProperty to create a new setter and getter for 'fieldName' on the provided
   * entity instance.
   * @param entityInstance Any object which should get a setter and getter for this field definition.
   * @param fieldName
   * @param defaultValue
   * @returns {*}
   */
  _createPropertyDefinition(entityInstance, fieldName, defaultValue = null) {
    let wrapper = {
      value: defaultValue !== null ? this.transform(defaultValue) : this.newInstance()
    }
    Object.defineProperty(entityInstance, fieldName, {
      configurable: false,
      enumerable: this.isEnumerable !== false,
      set: this._createSetter(wrapper),
      get() {
        return wrapper.value
      }
    })
  }

  _createSetter(wrapper) {
    let fieldDef = this
    return function (value) {
      let transformed = fieldDef.transform(value)
      let r = fieldDef.validate(transformed)
      if (r.valid) {
        wrapper.value = transformed
      } else {
        throw new ValidationError(r)
      }
    }
  }


  /* Fluent configuration calls */
  initTo(defaultValue) {
    this._defaultValue = defaultValue;
    return this
  }

  exists(msg = "@validations.exists.doesNotExist") {
    return this._applyValidation({name: 'exists', fn: LazyVerify.exists, msg: msg, abortOnFail: true})
  }

  notNull(msg = "@validations.obj.objectIsNull") {
    return this._applyValidation({name: 'notNull', fn: LazyVerify.exists, msg: msg, abortOnFail: true})
  }

  enumerable(isEnumerable = true) {
    this.isEnumerable = isEnumerable
    return this
  }

}


class ObjectForge extends BaseForge {

  constructor(fieldName, fields = null, defaultValue = null) {
    super(defaultValue)
    this.fieldName = fieldName
    this.fieldDefinitions = fields || {}
  }

  asNewable() {
    let ctor = function(cfg = null) {
      return this.newInstance(cfg)
    }
    ctor.prototype = this
    return ctor
  }

  newInstance(cfg = null) {
    cfg = cfg || this.defaultValue || {}
    let theInstance = new EntityBlank(this)

    this._createValidateFn(theInstance)

    if (this._v.obj.allowEmptyAtInit !== true) {
      this._initMemberProperties(theInstance, cfg)
    }
    return theInstance
  }

  _createValidateFn(entityInstance){
    var typeDefinition = this
    Object.defineProperty(entityInstance, 'validate', {
      configurable: false,
      enumerable: false,
      writable: false,
      value: function () {
        let results = typeDefinition.validate(this, typeDefinition.fieldName)
        if (results.valid === false) {
          throw new ValidationError(results, typeDefinition.fieldName)
        }
        return this
      }
    })
  }

  _initMemberProperties(theInstance, cfg) {
    let fieldDefs = this.fieldDefinitions
    Object.keys(fieldDefs).forEach((key)=> {
      let field = fieldDefs[key]
      field._createPropertyDefinition(theInstance, key, cfg[key])
    })
    // when we define all the child properties we can lock the object. This prevents arbitrary new
    // properties from being added.
    Object.freeze(theInstance)
  }

  transform(entity) {
    return entity || (this._v.notNull ? {} : null)
  }

  /**
   * Case 1: typeDef whose instances must always be valid.
   * Case 2: typeDef whose instances' child members must always exist, but those values don't always need to be valid.
   * Case 3: typeDef whose instances can be empty when created, but empty is not a valid state.
   * Case 4: typeDef whose instances can be empty when created, and empty is a valid state.
   * @param wrapper
   * @returns {*}
   * @private
   */
  _createSetter(wrapper) {
    let fieldDef = this
    let fn;
    if (!fieldDef._v.obj.allowEmptyAtInit) {
      fn = super._createSetter(wrapper)
    }
    else {
      fn = function (value) {
        let transformed = fieldDef.transform(value)
        if (LazyVerify.hasOnly(transformed, ['validate'], true)) {
          // because we allow setting the value to '{}', but will still fail if validate is called.
          wrapper.value = transformed
        } else {
          let r = fieldDef.validate(transformed, fieldDef.fieldName)
          if (r.valid) {
            wrapper.value = transformed
          } else {
            throw new ValidationError(r, fieldDef.fieldName)
          }
        }
      }
    }
    return fn
  }

  static memberPropertiesValid(entity, objFieldDef) {
    let errors = []
    // skip the field validation if entity is empty. If notEmpty is set it will throw as part of the
    // validation step, after this call returns.
    if (Verify.exists(entity) && !LazyVerify.hasOnly(entity, ['validate'], true)) {
      Object.keys(objFieldDef.fieldDefinitions).forEach((key)=> {
        let fieldDef = objFieldDef.fieldDefinitions[key]
        let result = fieldDef.validate(entity[key], key)
        if (!result.valid) {
          Array.prototype.push.apply(errors, result.errors)
        }
      })
    }
    return {valid: errors.length === 0, errors: errors}
  }

  /**********************   Fluent config methods.  **********************/

  static obj(fieldName, fields = {}, defaultValue = null, msg = "@validations.obj.invalidChildMember") {
    let forge = new ObjectForge(fieldName, fields, defaultValue )
    return forge._applyValidation({
      name: 'obj',
      fn: ObjectForge.memberPropertiesValid,
      args: [forge],
      msg: msg,
      abortOnFail: true
    })
  }

  notEmpty(msg = "@validations.obj.objectIsEmpty") {
    return this._applyValidation({name: 'notEmpty', fn: function(value){ return !LazyVerify.emptyObject(value)}, msg: msg, abortOnFail: true})
  }

  allowEmptyAtInit(allowEmptyAtInit=true){
    this._v.obj.allowEmptyAtInit = allowEmptyAtInit
    return this
  }

}

class StringForge extends BaseForge {

  constructor() {
    super()
  }

  static string(defaultValue = null, msg = "@validations.string.notStringValue") {
    let forge = new StringForge()
    forge._defaultValue = defaultValue
    return forge._applyValidation({name: 'string', fn: Verify.isString, args: [true], msg: msg, abortOnFail: true})
  }

  minLength(min, msg = "@validations.string.minLength.tooShort") {
    return this.exists()._applyValidation({name: 'minLength', fn: LazyVerify.minLength, args: [min], msg: msg})
  }

  maxLength(max, msg = "@validations.string.maxLength.tooLong") {
    return this.exists()._applyValidation({name: 'maxLength', fn: LazyVerify.maxLength, args: [max], msg: msg})
  }
}

class NumberForge extends BaseForge {

  constructor() {
    super()
  }

  static number(defaultValue = 0, msg = "@validations.number.notNumber") {
    let forge = new NumberForge()
    forge._defaultValue = defaultValue
    return forge._applyValidation({name: 'number', fn: LazyVerify.isNumber, msg: msg, abortOnFail: true})
  }

  static int(defaultValue = 0, msg = "@validations.number.int.notInteger") {
    let forge = new NumberForge()
    forge._defaultValue = defaultValue
    return forge._applyValidation({name: 'int', fn: LazyVerify.isInteger, msg: msg, abortOnFail: true})
  }

  min(min, msg = "@validations.number.min.tooSmall") {
    return this._applyValidation({name: "min", fn: LazyVerify.min, args: [min], msg: msg, abortOnFail: true})
  }

  max(max, msg = "@validations.number.max.tooLarge") {
    return this._applyValidation({name: "max", fn: LazyVerify.max, args: [max], msg: msg, abortOnFail: true})
  }

}

class BooleanForge extends BaseForge {

  constructor() {
    super()
  }

  static bool(defaultValue = false, msg = "@validations.boolean.notBoolean") {
    let forge = new BooleanForge()
    forge._defaultValue = defaultValue
    return forge._applyValidation({name: 'bool', fn: LazyVerify.isBoolean, msg: msg, abortOnFail: true})
  }
}

class EnumForge extends BaseForge {

  constructor() {
    super()
    this.allowedValues = []
  }

  static enum(defaultValue = null, msg = "@validations.enum.notMember") {
    let forge = new EnumForge()
    forge._defaultValue = defaultValue
    return forge._applyValidation({
      name: 'enum',
      fn: function (value) {
        return EnumForge.isMember(forge.allowedValues, value)
      },
      msg: msg, abortOnFail: true
    })
  }

  values(values) {
    if (values && values.length > 0) {
      Array.prototype.push.apply(this.allowedValues, values)
    }
    return this
  }

  initTo(value) {
    let def = value
    if (Verify.isNumber(value)) {
      def = this.allowedValues[value]
    }
    return super.initTo(def)
  }

  static isMember(allowedValues, value) {
    return allowedValues.some((allowedValue)=> {
      return allowedValue === value
    })
  }
}


let EntityForge = {
  any() {
    return new BaseForge()
  },
  string: StringForge.string,
  number: NumberForge.number,
  int: NumberForge.int,
  bool: BooleanForge.bool,
  enum: EnumForge.enum,
  obj: ObjectForge.obj
}

export {EntityForge, ValidationError}


