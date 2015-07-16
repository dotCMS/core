//entity-forge.d.ts
declare var ConnectionManager: any;

declare class EntityMeta {
  constructor(path:string);
  once(eventType:string, cb?:Function):Promise<any>;
  push(data:any):Promise<any>;
}

declare var RestDataStore:any;



