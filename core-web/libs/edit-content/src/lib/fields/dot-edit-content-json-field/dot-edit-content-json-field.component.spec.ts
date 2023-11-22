import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field.component';

describe('DotEditContentJsonFieldComponent', () => {
    let component: DotEditContentJsonFieldComponent;
    let fixture: ComponentFixture<DotEditContentJsonFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentJsonFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentJsonFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
