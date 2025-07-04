import { describe, it, expect } from '@jest/globals';
import { byTestId, createDirectiveFactory } from '@ngneat/spectator/jest';

import { UVE_MODE, UVEState } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { DotCMSShowWhenDirective } from './dotcms-show-when.directive';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('DotCMSShowWhenDirective', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    const createDirective = createDirectiveFactory({
        directive: DotCMSShowWhenDirective
    });

    const createDirectiveWithMode = (when: UVE_MODE, uveState?: Partial<UVEState>) => {
        getUVEStateMock.mockReturnValue(uveState);

        // Remember that in the template it needs the '' otherwise you will pass undefined
        return createDirective(
            `<div *dotCMSShowWhen="'${when}'"  data-testid="test-element"></div>`
        );
    };

    describe('Edit Mode', () => {
        it('should show the element when the UVE is in edit mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.EDIT, {
                mode: UVE_MODE.EDIT
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeTruthy();
        });

        it('should hide the element when the UVE is in live mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.EDIT, {
                mode: UVE_MODE.LIVE
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when the UVE is in preview mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.EDIT, {
                mode: UVE_MODE.PREVIEW
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when its outside of the UVE', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.EDIT);

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });
    });

    describe('Live Mode', () => {
        it('should show the element when the UVE is in live mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.LIVE, {
                mode: UVE_MODE.LIVE
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeTruthy();
        });

        it('should hide the element when the UVE is in edit mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.LIVE, {
                mode: UVE_MODE.EDIT
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when the UVE is in preview mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.LIVE, {
                mode: UVE_MODE.PREVIEW
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when its outside of the UVE', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.LIVE);

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });
    });

    describe('Preview Mode', () => {
        it('should show the element when the UVE is in preview mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.PREVIEW, {
                mode: UVE_MODE.PREVIEW
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeTruthy();
        });

        it('should hide the element when the UVE is in edit mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.PREVIEW, {
                mode: UVE_MODE.EDIT
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when the UVE is in live mode', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.PREVIEW, {
                mode: UVE_MODE.LIVE
            });

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });

        it('should hide the element when its outside of the UVE', () => {
            const spectator = createDirectiveWithMode(UVE_MODE.PREVIEW);

            const element = spectator.query(byTestId('test-element'));

            expect(element).toBeFalsy();
        });
    });
});
