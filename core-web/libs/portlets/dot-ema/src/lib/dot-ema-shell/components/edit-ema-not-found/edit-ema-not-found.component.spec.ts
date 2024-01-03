import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { ActivatedRoute, RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { EditEmaNotFoundComponent } from './edit-ema-not-found.component';

describe('EditEmaNotFoundComponent', () => {
    let spectator: Spectator<EditEmaNotFoundComponent>;

    const createComponent = createComponentFactory({
        component: EditEmaNotFoundComponent,
        imports: [ButtonModule, RouterLink],
        providers: [{ provide: ActivatedRoute, useValue: {} }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should button have routerLink to page', () => {
        expect(spectator.query(byTestId('button-go-to-page'))).toHaveAttribute(
            'routerLink',
            '/pages'
        );
    });
});
