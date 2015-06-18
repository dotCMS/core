import Dispatcher from 'flux';

declare class AppDispatcher extends Dispatcher.Dispatcher {  }

declare module Core {
  declare module Collections {
    declare function asArray(any, Function?)
  }

}
declare module RuleEngine {
    declare module ruleRepo {
      declare function push(rule:any)
  }
}