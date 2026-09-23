import { ActionModel } from '../rule/Rule';
import { ServerSideTypeModel } from '../serverside-field/ServerSideFieldModel';

describe('Unit.api.rule-engine.Action', () => {
    it("Isn't valid when no rule.", () => {
        // Priority omitted rather than passed as null: the constructor's `priority || 1` made
        // the two identical, and it is not what this test is about.
        const foo = new ActionModel(null, new ServerSideTypeModel());
        expect(foo.isValid()).toEqual(false);
    });
});
