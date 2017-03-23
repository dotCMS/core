import {ServerSideTypeModel} from './ServerSideFieldModel';
import {ActionModel} from './Rule';


describe('Unit.api.rule-engine.Action', function () {

  beforeEach(function () {
  });

  it("Isn't valid when no rule.", function(){
    var foo = new ActionModel(null, new ServerSideTypeModel(), null)
    expect(foo.isValid()).toEqual(false);
  })

});