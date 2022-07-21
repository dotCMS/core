/* eslint-disable @typescript-eslint/no-explicit-any */

import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotLayoutPropertiesItemComponent } from './dot-layout-properties-item.component';
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="group">
        <dot-layout-properties-item formControlName="header"></dot-layout-properties-item>
    </form>`
})
class TestHostComponent {
    group: UntypedFormGroup;
    f;
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
        DOTTestBed.configureTestingModule({
            declarations: [DotLayoutPropertiesItemComponent, TestHostComponent]
        });

        fixture = DOTTestBed.createComponent(DotLayoutPropertiesItemComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should propagate value on host click', () => {
        comp.value = false;
        expect(comp.value).toEqual(false);

        spyOn(comp, 'propagateChange');
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
        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-layout-properties-item'));
        const component: DotLayoutPropertiesItemComponent = de.componentInstance;
        comp.value = false;

        spyOn(component, 'writeValue');
        fixture.debugElement.nativeElement.click();
        hostComponentfixture.detectChanges();

        expect(comp.value).toEqual(true);
        expect<any>(component.writeValue).toHaveBeenCalledWith({ header: true });
    });
});
