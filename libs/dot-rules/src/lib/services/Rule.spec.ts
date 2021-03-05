import { RuleService, RuleModel } from './Rule';
import { ApiRoot } from '@dotcms/dotcms-js';
import { UserModel } from '@dotcms/dotcms-js';

describe('Unit.api.rule-engine.Rule', () => {
    beforeEach(() => {});

    it("Isn't valid when new.", () => {
        const foo = new RuleModel({});
        expect(foo.isValid()).toEqual(false);
    });
});
