import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBulkInformationComponent } from './dot-bulk-information.component';
import {
    DialogService,
    DynamicDialogConfig,
    DynamicDialogModule,
    DynamicDialogRef
} from 'primeng/dynamicdialog';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { FormatDateService } from '@services/format-date-service';
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

const messageServiceMock = new MockDotMessageService({
    'message.template.archived': 'archived',
    'message.template.failed': 'failed',
    'message.template.success': 'has been successfully',
    'message.template.singular': 'template',
    'message.template.plural': 'templates'
});

const mockBulkResponseFail: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 1,
    action: 'archived',
    fails: [
        {
            errorMessage: 'error 1',
            description: 'Template 1'
        },
        {
            errorMessage: 'error 2',
            description: 'Template 2'
        }
    ]
};

@Component({
    template: ` <div class="TestDynamicDialog"></div> `
})
export class TestDynamicDialogComponent {
    constructor(public dialogService: DialogService) {}

    show() {
        this.dialogService.open(DotBulkInformationComponent, {
            header: 'Test',
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: mockBulkResponseFail
        });
    }
}

describe('DotBulkInformationComponent', () => {
    let fixture: ComponentFixture<TestDynamicDialogComponent>;
    let testDynamicDialogComponent: TestDynamicDialogComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBulkInformationComponent, TestDynamicDialogComponent],
            imports: [
                CommonModule,
                DynamicDialogModule,
                DotMessagePipeModule,
                BrowserAnimationsModule
            ],
            providers: [
                DynamicDialogRef,
                DynamicDialogConfig,
                FormatDateService,
                DialogService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TestDynamicDialogComponent);
        testDynamicDialogComponent = fixture.componentInstance;
        testDynamicDialogComponent.show();
        fixture.detectChanges();
    });

    it('should show dialog', () => {
        const dynamicDialogEl = document.getElementsByClassName('p-dialog')[0];
        expect(dynamicDialogEl).toBeTruthy();
    });

    it('should load labels correctly', () => {
        const success: HTMLElement = document.querySelector('[data-testId="successful"]');
        const fail: HTMLElement = document.querySelector('[data-testId="fails"]');

        expect(success.innerText).toEqual('1 template has been successfully archived');
        expect(fail.innerText).toEqual('2 failed');
    });

    it('should list error messages', () => {
        const items: HTMLCollectionOf<Element> = document.getElementsByClassName(
            'bulk-information__fail-item'
        );
        expect(items.length).toEqual(2);
        expect((items[0].firstChild as HTMLElement).innerText).toEqual('Template 1');
        expect((items[0].lastChild as HTMLElement).innerText).toEqual('error 1');
        expect((items[1].firstChild as HTMLElement).innerText).toEqual('Template 2');
        expect((items[1].lastChild as HTMLElement).innerText).toEqual('error 2');
    });

    afterEach(() => {
        document.getElementsByTagName('p-dynamicdialog')[0].remove();
    });
});
