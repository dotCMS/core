/// <reference path="../../../jspm_packages/npm/angular2@2.0.0-alpha.44/typings/jasmine/jasmine.d.ts" />

import {RuleModel, RuleService} from 'api/rule-engine/Rule';
import {ActionModel} from 'api/rule-engine/Action';




describe('Unit.api.rule-engine.Action', function () {


  beforeEach(function () {


  });

  it("Isn't valid when new.", function(){

    var foo = new ActionModel()
    expect(foo.valid).toEqual(false);

  })



});