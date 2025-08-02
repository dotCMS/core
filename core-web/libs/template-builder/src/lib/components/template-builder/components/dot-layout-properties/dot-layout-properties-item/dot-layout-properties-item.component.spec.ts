/* eslint-disable @typescript-eslint/no-explicit-any */

import { describe, expect, it } from '@jest/globals';

import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormControl,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotLayoutPropertiesItemComponent } from './dot-layout-properties-item.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-test-host-component',
    standalone: false,
    template: `
        <form [formGroup]="group">
            <dot-layout-properties-item formControlName="header"></dot-layout-properties-item>
        </form>
    `
})
class TestHostComponent {
    group: UntypedFormGroup;

    constructor() {
        this.group = new UntypedFormGroup({
            header: new UntypedFormControl({
                header: true
            })
        });
    }
}

describe('DotLayoutPropertiesItemComponent', () => {
    let comp: DotLayoutPropertiesItemComponent;
    let fixture: ComponentFixture<DotLayoutPropertiesItemComponent>;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotLayoutPropertiesItemComponent, TestHostComponent],
            imports: [FormsModule, CommonModule, ReactiveFormsModule]
        });

        fixture = TestBed.createComponent(DotLayoutPropertiesItemComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should propagate value on host click', () => {
        comp.value = false;
        expect(comp.value).toEqual(false);

        jest.spyOn(comp, 'propagateChange');
        de.nativeElement.click();

        expect(comp.value).toEqual(true);
        expect(comp.propagateChange).toHaveBeenCalled();
    });

    it('should emit switch value on host click', () => {
        let res: boolean;

        comp.switch.subscribe((value) => (res = value));
        de.nativeElement.click();

        expect(res).toEqual(true);
    });

    it('should add a selected class to the item if value is true', () => {
        const svgEl = de.children[0].nativeElement;

        de.nativeElement.click();

        fixture.detectChanges();
        expect(svgEl.classList).toContain('property-item__icon--selected');
    });

    it('should call writeValue to define the initial value of the property item', () => {
        hostComponentfixture = TestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-layout-properties-item'));
        const component: DotLayoutPropertiesItemComponent = de.componentInstance;
        comp.value = false;

        jest.spyOn(component, 'writeValue');
        fixture.debugElement.nativeElement.click();
        hostComponentfixture.detectChanges();

        expect(comp.value).toEqual(true);
        expect<any>(component.writeValue).toHaveBeenCalledWith({ header: true });
    });
});
