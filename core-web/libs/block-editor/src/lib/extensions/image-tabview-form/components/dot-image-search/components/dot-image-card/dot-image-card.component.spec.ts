import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageCardComponent } from './dot-image-card.component';

describe('DotImageCardComponent', () => {
    let component: DotImageCardComponent;
    let fixture: ComponentFixture<DotImageCardComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageCardComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotImageCardComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
