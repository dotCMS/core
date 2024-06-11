import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

import { DotMenuComponent } from '@dotcms/ui';

describe('DotMenuComponent', () => {
    let component: DotMenuComponent;
    let fixture: ComponentFixture<DotMenuComponent>;
    let button: DebugElement;

    const menuItems = [
        {
            command: () => {
                //
            },
            label: 'Add'
        },
        {
            command: () => {
                //
            },
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

        expect(
            (button.componentInstance.styleClass as string).includes('p-button-text')
        ).toBeTrue();
    });

    it('should pass menu items to the Menu', () => {
        const menu: Menu = fixture.debugElement.query(By.css('p-menu')).componentInstance;
        expect(menu.model).toEqual(menuItems);
    });

    it('should show the menu list on click', () => {
        button.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });
        fixture.detectChanges();

        const menuList: DebugElement = fixture.debugElement.query(By.css('p-menu'));

        expect(menuList).not.toBeNull();
    });

    it('should close menus when click the button', () => {
        button.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });
        fixture.detectChanges();

        const menuList: Menu = fixture.debugElement.query(By.css('p-menu')).componentInstance;

        expect(menuList.visible).toEqual(true);

        button.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });

        fixture.detectChanges();
        expect(menuList.visible).toEqual(false);
    });
});
