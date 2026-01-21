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

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotLayoutSidebarComponent } from './dot-layout-property-sidebar.component';

import { DotLayoutPropertiesItemComponent } from '../dot-layout-properties-item/dot-layout-properties-item.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-test-host-component',
    standalone: false,
    template: `
        <form [formGroup]="group">
            <dot-layout-property-sidebar formControlName="sidebar"></dot-layout-property-sidebar>
        </form>
    `
})
class TestHostComponent {
    group: UntypedFormGroup;

    constructor() {
        this.group = new UntypedFormGroup({
            sidebar: new UntypedFormControl({
                location: 'right',
                containers: [],
                width: ''
            })
        });
    }
}

describe('DotLayoutSidebarComponent', () => {
    let comp: DotLayoutSidebarComponent;
    let fixture: ComponentFixture<DotLayoutSidebarComponent>;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;

    const messageServiceMock = new MockDotMessageService({
        'editpage.layout.properties.sidebar.left': 'Sidebar left',
        'editpage.layout.properties.sidebar.right': 'Sidebar right'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotLayoutSidebarComponent,
                DotLayoutPropertiesItemComponent,
                DotMessagePipe,
                FormsModule,
                ReactiveFormsModule,
                CommonModule
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = TestBed.createComponent(DotLayoutSidebarComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        comp.value = {
            location: 'right',
            containers: [],
            width: ''
        };

        fixture.detectChanges();
    });

    it('should propagate switch after sidebar property item is clicked', () => {
        let res = false;
        const dotLayoutPropertiesItem = de.query(
            By.css('dot-layout-properties-item')
        ).componentInstance;
        const layoutPropertyItemEl: HTMLElement = de.query(
            By.css('dot-layout-properties-item')
        ).nativeElement;

        dotLayoutPropertiesItem.switch.subscribe((value) => (res = value));
        jest.spyOn(comp, 'propagateChange');
        layoutPropertyItemEl.click();
        comp.setValue(true, 'left');

        expect(res).toEqual(true);
        expect(comp.propagateChange).toHaveBeenCalled();
    });

    it('should check left value and unchecked right value', () => {
        let res = false;
        const dotLayoutPropertiesItem = de.query(
            By.css('dot-layout-properties-item')
        ).componentInstance;
        const layoutPropertyItemEl: HTMLElement = de.query(
            By.css('dot-layout-properties-item')
        ).nativeElement;

        dotLayoutPropertiesItem.switch.subscribe((value) => (res = value));
        layoutPropertyItemEl.click();

        jest.spyOn(comp.propertyItemLeft, 'setChecked');
        jest.spyOn(comp.propertyItemRight, 'setUnchecked');
        comp.setValue(true, 'left');

        expect(res).toEqual(true);
        expect(comp.propertyItemLeft.setChecked).toHaveBeenCalled();
        expect(comp.propertyItemRight.setUnchecked).toHaveBeenCalled();
    });

    it('should check right value and unchecked left value', () => {
        let res = false;
        const dotLayoutPropertiesItem = de.query(
            By.css('dot-layout-properties-item')
        ).componentInstance;
        const layoutPropertyItemEl: HTMLElement = de.query(
            By.css('dot-layout-properties-item')
        ).nativeElement;

        dotLayoutPropertiesItem.switch.subscribe((value) => (res = value));
        layoutPropertyItemEl.click();

        jest.spyOn(comp.propertyItemLeft, 'setUnchecked');
        jest.spyOn(comp.propertyItemRight, 'setChecked');
        comp.setValue(true, 'right');

        expect(res).toEqual(true);
        expect(comp.propertyItemLeft.setUnchecked).toHaveBeenCalled();
        expect(comp.propertyItemRight.setChecked).toHaveBeenCalled();
    });

    it('should call writeValue to define the initial value of sidebar item', () => {
        hostComponentfixture = TestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-layout-property-sidebar'));
        const component: DotLayoutSidebarComponent = de.componentInstance;
        component.value = {
            location: '',
            containers: [],
            width: ''
        };

        jest.spyOn(component, 'writeValue');
        comp.setValue(true, 'right');
        hostComponentfixture.detectChanges();

        expect(comp.value.location).toEqual('right');
        expect(component.writeValue).toHaveBeenCalledWith({
            location: 'right',
            containers: [],
            width: ''
        });
    });

    it.skip('should show selected left or right based on the sidebar location value', () => {
        //
    });
});
