
/// <reference path="../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />
 
 import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

 
 @Component({
  selector: 'conditionlet users-host-conditionlet'
 })
 @View({
   directives: [NgFor],
   template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.condition.comparison" (change)="setComparison($event)">
        <option value="{{x.id}}" *ng-for="var x of conditionletDir.conditionlet.comparisons">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <input type="text" class="form-control condition-value" [value]="conditionletDir.value" (input)="setValue($event)"/>
    </div>
  `
 })
 export class UsersHostConditionlet {
 
   constructor( @Attribute('id') id:string) {

   }
 
 }
