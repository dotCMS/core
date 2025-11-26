import { describe, expect, it } from '@jest/globals';

import { NgStyle } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { DOT_MESSAGE_SERVICE_TB_MOCK, INITIAL_STATE_MOCK } from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBackgroundColumnsComponent } from '../template-builder-background-columns/template-builder-background-columns.component';

@Component({
    selector: 'dotcms-host-component',
    standalone: false,
    template: `
        <dotcms-template-builder-row [row]="row" [isResizing]="isResizing">
            <p>Some component</p>
        </dotcms-template-builder-row>
    `
})
class HostComponent {
    isResizing = false;
    row = {
        id: '1',
        willBoxFit: false
    };
}

describe('TemplateBuilderRowComponent', () => {
    let component: TemplateBuilderRowComponent;
    let fixture: ComponentFixture<HostComponent>;
    let store: DotTemplateBuilderStore;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                NgStyle,
                ButtonModule,
                TemplateBuilderRowComponent,
                RemoveConfirmDialogComponent,
                NoopAnimationsModule,
                DotMessagePipe,
                HttpClientTestingModule,
                TemplateBuilderBackgroundColumnsComponent
            ],
            declarations: [HostComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                DotTemplateBuilderStore,
                DialogService
            ],
            teardown: { destroyAfterEach: false }
        }).compileComponents();

        fixture = TestBed.createComponent(HostComponent);
        component = fixture.debugElement.query(
            By.css('dotcms-template-builder-row')
        ).componentInstance;

        store = TestBed.inject(DotTemplateBuilderStore);

        store.setState({
            ...INITIAL_STATE_MOCK,
            layoutProperties: {
                header: false,
                footer: false,
                sidebar: null
            }
        });

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

    it('should have a background when resizing', () => {
        fixture.componentInstance.isResizing = true;
        fixture.detectChanges();

        expect(
            fixture.debugElement.query(By.css('dotcms-template-builder-background-columns'))
        ).toBeTruthy();
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
