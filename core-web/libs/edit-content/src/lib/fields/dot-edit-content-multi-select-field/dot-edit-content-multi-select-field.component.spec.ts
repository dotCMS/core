import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field.component';

describe('DotEditContentMultiSelectFieldComponent', () => {
    let component: DotEditContentMultiSelectFieldComponent;
    let fixture: ComponentFixture<DotEditContentMultiSelectFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentMultiSelectFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentMultiSelectFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
