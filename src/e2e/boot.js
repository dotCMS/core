"use strict";

console.log("boot.js")
var initSpecFns = protractor.__hack ? protractor.__hack : "dang"

let b = browser
if (initSpecFns && initSpecFns.length) {
  console.log("Loading " + initSpecFns.length + " specs")
  for(var i = 0, L = initSpecFns.length; i < L; i++){
    try{
      this.fn = initSpecFns[i].initSpec
      describe("The Rules Engine", this.fn(b))
    }catch(e){
      console.log("Error loading spec.", e)
    }
  }
}