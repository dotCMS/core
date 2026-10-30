import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AddWidgetComponent } from './add-widget.component';

import { colIcon, rowIcon } from '../../assets/icons';

@Component({
    standalone: false,
    selector: 'dotcms-host-component',
    template: `
        <dotcms-add-widget [label]="label" [icon]="icon"></dotcms-add-widget>
    `
})
class HostComponent {
    @Input() label = 'Add Widget';
    @Input() icon = rowIcon;
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

        it('should have row icon', () => {
            expect(component.icon).toBe(rowIcon);
        });

        it('should have col icon', () => {
            component.icon = colIcon;
            fixture.detectChanges();
            expect(component.icon).toBe(colIcon);
        });
    });

    describe('template', () => {
        it('should have label', () => {
            de.query(By.css('[data-testid="cancelBtn"]'));
            const label = de.query(By.css('[data-testid="addWidgetLabel"]'));
            expect(label.nativeElement.textContent).toBe('Add Widget');
        });

        it('should have a image element with the row icon', () => {
            component.icon = rowIcon;
            fixture.detectChanges();
            const img = de.query(By.css('img'));
            expect(img.nativeElement.src).toContain(rowIcon);
        });

        it('it should have material icon element when image load fails', () => {
            component.icon = 'add';
            fixture.detectChanges();
            const img = de.query(By.css('img'));
            img.triggerEventHandler('error', null);
            fixture.detectChanges();
            const icon = de.query(By.css('[data-testId="material-icons-fallback"]'));
            expect(icon?.nativeElement.textContent).toContain('add');
        });
    });
});
