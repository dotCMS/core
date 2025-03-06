import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUvePageVersionNotFoundComponent } from './dot-uve-page-version-not-found.component';

import { UVEStore } from '../../../store/dot-uve.store';

const messagesMock = {
    'uve.editor.error.404.title': 'No Live Version Available',
    'uve.editor.error.404.description':
        "There's no live version of this content available for the selected date but you can explore future releases in the calendar. Navigate through the calendar to see what's schedules next."
};

describe('DotUveErrorComponent', () => {
    let spectator: Spectator<DotUvePageVersionNotFoundComponent>;

    const createComponent = createComponentFactory({
        component: DotUvePageVersionNotFoundComponent,
        providers: [
            {
                provide: UVEStore,
                useValue: {
                    errorCode: signal(404)
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the error icon, title and description', () => {
        const icon = spectator.query('[data-testId="icon"]');
        const title = spectator.query('[data-testId="title"]');
        const description = spectator.query('[data-testId="description"]');

        expect(icon).toBeDefined();
        expect(title).toContainText('No Live Version Available');
        expect(description).toContainText(
            "There's no live version of this content available for the selected date but you can explore future releases in the calendar. Navigate through the calendar to see what's schedules next."
        );
    });
});
