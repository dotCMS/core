import Dispatcher from 'flux';

declare class AppDispatcher extends Dispatcher.Dispatcher {  }

declare module Core {
  declare module Collections {
    declare function asArray(any, Function?)
  }

}
declare module RuleEngine {
    declare module ruleRepo {
      declare function push(rule:any):Promise
      declare function set(rule:any):Promise
      declare function remove(rule:any):Promise
  }
}

declare class EntityMeta {
  once(eventType:string, callback:Function):Promise;
}