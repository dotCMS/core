import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotActionButtonComponent } from './dot-action-button.component';

describe('ActionButtonComponent', () => {
    let comp: DotActionButtonComponent;
    let fixture: ComponentFixture<DotActionButtonComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotActionButtonComponent],
            imports: [
                BrowserAnimationsModule,
                MenuModule,
                ButtonModule,
                RouterTestingModule.withRoutes([
                    {
                        component: DotActionButtonComponent,
                        path: 'test'
                    }
                ])
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotActionButtonComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    }));

    it('should have no-label class by default', () => {
        fixture.detectChanges();
        expect(de.nativeElement.classList).toContain('action-button--no-label');
    });

    it('should have no-label class by default', () => {
        comp.label = 'Hello World';
        fixture.detectChanges();
        expect(de.nativeElement.classList).not.toContain('action-button--no-label');
    });

    it('should have only button in default state', () => {
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('p-button'))).toBeTruthy();
        expect(fixture.debugElement.query(By.css('.action-button__label')) === null).toBe(
            true,
            'label hidden by default'
        );
        expect(fixture.debugElement.query(By.css('p-menu')) === null).toBe(
            true,
            'menu hidden by default'
        );
    });

    it('should have label', () => {
        comp.label = 'Hello World';
        fixture.detectChanges();
        const label = fixture.debugElement.query(By.css('.action-button__label'));
        expect(label.nativeElement.textContent.trim()).toBe('Hello World');
    });

    it('should have p-menu and pass the model to it', () => {
        const model = [
            {
                command: () => {
                    //
                },
                icon: 'whatever',
                label: 'Whatever'
            }
        ];

        comp.model = model;
        fixture.detectChanges();
        const menu = fixture.debugElement.query(By.css('p-menu'));
        expect(menu).toBeDefined();

        expect(menu.componentInstance.model).toEqual(
            model,
            'model its being pass to primeng component'
        );
    });

    it('should emit event on button click', () => {
        let res;

        comp.press.subscribe((event) => {
            res = event;
        });

        const button = fixture.debugElement.query(By.css('p-button'));
        button.nativeNode.click();
        expect(res).toBeDefined();
    });

    it('should toggle the menu on button click', () => {
        const model = [
            {
                command: () => {
                    //
                },
                icon: 'whatever',
                label: 'Whatever'
            }
        ];

        comp.model = model;
        fixture.detectChanges();

        jest.spyOn(comp.menu, 'toggle');

        const button = fixture.debugElement.query(By.css('p-button'));
        button.nativeNode.click();
        expect(comp.menu.toggle).toHaveBeenCalledTimes(1);
    });

    it('should set button to disabled state', () => {
        comp.disabled = true;
        comp.label = 'Label';
        fixture.detectChanges();
        const button = fixture.debugElement.query(By.css('p-button'));
        const label = fixture.debugElement.query(By.css('.action-button__label'));
        expect(button.componentInstance.disabled).toBe(true);
        expect(label.nativeElement.classList).toContain(
            'action-button__label--disabled',
            'Label disabled class'
        );
    });
});
