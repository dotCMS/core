import * as Rx from 'rxjs/Rx'
import {Injector, Provider} from 'angular2/core';
import {DataStore} from '../../api/persistence/DataStore'
import {RestDataStore} from '../../api/persistence/RestDataStore';
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';
import {I18nService} from "../system/locale/I18n";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {HTTP_PROVIDERS} from "angular2/http";

var injector = Injector.resolveAndCreate([
  ApiRoot,
  I18nService,
  UserModel,
  ConditionTypeService,
  HTTP_PROVIDERS,
  new Provider(DataStore, {useClass: RestDataStore})
])

describe('Integration.api.rule-engine.RuleService', function () {

  var typeService:ConditionTypeService
  var subscriptions:Array<Rx.Subscription<ServerSideTypeModel>>

  beforeEach(function () {
    subscriptions = []
    typeService = injector.get(ConditionTypeService)

  });

  afterEach(function(){
    subscriptions.forEach((sub:Rx.Subscription<ServerSideTypeModel>)=>{
      sub.unsubscribe()
    })
  })

  it("Can list condition types, and they are all persisted and valid.", function(done){
    let count = 0;
    let subscription = typeService.list().subscribe((types:ServerSideTypeModel[]) => {
      types.forEach((type:ServerSideTypeModel)=>{
        expect(type.isPersisted()).toBe(true, "Condition types are readonly and should always be persisted.")
        expect(type.isValid()).toBe(true, "Condition types are readonly and should always be valid.")
      })
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown creating Rule.")
      done()
    }, ()=>{
    })
    subscriptions.push(subscription) // for cleanup.
  })

  it("There are (n) active condition types.", function(done){
    typeService.list().subscribe((types:ServerSideTypeModel[])=>{
      expect(types.length).toEqual(12, "We have 12 implemented condition types.")
      done()
    })
  })

});
