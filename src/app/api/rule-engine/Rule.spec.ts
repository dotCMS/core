
import {Check} from '../../api/validation/Check';
import {RuleService, RuleModel} from '../../api/rule-engine/Rule';
import {ApiRoot} from "../../api/persistence/ApiRoot";
import {UserModel} from "../../api/auth/UserModel";



describe('Unit.api.rule-engine.Rule', function () {


  beforeEach(function () {
  });

  it("Isn't valid when new.", function(){

    var foo = new RuleModel(null)
    expect(foo.isValid()).toEqual(false);

  })



});

