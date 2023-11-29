import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator';
import { of } from 'rxjs';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

import { EditEmaStore } from '../../feature/store/dot-ema.store';
import { DotPageApiService } from '../../services/dot-page-api.service';

describe('EditEmaToolbarComponent', () => {
    let spectator: Spectator<EditEmaToolbarComponent>;

    const createComponent = createComponentFactory({
        component: EditEmaToolbarComponent,
        providers: [
            EditEmaStore,
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world'
                            }
                        });
                    },
                    save() {
                        return of({});
                    }
                }
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                pageTitle: 'Test Page'
            }
        });
    });

    describe('DOM', () => {
        it('should have a heading with the page title', () => {
            expect(spectator.query(byTestId('page-title')).textContent).toBe('Test Page');
        });
    });
});
