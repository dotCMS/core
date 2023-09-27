import { expect, it } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotMessageService } from '@dotcms/data-access';

import { DotBinaryFieldUrlModeComponent } from './dot-binary-field-url-mode.component';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../../../utils/mock';

describe('DotBinaryFieldUrlModeComponent', () => {
    let spectator: Spectator<DotBinaryFieldUrlModeComponent>;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldUrlModeComponent,
        imports: [],
        providers: [
            {
                provide: DotMessageService,
                useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have a form with url field', () => {
        expect(spectator.query(byTestId('url-input'))).not.toBeNull();
    });
});
