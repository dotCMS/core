import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveBookmarkComponent } from './dot-uve-bookmark.component';

describe('DotUveBookmarkComponent', () => {
    let component: DotUveBookmarkComponent;
    let fixture: ComponentFixture<DotUveBookmarkComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveBookmarkComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveBookmarkComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
