import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

import { DotMenuComponent } from './dot-menu.component';

describe('DotMenuComponent', () => {
    let component: DotMenuComponent;
    let fixture: ComponentFixture<DotMenuComponent>;
    let button: DebugElement;

    const menuItems = [
        {
            command: jest.fn(),
            label: 'Add'
        },
        {
            command: jest.fn(),
            label: 'Remove',
            disabled: true
        }
    ];

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                ButtonModule,
                MenuModule,
                NoopAnimationsModule,
                DotMenuComponent
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotMenuComponent);
        component = fixture.componentInstance;
        component.float = true;
        component.model = menuItems;
        fixture.detectChanges();
        button = fixture.debugElement.query(By.css('[data-testid="dot-menu-button"]'));
    });

    it('should set the button to float', () => {
        component.float = false;
        fixture.detectChanges();
        button = fixture.debugElement.query(By.css('[data-testid="dot-menu-button"]'));

        expect((button.componentInstance.styleClass as string).includes('p-button-text')).toBe(
            true
        );
    });

    it('should pass menu items to the Menu', () => {
        const menu: Menu = fixture.debugElement.query(By.css('p-menu')).componentInstance;
        expect(menu.model).toEqual(menuItems);
    });

    it('should show the menu list on click', () => {
        const stopPropagation = jest.fn();
        button.triggerEventHandler('click', { stopPropagation });
        fixture.detectChanges();

        const menuList: DebugElement = fixture.debugElement.query(By.css('p-menu'));

        expect(menuList).not.toBeNull();
        expect(stopPropagation).toHaveBeenCalled();
    });

    it('should close menus when click the button', () => {
        const stopPropagation = jest.fn();
        button.triggerEventHandler('click', { stopPropagation });
        fixture.detectChanges();

        const menuList: Menu = fixture.debugElement.query(By.css('p-menu')).componentInstance;

        expect(menuList.visible).toBe(true);
        expect(stopPropagation).toHaveBeenCalled();

        button.triggerEventHandler('click', { stopPropagation });

        fixture.detectChanges();
        expect(menuList.visible).toBe(false);
        expect(stopPropagation).toHaveBeenCalledTimes(2);
    });
});
