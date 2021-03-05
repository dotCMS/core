import { ServerSideTypeModel } from './ServerSideFieldModel';
import { ActionModel } from './Rule';

describe('Unit.api.rule-engine.Action', () => {
    it("Isn't valid when no rule.", () => {
        let foo = new ActionModel(null, new ServerSideTypeModel(), null);
        expect(foo.isValid()).toEqual(false);
    });
});
