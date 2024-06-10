import { ActionModel } from './Rule';
import { ServerSideTypeModel } from './ServerSideFieldModel';

describe('Unit.api.rule-engine.Action', () => {
    it("Isn't valid when no rule.", () => {
        const foo = new ActionModel(null, new ServerSideTypeModel(), null);
        expect(foo.isValid()).toEqual(false);
    });
});
