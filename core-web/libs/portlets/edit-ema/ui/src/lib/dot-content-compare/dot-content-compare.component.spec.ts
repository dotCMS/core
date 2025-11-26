/* eslint-disable @typescript-eslint/no-explicit-any */
import { it, expect } from '@jest/globals';
import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotMessageService,
    DotIframeService,
    DotFormatDateService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotContentCompareEvent } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { dotContentCompareTableDataMock } from './components/dot-content-compare-block-editor/dot-content-compare-block-editor.component.spec';
import { DotContentCompareTableComponent } from './components/dot-content-compare-table/dot-content-compare-table.component';
import { DotContentCompareComponent } from './dot-content-compare.component';
import { DotContentCompareStore } from './store/dot-content-compare.store';

const DotContentCompareEventMOCK = {
    inode: '1',
    identifier: '2',
    language: 'es'
};

@Component({
    standalone: false,
    selector: 'dot-test-host-component',
    template:
        '<dot-content-compare [data]="data"  (shutdown)="shutdown.emit(true)" ></dot-content-compare>'
})
class TestHostComponent {
    @Input() data: DotContentCompareEvent;
    @Output() shutdown = new EventEmitter<boolean>();
}

describe('DotContentCompareComponent', () => {
    let hostComponent: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotContentCompareStore: DotContentCompareStore;
    let contentCompareTableComponent: DotContentCompareTableComponent;
    let dotAlertConfirmService: DotAlertConfirmService;
    let dotIframeService: DotIframeService;

    const messageServiceMock = new MockDotMessageService({
        Confirm: 'Confirm',
        'folder.replace.contentlet.working.version':
            'Are you sure you would like to replace your working version with this contentlet version?'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [DotContentCompareComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotAlertConfirmService,
                ConfirmationService,
                {
                    provide: DotIframeService,
                    useValue: {
                        run: () => ({})
                    }
                },
                DotFormatDateService,
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'America/Costa_Rica',
                                label: 'Central Standard Time (America/Costa_Rica)',
                                offset: -21600000
                            })
                    }
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
            ]
        });
        TestBed.overrideProvider(DotContentCompareStore, {
            useValue: {
                vm$: of({ data: dotContentCompareTableDataMock, showDiff: false }),
                loadData: jest.fn(),
                updateShowDiff: jest.fn(),
                updateCompare: jest.fn(),
                bringBack: jest.fn()
            }
        });

        hostFixture = TestBed.createComponent(TestHostComponent);
        de = hostFixture.debugElement;

        dotContentCompareStore = TestBed.inject(DotContentCompareStore);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        dotIframeService = TestBed.inject(DotIframeService);
        hostComponent = hostFixture.componentInstance;
        hostComponent.data = DotContentCompareEventMOCK;
        hostFixture.detectChanges();
        contentCompareTableComponent = de.query(
            By.css('dot-content-compare-table')
        ).componentInstance;
    });

    it('should pass data correctly', () => {
        expect(dotContentCompareStore.loadData).toHaveBeenCalledWith(DotContentCompareEventMOCK);
        expect(contentCompareTableComponent.data).toEqual(dotContentCompareTableDataMock);
        expect(contentCompareTableComponent.showDiff).toEqual(false);
    });

    it('should update diff flag', () => {
        contentCompareTableComponent.changeDiff.emit(true);
        expect(dotContentCompareStore.updateShowDiff).toHaveBeenCalledWith(true);
    });

    it('should update compare content', () => {
        contentCompareTableComponent.changeVersion.emit('value' as unknown as DotCMSContentlet);
        expect(dotContentCompareStore.updateCompare).toHaveBeenCalledWith(
            'value' as unknown as DotCMSContentlet
        );
    });

    it('should bring back version after confirm and emit shutdown', () => {
        jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });
        const emitSpy = jest.spyOn(hostComponent.shutdown, 'emit');
        const iframeServiceSpy = jest.spyOn(dotIframeService, 'run');

        contentCompareTableComponent.bringBack.emit('123');

        expect<any>(dotAlertConfirmService.confirm).toHaveBeenCalledWith({
            accept: expect.any(Function),
            reject: expect.any(Function),
            header: 'Confirm',
            message:
                'Are you sure you would like to replace your working version with this contentlet version?'
        });

        expect(iframeServiceSpy).toHaveBeenCalledWith({
            name: 'getVersionBack',
            args: ['123']
        });
        expect(emitSpy).toHaveBeenCalledWith(true);
    });
});
