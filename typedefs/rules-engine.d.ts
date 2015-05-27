import Dispatcher from 'flux';

declare class AppDispatcher extends Dispatcher.Dispatcher {  }

declare module Core {
  declare module Collections {
    declare function asArray(any, Function?)
  }

}
declare module RuleEngine {
  declare module api {
    declare interface ruleRepo {
      push(rule:any)

    }

  }
}