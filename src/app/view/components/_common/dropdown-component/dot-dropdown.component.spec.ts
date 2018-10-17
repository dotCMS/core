import { ComponentFixture, tick, fakeAsync } from '@angular/core/testing';
import { Component, Input, DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotDropdownComponent } from './dot-dropdown.component';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

@Component({
    selector: 'dot-test-host-component',
    template:
        `<dot-dropdown-component
            [gravatar]="gravatar"
            [icon]="icon"
            [title]="title"
            [disabled]="disabled"></dot-dropdown-component>`
})
class DotTestHostComponent {
    disabled: boolean;
    gravatar: string;
    icon: string;
    title: string;
}

@Component({
    selector: 'dot-gravatar',
    template: ''
})
class MockGravatarComponent {
    @Input()
    email;
}

@Component({
    selector: 'dot-icon-button',
    template: ''
})
class MockDotIconButtonComponent {
    @Input()
    icon: string;
    @Input()
    disabled?: boolean;
}

describe('DotDropdownComponent', () => {
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: DotTestHostComponent;

    let comp: DotDropdownComponent;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DotDropdownComponent,
                MockGravatarComponent,
                MockDotIconButtonComponent,
                DotTestHostComponent
            ],
            imports: [
                BrowserAnimationsModule
            ]
        });

        hostFixture = DOTTestBed.createComponent(DotTestHostComponent);
        hostComp = hostFixture.debugElement.componentInstance;
        hostDe = hostFixture.debugElement;

        de = hostDe.query(By.css('dot-dropdown-component'));
        comp = de.componentInstance;
    });

    it(`should dot-icon button be enabled`, () => {
        spyOn(comp.toggle, 'emit');
        hostComp.icon = 'icon';
        hostComp.disabled = false;
        hostFixture.detectChanges();
        const button: DebugElement = de.query(By.css('dot-icon-button'));
        button.nativeElement.click();
        hostFixture.detectChanges();
        const content: DebugElement = de.query(By.css('.dropdown-content'));
        expect(button).toBeTruthy();
        expect(content).toBeTruthy();
        expect(button.attributes.disabled).toBe(null);
        expect(comp.toggle.emit).toHaveBeenCalledWith(true);
    });

    it(`should dot-icon button be disabled --> null`, fakeAsync(() => {
        hostComp.icon = 'icon';
        hostComp.disabled = true;
        hostFixture.detectChanges();
        const button: DebugElement = de.query(By.css('dot-icon-button'));
        button.nativeElement.click();
        tick(1);
        const content: DebugElement = de.query(By.css('.dropdown-content'));
        expect(button).toBeTruthy();
        expect(content).toBeFalsy();
        expect(button.attributes.disabled).toBe('true');
    }));

    it(`should title button be enabled`, () => {
        spyOn(comp.toggle, 'emit');
        hostComp.title = 'test';
        hostComp.disabled = false;
        hostFixture.detectChanges();
        const button: DebugElement = de.query(By.css('button'));
        button.nativeElement.click();
        hostFixture.detectChanges();
        const content: DebugElement = de.query(By.css('.dropdown-content'));
        expect(button).toBeTruthy();
        expect(content).toBeTruthy();
        expect(comp.toggle.emit).toHaveBeenCalledWith(true);
    });

    it(`should title button be disabled`, fakeAsync(() => {
        spyOn(comp.toggle, 'emit');
        hostComp.title = 'test';
        hostComp.disabled = true;
        hostFixture.detectChanges();
        const button: DebugElement = de.query(By.css('button'));
        button.nativeElement.click();
        tick(1);
        const content: DebugElement = de.query(By.css('.dropdown-content'));
        expect(button).toBeTruthy();
        expect(content).toBeFalsy();
        expect(comp.toggle.emit).not.toHaveBeenCalled();
    }));

    it(`should hide the dropdown dialog`, () => {
        comp.closeIt();
        expect(comp.show).toBe(false);
    });

    it(`should dot-gravatar be displayed`, () => {
        spyOn(comp.toggle, 'emit');
        hostComp.gravatar = 'test@test.com';
        hostFixture.detectChanges();
        const gravatar: DebugElement = de.query(By.css('dot-gravatar'));
        gravatar.nativeElement.click();
        hostFixture.detectChanges();
        const content: DebugElement = de.query(By.css('.dropdown-content'));
        expect(gravatar).toBeTruthy();
        expect(content).toBeTruthy();
        expect(comp.toggle.emit).toHaveBeenCalledWith(true);
    });

});
