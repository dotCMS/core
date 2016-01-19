import {Injectable} from "angular2/core";
import {Verify} from './Verify'
import {Control} from 'angular2/common'
import {I18nService} from "../system/locale/I18n";

@Injectable()
export class CwValidators {


  constructor(private resources:I18nService) {
  }

  required(errorKey):Function {

    return function (control) {
      let err = null
      if (Verify.minLength(control.value, 1)) {
        err = {
          required: {
            message$: this.resources.get(errorKey, errorKey )
          }
        }
      }
      return err
    }

  }

}