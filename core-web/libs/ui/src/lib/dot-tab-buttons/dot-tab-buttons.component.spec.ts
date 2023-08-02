import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTabButtonsComponent } from './dot-tab-buttons.component';

describe('DotTabButtonsComponent', () => {
    let component: DotTabButtonsComponent;
    let fixture: ComponentFixture<DotTabButtonsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotTabButtonsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTabButtonsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
