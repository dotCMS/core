
import {Verify} from "../validation/Verify";

export class DevUtil {
  static objToProperties(rsrcTree, base){
    Object.keys(rsrcTree).forEach((key)=>{
      let subtreeOrValue = rsrcTree[key];
      if(!Verify.isString(subtreeOrValue)){
        DevUtil.objToProperties(subtreeOrValue, base + '.' + key )
      } else {
        console.log(base + '.' + key + '=' + subtreeOrValue )
      }
    })

  }
}