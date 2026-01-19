import { ApiRoot } from '@dotcms/dotcms-js';
import { UserModel } from '@dotcms/dotcms-js';

import { RuleModel } from './Rule';

describe('Unit.api.rule-engine.Rule', () => {
    beforeEach(() => {});

    it("Isn't valid when new.", () => {
        const foo = new RuleModel({});
        expect(foo.isValid()).toEqual(false);
    });
});
