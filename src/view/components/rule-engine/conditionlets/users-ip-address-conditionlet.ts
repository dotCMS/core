
/// <reference path="../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />
 
 import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

 import {CwCidrInput} from '../../input/cidr/cidr';
 import {CwIpAddressInput} from '../../input/ip-address/ip-address';

 @Component({
  selector: 'conditionlet users-ip-address-conditionlet'
 })
 @View({
   directives: [NgFor, NgIf, CwCidrInput, CwIpAddressInput],
   template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.condition.comparison" (change)="setComparison($event)">
        <option [selected]="x.id == conditionletDir.condition.comparison" value="{{x.id}}" *ng-for="var x of conditionletDir.conditionlet.comparisons">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <cw-ip-address-input *ng-if="conditionletDir.condition.comparison == 'is' || conditionletDir.condition.comparison == 'is_not' " [value]="conditionletDir.value" > </cw-ip-address-input>
      <cw-cidr-input *ng-if="conditionletDir.condition.comparison == 'netmask'" [value]="conditionletDir.value"></cw-cidr-input>


    </div>
  `
 })
 export class UsersIpAddressConditionlet {
 
   constructor( @Attribute('id') id:string) {

   }
 }
