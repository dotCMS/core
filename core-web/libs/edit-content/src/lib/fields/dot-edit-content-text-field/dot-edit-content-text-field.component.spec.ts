import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';

describe('DotEditContentTextFieldComponent', () => {
    let component: DotEditContentTextFieldComponent;
    let fixture: ComponentFixture<DotEditContentTextFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentTextFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentTextFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
