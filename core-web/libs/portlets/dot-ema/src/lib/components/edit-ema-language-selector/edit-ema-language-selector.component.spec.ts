import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmaLanguageSelectorComponent } from './edit-ema-language-selector.component';

describe('DotEmaLanguageSelectorComponent', () => {
    let component: EmaLanguageSelectorComponent;
    let fixture: ComponentFixture<EmaLanguageSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EmaLanguageSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EmaLanguageSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    // it('should update store and update the route on page change', () => {
    //     const router = spectator.inject(Router);

    //     jest.spyOn(store, 'setLanguage');
    //     jest.spyOn(router, 'navigate');

    //     spectator.detectChanges();

    //     spectator.triggerEventHandler('select[data-testId="language_id"]', 'change', {
    //         target: { name: 'language_id', value: '2' }
    //     });

    //     expect(store.setLanguage).toHaveBeenCalledWith('2');
    //     expect(router.navigate).toHaveBeenCalledWith([], {
    //         queryParams: { language_id: '2' },
    //         queryParamsHandling: 'merge'
    //     });

    //     const iframe = spectator.query(byTestId('iframe'));
    //     expect(iframe.getAttribute('src')).toBe('http://localhost:3000/page-one?language_id=2');
    // });
});
