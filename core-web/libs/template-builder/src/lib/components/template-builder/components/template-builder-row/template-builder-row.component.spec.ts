import { describe, it, expect } from '@jest/globals';

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

@Component({
    selector: 'dotcms-host-component',
    template: ` <dotcms-template-builder-row
        (editStyleClasses)="editRowStyleClass()"
        (deleteRow)="deleteRow()"
    >
        <p>Some component</p>
    </dotcms-template-builder-row>`
})
class HostComponent {
    deleteRow() {
        /*  */
    }
    editRowStyleClass() {
        /*  */
    }
}

describe('TemplateBuilderRowComponent', () => {
    let component: TemplateBuilderRowComponent;
    let fixture: ComponentFixture<HostComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                ButtonModule,
                TemplateBuilderRowComponent,
                RemoveConfirmDialogComponent,
                NoopAnimationsModule
            ],
            declarations: [HostComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(HostComponent);
        component = fixture.debugElement.query(
            By.css('dotcms-template-builder-row')
        ).componentInstance;

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a drag handler', () => {
        expect(fixture.debugElement.query(By.css('[data-testid="row-drag-handler"]'))).toBeTruthy();
    });
    it('should have a style class edit button', () => {
        expect(
            fixture.debugElement.query(By.css('p-button[data-testid="row-style-class-button"]'))
        ).toBeTruthy();
    });

    it('should render child', () => {
        expect(fixture.debugElement.query(By.css('p'))).toBeTruthy();
    });

    it('should trigger editRowStyleClass when clicking on editStyleClass button', () => {
        const editRowStyleClassMock = jest.spyOn(fixture.componentInstance, 'editRowStyleClass');
        const button = fixture.debugElement.query(
            By.css('p-button[data-testid="row-style-class-button"]')
        );

        button.nativeElement.dispatchEvent(new Event('onClick'));

        expect(editRowStyleClassMock).toHaveBeenCalled();
    });

    it('should have a remove item button', () => {
        expect(
            fixture.debugElement.query(By.css('p-button[data-testid="btn-remove-item"]'))
        ).toBeTruthy();
    });

    it('should trigger deleteRow when clicking on deleteRow button and click yes', () => {
        const deleteMock = jest.spyOn(fixture.componentInstance, 'deleteRow');

        const deleteButton = fixture.debugElement.query(
            By.css('p-button[data-testid="btn-remove-item"]')
        );

        deleteButton.nativeElement.dispatchEvent(new Event('onClick'));

        fixture.detectChanges();

        const confirmButton = document.querySelector('.p-confirm-popup-accept');
        confirmButton.dispatchEvent(new Event('click'));

        expect(deleteMock).toHaveBeenCalled();
    });

    it('should not trigger deleteRow when clicking on deleteRow button and click no', () => {
        const deleteMock = jest.spyOn(fixture.componentInstance, 'deleteRow');

        const deleteButton = fixture.debugElement.query(
            By.css('p-button[data-testid="btn-remove-item"]')
        );

        deleteButton.nativeElement.dispatchEvent(new Event('onClick'));

        fixture.detectChanges();

        const rejectButton = document.querySelector('.p-confirm-popup-reject');
        rejectButton.dispatchEvent(new Event('click'));

        expect(deleteMock).toHaveBeenCalledTimes(0);
    });
});
