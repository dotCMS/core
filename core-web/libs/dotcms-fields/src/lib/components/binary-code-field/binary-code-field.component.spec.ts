import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BinaryCodeFieldComponent } from './binary-code-field.component';

describe('BinaryCodeFieldComponent', () => {
    let component: BinaryCodeFieldComponent;
    let fixture: ComponentFixture<BinaryCodeFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BinaryCodeFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(BinaryCodeFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
