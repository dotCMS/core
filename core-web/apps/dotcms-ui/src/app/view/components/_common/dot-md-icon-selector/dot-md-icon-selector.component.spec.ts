import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UntypedFormBuilder, UntypedFormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMdIconSelectorComponent } from './dot-md-icon-selector.component';

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-md-icon-selector formControlName="icon"></dot-md-icon-selector>
        </form>
    `
})
class DotTestHostComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        this.form = this.fb.group({
            icon: '123'
        });
    }
}

describe('DotMdIconSelectorComponent', () => {
    let hostComp: DotTestHostComponent;
    let fixture: ComponentFixture<DotTestHostComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTestHostComponent, DotMdIconSelectorComponent],
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTestHostComponent);
        de = fixture.debugElement;
        hostComp = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        const selector = de.query(By.css('dot-md-icon-selector [data-testId="icon-picker"]'))
        selector.triggerEventHandler('dotValueChange', {target: {value: 'someicon'}});
        fixture.detectChanges();
        expect(hostComp.form.value).toEqual({ icon: 'someicon' });
    });
});
