import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';

import { DotDropdownComponent } from './dot-dropdown.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-dropdown-component
            [icon]="icon"
            [title]="title"
            [disabled]="disabled"></dot-dropdown-component>
    `
})
class DotTestHostComponent {
    disabled: boolean;
    icon: string;
    title: string;

    constructor() {
        this.icon = 'icon';
        this.title = 'test';
    }
}

function executeEnabled(
    elem: DebugElement,
    hostFixture: ComponentFixture<DotTestHostComponent>,
    de: DebugElement,
    comp: DotDropdownComponent
) {
    elem.nativeElement.click();
    hostFixture.detectChanges();
    const content = de.query(By.css('.dropdown-content'));
    expect(content).toBeTruthy();
    expect(elem).toBeTruthy();
    expect(comp.toggle.emit).toHaveBeenCalledWith(true);
}

function executeDisabled(elem: DebugElement, de: DebugElement) {
    elem.nativeElement.click();
    const content = de.query(By.css('.dropdown-content'));
    expect(content).toBeFalsy();
    expect(elem).toBeTruthy();
}

describe('DotDropdownComponent', () => {
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: DotTestHostComponent;

    let comp: DotDropdownComponent;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotDropdownComponent, DotTestHostComponent],
            imports: [BrowserAnimationsModule, ButtonModule]
        });

        hostFixture = DOTTestBed.createComponent(DotTestHostComponent);
        hostComp = hostFixture.debugElement.componentInstance;
        hostDe = hostFixture.debugElement;

        de = hostDe.query(By.css('dot-dropdown-component'));
        comp = de.componentInstance;
    }));

    describe('Enabled', () => {
        let button: DebugElement;
        let titleButton: DebugElement;

        beforeEach(() => {
            spyOn(comp.toggle, 'emit');
            hostComp.disabled = false;
            hostFixture.detectChanges();
            button = de.query(By.css('[data-testid="icon-button"]'));
            titleButton = de.query(By.css('[data-testid="title-button"]'));
        });

        it(`should dot-icon button be displayed & emit`, () => {
            executeEnabled(button, hostFixture, de, comp);
            expect(button.attributes.disabled).not.toBeDefined();
        });

        it(`should title button be displayed & emit`, () => {
            executeEnabled(titleButton, hostFixture, de, comp);
        });
    });

    describe('Disabled', () => {
        let button: DebugElement;
        let titleButton: DebugElement;

        beforeEach(() => {
            spyOn(comp.toggle, 'emit');
            hostComp.disabled = true;
            hostFixture.detectChanges();
            button = de.query(By.css('[data-testid="icon-button"]'));
            titleButton = de.query(By.css('[data-testid="title-button"]'));
        });

        it(`should dot-icon button not be displayed --> null`, () => {
            executeDisabled(button, de);
            expect(button.componentInstance.disabled).toBe(true);
        });

        it(`should title button not be displayed & not emit`, () => {
            executeDisabled(titleButton, de);
            expect(comp.toggle.emit).not.toHaveBeenCalled();
        });
    });

    it(`should hide the dropdown dialog`, () => {
        comp.closeIt();
        expect(comp.show).toBe(false);
    });

    it('shold show the mask', () => {
        comp.show = true;
        hostFixture.detectChanges();
        const mask = de.query(By.css('.dot-mask'));
        mask.nativeElement.click();
        expect(mask).toBeTruthy();
    });

    it('shold hide the mask', () => {
        comp.show = false;
        hostFixture.detectChanges();
        const mask = de.query(By.css('.dot-mask'));
        expect(mask).toBeFalsy();
    });
});
