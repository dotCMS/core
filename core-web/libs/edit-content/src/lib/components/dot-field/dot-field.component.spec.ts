import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotFieldComponent } from './dot-field.component';

describe('DotFieldComponent', () => {
    let component: DotFieldComponent;
    let fixture: ComponentFixture<DotFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
