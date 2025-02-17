import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotUveErrorComponent } from './dot-uve-error.component';

import { UVEStore } from '../../../store/dot-uve.store';

describe('DotUveErrorComponent', () => {
    let spectator: Spectator<DotUveErrorComponent>;

    const createComponent = createComponentFactory({
        component: DotUveErrorComponent,
        providers: [
            {
                provide: UVEStore,
                useValue: {
                    errorCode: signal(404)
                }
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

        expect(icon).toBeTruthy();
        expect(title).toBeTruthy();
        expect(description).toBeTruthy();
    });
});
