declare module RuleEngine {
  var templates:any;
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

declare module Logger {
  function getLogger(prefix:string);
}