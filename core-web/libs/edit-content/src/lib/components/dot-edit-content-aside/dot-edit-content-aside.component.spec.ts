import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';

import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { DotFormatDateService } from '@dotcms/ui';

import { DotEditContentAsideComponent } from './dot-edit-content-aside.component';

import { CONTENT_FORM_DATA_MOCK } from '../../utils/mocks';

describe('DotEditContentAsideComponent', () => {
    let spectator: Spectator<DotEditContentAsideComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentAsideComponent,
        detectChanges: false,
        providers: [
            mockProvider(ActivatedRoute), // Needed, use RouterLink
            {
                provide: DotMessageService,
                useValue: {
                    get() {
                        return 'Sample';
                    }
                }
            },
            {
                provide: DotFormatDateService,
                useValue: {
                    differenceInCalendarDays: () => 10,
                    format: () => '11/07/2023',
                    getUTC: () => new Date()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render aside info', () => {
        spectator.setInput('contentLet', CONTENT_FORM_DATA_MOCK.contentlet);
        spectator.setInput('contentType', CONTENT_FORM_DATA_MOCK.contentType);
        spectator.detectChanges();
        expect(spectator.query(byTestId('modified-by')).textContent.trim()).toBe('Admin User');
        expect(spectator.query(byTestId('last-modified')).textContent.trim()).toBe('11/07/2023');
        expect(spectator.query(byTestId('inode')).textContent.trim()).toBe(
            CONTENT_FORM_DATA_MOCK.contentlet.inode.slice(0, 8)
        );
    });

    it('should not render aside info', () => {
        const CONTENT_WITHOUT_CONTENTLET = CONTENT_FORM_DATA_MOCK;
        delete CONTENT_WITHOUT_CONTENTLET.contentlet;
        spectator.setInput('contentLet', CONTENT_WITHOUT_CONTENTLET.contentlet);
        spectator.setInput('contentType', CONTENT_WITHOUT_CONTENTLET.contentType);
        spectator.detectChanges();

        expect(spectator.query(byTestId('modified-by')).textContent).toBe('');
        expect(spectator.query(byTestId('last-modified')).textContent).toBe('');
        expect(spectator.query(byTestId('inode'))).toBeFalsy();
    });
});
