import { Spectator, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotEmaComponent } from './dot-ema.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';

describe('DotEmaComponent', () => {
    let spectator: Spectator<DotEmaComponent>;

    const createComponent = createRoutingFactory({
        component: DotEmaComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
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
                    }
                }
            }
        ]
    });

    beforeEach(
        () =>
            (spectator = createComponent({
                queryParams: { language_id: '1', url: 'page-one' }
            }))
    );

    it('should initialize with route query parameters', () => {
        const mockQueryParams = { language_id: '1', url: 'page-one' };

        const store = spectator.inject(EditEmaStore, true);

        jest.spyOn(store, 'load');

        spectator.detectChanges();

        expect(store.load).toHaveBeenCalledWith(mockQueryParams);
    });

    it('should update store and update the route on page change', () => {
        const store = spectator.inject(EditEmaStore, true);
        const router = spectator.inject(Router);

        jest.spyOn(store, 'setLanguage');
        jest.spyOn(router, 'navigate');

        spectator.detectChanges();

        spectator.triggerEventHandler('select[data-testId="language_id"]', 'change', {
            target: { name: 'language_id', value: '2' }
        });

        expect(store.setLanguage).toHaveBeenCalledWith('2');
        expect(router.navigate).toHaveBeenCalledWith([], {
            queryParams: { language_id: '2' },
            queryParamsHandling: 'merge'
        });

        const iframe = spectator.query(byTestId('iframe'));
        expect(iframe).toHaveAttribute('src', 'http://localhost:3000/page-one?language_id=2');
    });
});
