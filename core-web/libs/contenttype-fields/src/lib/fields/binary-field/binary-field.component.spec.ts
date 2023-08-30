import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BinaryFieldComponent } from './binary-field.component';

describe('BinaryFieldComponent', () => {
    let component: BinaryFieldComponent;
    let fixture: ComponentFixture<BinaryFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [BinaryFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(BinaryFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
