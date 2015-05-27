// Type definitions for ES6 methods, specifically written for IntelliJ/WebStorm code inspection.
// Which means that the definitions might not actually be *correct*, but they do suppress the false errors.

declare module Object {
    function assign(to:any, ...args:any[]): boolean;
}

declare interface String {
  startsWith(string):string
}

declare interface Promise {
  all(Array)
  catch(any)
}
declare module Map {
  function values():Array<any>
}

declare module Array {
  function find(fn);
  function from(any);
}

declare class Promise {
  constructor(fn:Function);

}