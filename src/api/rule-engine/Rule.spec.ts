/// <reference path="../../../jspm_packages/npm/angular2@2.0.0-alpha.44/typings/jasmine/jasmine.d.ts" />



import {Check} from '/api/validation/Check';
import {EntityMeta, EntitySnapshot} from "api/persistence/EntityBase";
import {RuleService, RuleModel} from 'api/rule-engine/Rule';
import {ApiRoot} from "api/persistence/ApiRoot";
import {UserModel} from "api/auth/UserModel";



describe('Unit.api.rule-engine.Rule', function () {


  beforeEach(function () {


  });

  it("Isn't valid when new.", function(){

    var foo = new RuleModel()
    expect(foo.valid).toEqual(false);

  })



});

