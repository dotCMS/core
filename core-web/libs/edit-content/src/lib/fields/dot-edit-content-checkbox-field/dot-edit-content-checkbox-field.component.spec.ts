import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field.component';

describe('DotEditContentCheckboxFieldComponent', () => {
    let component: DotEditContentCheckboxFieldComponent;
    let fixture: ComponentFixture<DotEditContentCheckboxFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentCheckboxFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentCheckboxFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
