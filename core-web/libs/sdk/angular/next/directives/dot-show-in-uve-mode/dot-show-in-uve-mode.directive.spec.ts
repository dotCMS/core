import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { UVE_MODE } from '@dotcms/uve/types';

import { DotShowInUVEModeDirective } from './dot-show-in-uve-mode.directive';

describe('DotShowInUVEModeDirective', () => {
    let spectator: SpectatorDirective<DotShowInUVEModeDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotShowInUVEModeDirective,
        template: `<div *dotCMSShowInUVEMode="${UVE_MODE.PREVIEW}"></div>`
    });

    beforeEach(() => {
        spectator = createDirective();
    });

    it('should initialize the directive', () => {
        expect(spectator.directive).toBeDefined();
    });
});
