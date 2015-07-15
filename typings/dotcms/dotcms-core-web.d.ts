declare module RuleEngine {
  class Rule{
    name:string;
    enabled:boolean;
    priority:number;
    fireOn:string;
    shortCircuit:boolean;
    conditionGroups:any;
    actions:any;

  }
}