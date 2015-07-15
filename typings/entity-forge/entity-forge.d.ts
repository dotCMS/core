//entity-forge.d.ts
declare var ConnectionManager: any;

declare class EntityMeta {
  constructor(path:string);
  once(eventType:string, cb:Function = null):Promise;
  push(data:any):Promise;
}

declare var RestDataStore:any;



