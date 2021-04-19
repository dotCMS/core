import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotMenuComponent } from './dot-menu.component';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

describe('DotMenuComponent', () => {
    let component: DotMenuComponent;
    let fixture: ComponentFixture<DotMenuComponent>;
    let button: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotMenuComponent],
            imports: [CommonModule, UiDotIconButtonModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotMenuComponent);
        component = fixture.componentInstance;
        component.float = true;
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
        fixture.detectChanges();
        button = fixture.debugElement.query(By.css('.dot-menu__button'));
    });

    it('should set the button to float', () => {
        expect(button.attributes.float).toBeDefined();

        component.float = false;
        fixture.detectChanges();
        button = fixture.debugElement.query(By.css('.dot-menu__button'));

        expect(button.attributes.float).not.toBeDefined();
    });

    it('should set visible to true and show the menu list', () => {
        button.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        fixture.detectChanges();

        const menulist: DebugElement = fixture.debugElement.query(By.css('.dot-menu__list'));

        expect(menulist).not.toBeNull();
        expect(component.visible).toBeTruthy();
    });

    it('should close menus when click the button', () => {
        button.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        fixture.detectChanges();

        const menulist: DebugElement = fixture.debugElement.query(By.css('.dot-menu__list'));

        expect(menulist).not.toBeNull();

        button.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        fixture.detectChanges();

        expect(component.visible).toBeFalsy();
    });

    it('should execute the command on the selected menu item and hide the menu', () => {
        spyOn(component.model[0], 'command');

        button.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        fixture.detectChanges();

        const menuItem: DebugElement = fixture.debugElement.query(By.css('.dot-menu-item__link'));
        menuItem.triggerEventHandler('click', {
            stopPropagation: () => {}
        });

        expect(component.model[0].command).toHaveBeenCalled();
        expect(component.visible).toBeFalsy();
    });

    it('should NOT exceute the command on the selected menu item if is disabled', () => {
        spyOn(component.model[1], 'command');
        button.triggerEventHandler('click', {
            stopPropagation: () => {}
        });

        fixture.detectChanges();
        const menuItems: DebugElement[] = fixture.debugElement.queryAll(
            By.css('.dot-menu-item__link')
        );
        menuItems[1].triggerEventHandler('click', {
            preventDefault: () => {}
        });

        expect(component.model[1].command).not.toHaveBeenCalled();
    });
});
