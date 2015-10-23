export class CwEvent {
  key:string
  target:any

  constructor(key:string, target:any) {
    this.key = key;
    this.target = target;
  }
}


export class AddEvent extends CwEvent {

  constructor(key:string, target:any) {
    super(key, target);
  }
}