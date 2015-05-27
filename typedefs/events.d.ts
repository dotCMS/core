// Type definitions for events.EventEmitter
// Definitions by: ggranum <https://github.com/ggranum>


declare module events {

  class EventEmitter {

    emit(key:any, ...args:any[]): boolean;
    on(key:any, callback:Function): boolean;
    removeListener(key:any, callback:Function): boolean;
    setMaxListeners(count:number): boolean;
  }


}
