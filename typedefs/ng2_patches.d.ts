// Type definitions that the Angular team seems to have missed, or IntelliJ/WebStorm doesn't recognize.


declare module form {
  class ControlGroup {

  }
  class FormBuilder {
    group(cfg:any):ControlGroup;
  }

}

declare module FormBuilder {
  function group(cfg:any)
}
