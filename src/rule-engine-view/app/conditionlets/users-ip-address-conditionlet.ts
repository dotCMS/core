 /// <reference path="../../../../typings/es6/lib.es6.d.ts" />
 /// <reference path="../../../../typings/angular2/angular2.d.ts" />
 
 import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';
 import {ConditionletDirective, BaseConditionletComponent} from './conditionlet-base';
 import {CIDRInputContainer, CwCidrInput} from '../../../view/components/input/cidr/cidr';
 import {CwIpAddressInputContainer, CwIpAddressInput} from '../../../view/components/input/ip-address/ip-address';

 @Component({
  selector: 'conditionlet users-ip-address-conditionlet'
 })
 @View({
   directives: [NgFor, NgIf,  CwIpAddressInputContainer, CIDRInputContainer, CwCidrInput, CwIpAddressInput],
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
      <ipaddress *ng-if="conditionletDir.condition.comparison == 'is'">
        <input type="text" class="form-control condition-value" [value]="conditionletDir.value" (input)="setValue($event)"/>
      </ipaddress>
      <cidr *ng-if="conditionletDir.condition.comparison == 'netmask'">
        <input type="text" class="form-control condition-value" [value]="conditionletDir.value" (input)="setValue($event)"/>
      </cidr>

    </div>
  `
 })
 export class UsersIpAddressConditionlet extends BaseConditionletComponent{
 
   constructor(@SkipSelf() @Host() conditionletDir:ConditionletDirective, @Attribute('id') id:string) {
     super(conditionletDir, id)
   }
 }
