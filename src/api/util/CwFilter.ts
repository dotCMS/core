
import {Verify} from "../validation/Verify";


const numberRegex = /[\d.]/
export class CwFilter {
  static isFiltered(object:any, fieldName:string, fieldValue:any){


  }

  static transformValue(fieldValue:any):any{
    let xform = fieldValue
    if(Verify.exists(fieldValue)){
      if(fieldValue === "true"){
        xform = true
      } else if(fieldValue === "false"){
        xform = false
      } else if(fieldValue.match(numberRegex)){
        xform = Number.parseFloat(fieldValue)
      }
    }
    return xform;
  }
}