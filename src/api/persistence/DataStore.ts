import {Injectable} from 'angular2/core';
import  {Check} from '../validation/Check'

@Injectable()
export class DataStore {

  get length():number {
    return 0
  }
  checkPath(path:string):string{
    Check.notEmpty(path, "Path cannot be blank.")
    Check.isString(path, "Path must be a string.")
    if(!path.startsWith("http")){
      throw new Error("Path must be a valid URL")
    }
    return path
  }
  path(idx:string):any { }
  clear():void {  }
  setItem(path:string, value:any, isNew:boolean=false) { }
  getItem(path:string):any {   }
  getItems(...paths):Array<any>{ return [] }
  hasItem(path:string):boolean { return false }
  removeItem(path:string):any { }
  childPaths(path:string):Array<string> {return [] }
  childKeys(path:string):Array<string>{ return []}
  childItems(path:string):any {  }
  setAuth(x:any=null, y:any=null){}

}