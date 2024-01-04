import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEmaBookmarksComponent } from './dot-ema-bookmarks.component';

describe('DotEmaBookmarksComponent', () => {
    let component: DotEmaBookmarksComponent;
    let fixture: ComponentFixture<DotEmaBookmarksComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEmaBookmarksComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEmaBookmarksComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
