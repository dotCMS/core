import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ActionModel} from '../../api/rule-engine/Action';
import {ActionTypeModel} from "./ActionType";

describe('Unit.api.rule-engine.Action', function () {

  beforeEach(function () {
  });

  it("Isn't valid when new.", function(){
    var foo = new ActionModel(null, new ActionTypeModel())
    expect(foo.isValid()).toEqual(false);
  })

});