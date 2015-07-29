 /// <reference path=../../../../typings/es6/lib.es6.d.ts />
 /// <reference path=../../../../typings/angular2/angular2.d.ts />
 
 import {Directive, LifecycleEvent, Attribute, Ancestor, ObservableWrapper, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';
 import {ConditionletDirective, BaseConditionletComponent} from './conditionlet-base';
 
 @Component({
  selector: 'conditionlet users-log-in-conditionlet'
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
 export class UsersLogInConditionlet extends BaseConditionletComponent{
 
   constructor(@Ancestor() conditionletDir:ConditionletDirective, @Attribute('id') id:string) {
     super(conditionletDir, id)
   }
 
 }
