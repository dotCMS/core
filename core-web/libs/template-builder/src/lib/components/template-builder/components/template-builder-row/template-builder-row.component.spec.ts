import { describe, it, expect } from '@jest/globals';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';
import { DotAddStyleClassesDialogStore } from '../add-style-classes-dialog/store/add-style-classes-dialog.store';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

@Component({
    selector: 'dotcms-host-component',
    template: ` <dotcms-template-builder-row [row]="row">
        <p>Some component</p>
    </dotcms-template-builder-row>`
})
class HostComponent {
    row = {
        id: '1'
    };
}

describe('TemplateBuilderRowComponent', () => {
    let component: TemplateBuilderRowComponent;
    let fixture: ComponentFixture<HostComponent>;
    let store: DotTemplateBuilderStore;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                ButtonModule,
                TemplateBuilderRowComponent,
                RemoveConfirmDialogComponent,
                NoopAnimationsModule,
                DotMessagePipeModule,
                HttpClientTestingModule
            ],
            declarations: [HostComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                DotTemplateBuilderStore,
                DialogService,
                DotAddStyleClassesDialogStore
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HostComponent);
        component = fixture.debugElement.query(
            By.css('dotcms-template-builder-row')
        ).componentInstance;

        store = TestBed.inject(DotTemplateBuilderStore);

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
        const editRowStyleClassMock = jest.spyOn(component, 'editClasses');
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

    it('should trigger removeRow from store when clicking on deleteRow button and click yes', () => {
        const deleteMock = jest.spyOn(store, 'removeRow');

        const deleteButton = fixture.debugElement.query(
            By.css('p-button[data-testid="btn-remove-item"]')
        );

        deleteButton.nativeElement.dispatchEvent(new Event('onClick'));

        fixture.detectChanges();

        const confirmButton = document.querySelector('.p-confirm-popup-accept');
        confirmButton.dispatchEvent(new Event('click'));

        expect(deleteMock).toHaveBeenCalled();
    });

    it('should not trigger removeRow from store when clicking on deleteRow button and click no', () => {
        const deleteMock = jest.spyOn(store, 'removeRow');

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
