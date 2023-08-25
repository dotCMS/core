import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AddWidgetComponent } from './add-widget.component';

@Component({
    selector: 'dotcms-host-component',
    template: ` <dotcms-add-widget [label]="label"></dotcms-add-widget> `
})
class HostComponent {
    @Input() label = 'Add Widget';
}

describe('AddWidgetComponent', () => {
    let fixture: ComponentFixture<HostComponent>;
    let de: DebugElement;
    let component: AddWidgetComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostComponent],
            imports: [AddWidgetComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(HostComponent);

        component = fixture.debugElement.query(By.css('dotcms-add-widget')).componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('inputs', () => {
        it('should set label', () => {
            expect(component.label).toBe('Add Widget');
        });
    });

    describe('template', () => {
        it('should have label', () => {
            de.query(By.css('[data-testid="cancelBtn"]'));
            const label = de.query(By.css('[data-testid="addWidgetLabel"]'));
            expect(label.nativeElement.textContent).toBe('Add Widget');
        });
    });
});
