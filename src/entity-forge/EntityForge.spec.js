import {EntityForge as EF} from 'src/entity-forge/EntityForge.js';

describe('EntityForge.string', function () {
  beforeEach(function () {
  });

  it('Accepts null by default', function () {
    let model = {
      foo: EF.string()
    }
    let result = model.foo.validate(null)
    expect(result).toBeDefined()
    expect(result.valid).toBe(true)
  });

  it('is valid when value is valid', function () {
    let validValues = [
      null,
      "null",
      "undefined",
      "",
      "\n",
      "abc",
      "123",
      "Sort of a long value to test, but not really all that long. \n Etc."
    ]
    let model = {
      foo: EF.string()
    }
    validValues.map((value)=>{
      expect(model.foo.validate(value).valid).toBe(true)
    })
  });

  it('is valid when minLength set and value is valid', function () {
    let model = {
      foo: EF.string().minLength(4)
    }
    let result = model.foo.validate("abcdef")
    expect(result.valid).toBe(true)
  });

  it('is valid when maxLength set and value is valid', function () {
    let model = {
      foo: EF.string().maxLength(10)
    }
    let result = model.foo.validate("abcdef")
    expect(result.valid).toBe(true)
  });

  it('is valid when minLength and maxLength are set and value is valid', function () {
    let model = {
      foo: EF.string().minLength(5).maxLength(10)
    }
    let result = model.foo.validate("12345")
    expect(result.valid).toBe(true)
  });

  it('minLength is inclusive', function () {
    let model = {
      foo: EF.string().minLength(5)
    }
    expect(model.foo.validate("12345").valid).toBe(true)
    expect(model.foo.validate("1234").valid).toBe(false)
  });

  it('maxLength is inclusive', function () {
    let model = {
      foo: EF.string().maxLength(5)
    }
    expect(model.foo.validate("12345").valid).toBe(true)
    expect(model.foo.validate("123456").valid).toBe(false)
  });

  it('handles minLength == maxLength', function () {
    let model = {
      foo: EF.string().minLength(5).maxLength(5)
    }
    expect(model.foo.validate("12345").valid).toBe(true)
    expect(model.foo.validate("123456").valid).toBe(false)
    expect(model.foo.validate("1234").valid).toBe(false)
  });


  it('provides an empty error array even when valid', function () {
    let model = {
      foo: EF.string()
    }
    let result = model.foo.validate(null)
    expect(result.valid).toBe(true)
    expect(result.errors).toBeDefined()
    expect(result.errors.length).toBe(0)
  });

  it('Provides error when value is a number.', function () {
    let model = {
      foo: EF.string()
    }
    let result = model.foo.validate(100)
    expect(result.valid).toBe(false)
  });

  it('Provides error when value is an object.', function () {
    let model = {
      foo: EF.string()
    }
    let result = model.foo.validate({"asdfasdf": "Asdfsd"})
    expect(result.valid).toBe(false)
  });


  it('Provides error when value is a function.', function () {
    let model = {
      foo: EF.string()
    }
    let result = model.foo.validate(()=> { return "a string";})
    expect(result.valid).toBe(false)
  });

  it('Provides error when minLength set and value is null.', function () {
    let model = {
      foo: EF.string().minLength(4)
    }
    let result = model.foo.validate(null)
    expect(result.valid).toBe(false)
    expect(result.toString()).toMatch(/@validations.exists.doesNotExist/)

  });

  it('is invalid when minLength set and value is a string and is too short', function () {
    let model = {
      foo: EF.string().minLength(4)
    }
    let result = model.foo.validate("abc")
    expect(result.valid).toBe(false)
    expect(result.toString()).toMatch(/@validations.string.minLength.tooShort/)
  });


})