import  {LazyVerify, Verify} from './Verify.js'
import  {ValidationError} from './Validation.js'

class EntityBase {

  constructor() {
  }



  toJson(validate=true){
    if(validate !== false){
      this.validate()
    }
    return JSON.stringify(this)
  }

}

export {EntityBase}


