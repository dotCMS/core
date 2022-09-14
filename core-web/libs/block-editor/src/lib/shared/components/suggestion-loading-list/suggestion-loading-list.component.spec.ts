import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuggestionLoadingListComponent } from './suggestion-loading-list.component';

describe('SuggestionSkeletonComponent', () => {
    let component: SuggestionLoadingListComponent;
    let fixture: ComponentFixture<SuggestionLoadingListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SuggestionLoadingListComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(SuggestionLoadingListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
