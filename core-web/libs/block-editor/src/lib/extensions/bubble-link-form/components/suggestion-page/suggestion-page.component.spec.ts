import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuggestionPageComponent } from './suggestion-page.component';

describe('SuggestionPageComponent', () => {
    let component: SuggestionPageComponent;
    let fixture: ComponentFixture<SuggestionPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SuggestionPageComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(SuggestionPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
