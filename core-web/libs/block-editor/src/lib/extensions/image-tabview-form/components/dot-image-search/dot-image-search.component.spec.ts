import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageSearchComponent } from './dot-image-search.component';

describe('DotImageSearchComponent', () => {
    let component: DotImageSearchComponent;
    let fixture: ComponentFixture<DotImageSearchComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageSearchComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotImageSearchComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
