import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ActionModel} from '../../api/rule-engine/Action';
import {ServerSideTypeModel} from "./ServerSideFieldModel";


describe('Unit.api.rule-engine.Action', function () {

  beforeEach(function () {
  });

  it("Isn't valid when no rule.", function(){
    var foo = new ActionModel(null, new ServerSideTypeModel(), null)
    expect(foo.isValid()).toEqual(false);
  })

});