import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
    DialogService,
    DynamicDialogConfig,
    DynamicDialogModule,
    DynamicDialogRef
} from 'primeng/dynamicdialog';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotActionBulkResult } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotBulkInformationComponent } from './dot-bulk-information.component';

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
    action: 'Template archived',
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
    template: `
        <div class="TestDynamicDialog"></div>
    `,
    standalone: false
})
export class TestDynamicDialogComponent {
    dialogService = inject(DialogService);

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
            imports: [CommonModule, DynamicDialogModule, DotMessagePipe, BrowserAnimationsModule],
            providers: [
                DynamicDialogRef,
                DynamicDialogConfig,
                DotFormatDateService,
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

        expect(success.textContent?.trim()).toEqual('Template archived: 1');
        expect(fail.textContent?.trim()).toEqual('2 failed');
    });

    it('should list error messages', () => {
        const items: HTMLCollectionOf<Element> = document.getElementsByClassName(
            'bulk-information__fail-item'
        );
        expect(items.length).toEqual(2);
        expect((items[0].firstChild as HTMLElement).textContent).toEqual('Template 1');
        expect((items[0].lastChild as HTMLElement).textContent).toEqual('error 1');
        expect((items[1].firstChild as HTMLElement).textContent).toEqual('Template 2');
        expect((items[1].lastChild as HTMLElement).textContent).toEqual('error 2');
    });

    afterEach(() => {
        document.getElementsByTagName('p-dynamicdialog')[0].remove();
    });
});
