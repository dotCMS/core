import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DotLayoutSidebarComponent } from './dot-layout-property-sidebar.component';
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { DotLayoutPropertiesItemModule } from '../dot-layout-properties-item/dot-layout-properties-item.module';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="group">
        <dot-layout-property-sidebar formControlName="sidebar"></dot-layout-property-sidebar>
    </form>`
})
class TestHostComponent {
    group: UntypedFormGroup;
    constructor() {
        this.group = new UntypedFormGroup({
            sidebar: new UntypedFormControl({
                location: 'left',
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
        DOTTestBed.configureTestingModule({
            declarations: [DotLayoutSidebarComponent, TestHostComponent],
            imports: [DotLayoutPropertiesItemModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(DotLayoutSidebarComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        comp.value = {
            location: 'left',
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
        layoutPropertyItemEl.click();
        spyOn(comp, 'propagateChange');
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

        spyOn(comp.propertyItemLeft, 'setChecked');
        spyOn(comp.propertyItemRight, 'setUnchecked');
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

        spyOn(comp.propertyItemLeft, 'setUnchecked');
        spyOn(comp.propertyItemRight, 'setChecked');
        comp.setValue(true, 'right');

        expect(res).toEqual(true);
        expect(comp.propertyItemLeft.setUnchecked).toHaveBeenCalled();
        expect(comp.propertyItemRight.setChecked).toHaveBeenCalled();
    });

    it('should call writeValue to define the initial value of sidebar item', () => {
        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-layout-property-sidebar'));
        const component: DotLayoutSidebarComponent = de.componentInstance;
        component.value = {
            location: '',
            containers: [],
            width: ''
        };

        spyOn(component, 'writeValue');
        comp.setValue(true, 'left');
        hostComponentfixture.detectChanges();

        expect(comp.value.location).toEqual('left');
        expect(component.writeValue).toHaveBeenCalledWith({
            location: 'left',
            containers: [],
            width: ''
        });
    });

    xit('should show selected left or right based on the sidebar location value', () => {
        //
    });
});
