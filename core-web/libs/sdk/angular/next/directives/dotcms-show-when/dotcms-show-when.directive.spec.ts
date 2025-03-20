import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { UVE_MODE } from '@dotcms/uve/types';

import { DotCMSShowWhenDirective } from './dotcms-show-when.directive';

describe('DotCMSShowWhenDirective', () => {
    let spectator: SpectatorDirective<DotCMSShowWhenDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotCMSShowWhenDirective,
        template: `<div *dotCMSShowWhen="${UVE_MODE.PREVIEW}"></div>`
    });

    beforeEach(() => {
        spectator = createDirective();
    });

    it('should initialize the directive', () => {
        expect(spectator.directive).toBeDefined();
    });
});
