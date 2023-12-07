import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotMessageService } from '@dotcms/data-access';
import { DotFormatDateService } from '@dotcms/ui';
import { DotFormatDateServiceMock } from '@dotcms/utils-testing';

import { DotEditContentAsideComponent } from './dot-edit-content-aside.component';

import { CONTENT_FORM_DATA_MOCK } from '../../utils/mocks';

describe('DotEditContentAsideComponent', () => {
    let spectator: Spectator<DotEditContentAsideComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentAsideComponent,
        detectChanges: false,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get() {
                        return 'Sample';
                    }
                }
            },
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render aside info', () => {
        spectator.setInput('asideData', CONTENT_FORM_DATA_MOCK);
        spectator.detectChanges();
        expect(spectator.query(byTestId('modified-by'))).toBeTruthy();
        expect(spectator.query(byTestId('last-modified'))).toBeTruthy();
        expect(spectator.query(byTestId('inode'))).toBeTruthy();
    });

    it('should not render aside info', () => {
        const CONTENT_WITHOUT_CONTENTLET = CONTENT_FORM_DATA_MOCK;
        delete CONTENT_WITHOUT_CONTENTLET.contentlet;
        spectator.setInput('asideData', CONTENT_WITHOUT_CONTENTLET);
        spectator.detectChanges();

        expect(spectator.query(byTestId('modified-by'))).toBeFalsy();
        expect(spectator.query(byTestId('last-modified'))).toBeFalsy();
        expect(spectator.query(byTestId('inode'))).toBeFalsy();
    });
});
