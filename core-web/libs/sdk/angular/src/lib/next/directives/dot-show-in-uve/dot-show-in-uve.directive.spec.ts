import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { UVE_MODE } from '@dotcms/uve/types';

import { DotShowInUVEDirective } from './dot-show-in-uve';

describe('DotShowInUVEDirective', () => {
    let spectator: SpectatorDirective<DotShowInUVEDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotShowInUVEDirective,
        template: `<div dotCMSShowInUVE [when]="${UVE_MODE.PREVIEW}"></div>`
    });

    beforeEach(() => {
        spectator = createDirective();
    });

    it('should initialize the directive', () => {
        expect(spectator.directive).toBeDefined();
    });
});
