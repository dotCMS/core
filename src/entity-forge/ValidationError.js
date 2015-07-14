

class ValidationError extends Error{

  constructor(validationResult, target) {
    super()
    this.target = target || '{unknown}';
    this.messages = validationResult.errors.map((error) => {
      return error.fieldName + ': ' + error.validator.msg
    })
    this.message = "One or more failures occurred while validating '" + this.target + "': [["
    this.message += this.messages.join("],     [") + "]]"
  }
}

export {ValidationError}