import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageCardListComponent } from './dot-image-card-list.component';

describe('DotImageCardListComponent', () => {
    let component: DotImageCardListComponent;
    let fixture: ComponentFixture<DotImageCardListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageCardListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotImageCardListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
