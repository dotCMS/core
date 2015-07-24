//entity-forge.d.ts
declare var ConnectionManager: any;

declare class EntityMeta {
  constructor(path:string);
  once(eventType:string, cb?:Function):Promise<any>;
  on(eventType:string, cb?:Function):Promise<any>;
  push(data:any):Promise<any>;
}

declare class EntitySnapshot {
  key():string;
  push(data:any):Promise<any>;
  forEach(callback:Function);
  ref():EntityMeta;
}

declare var RestDataStore:any;



