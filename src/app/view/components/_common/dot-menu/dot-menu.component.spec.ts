import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotMenuComponent } from './dot-menu.component';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

describe('DotMenuComponent', () => {
    let component: DotMenuComponent;
    let fixture: ComponentFixture<DotMenuComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotMenuComponent],
            imports: [CommonModule, ButtonModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotMenuComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        component.model = [
            {
                command: () => {},
                label: 'Add'
            },
            {
                command: () => {},
                label: 'Remove',
                disabled: true
            }
        ];
    });

    it('should set visible to true and show the menu list', () => {
        const button: DebugElement = fixture.debugElement.query(By.css('.dot-menu__button'));
        button.triggerEventHandler('click', {});
        fixture.detectChanges();

        const menulist: DebugElement = fixture.debugElement.query(By.css('.dot-menu__list'));

        expect(menulist).not.toBeNull();
        expect(component.visible).toBeTruthy();
    });

    it('should close menus when click the button', () => {
        const button: DebugElement = fixture.debugElement.query(By.css('.dot-menu__button'));
        button.triggerEventHandler('click', {});
        fixture.detectChanges();

        const menulist: DebugElement = fixture.debugElement.query(By.css('.dot-menu__list'));

        expect(menulist).not.toBeNull();

        button.triggerEventHandler('click', {});
        fixture.detectChanges();

        expect(component.visible).toBeFalsy();
    });

    it('should execute the command on the selected menu item and hide the menu', () => {
        spyOn(component.model[0], 'command');
        const button: DebugElement = fixture.debugElement.query(By.css('.dot-menu__button'));
        button.triggerEventHandler('click', {});
        fixture.detectChanges();
        const menuItem: DebugElement = fixture.debugElement.query(By.css('.dot-menu-item__link'));
        menuItem.triggerEventHandler('click', {});

        expect(component.model[0].command).toHaveBeenCalled();
        expect(component.visible).toBeFalsy();
    });

    it('should NOT exceute the command on the selected menu item if is disabled', () => {
        spyOn(component.model[1], 'command');
        const button: DebugElement = fixture.debugElement.query(By.css('.dot-menu__button'));
        button.triggerEventHandler('click', {});
        fixture.detectChanges();
        const menuItems: DebugElement[] = fixture.debugElement.queryAll(
            By.css('.dot-menu-item__link')
        );
        menuItems[1].triggerEventHandler('click', {});

        expect(component.model[1].command).not.toHaveBeenCalled();
    });
});
