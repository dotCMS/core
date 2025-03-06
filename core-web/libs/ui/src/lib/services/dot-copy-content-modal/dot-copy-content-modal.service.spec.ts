import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyContentModalService } from './dot-copy-content-modal.service';

const messageServiceMock = new MockDotMessageService({
    'Edit-Content': 'Edit Content'
});

const CONTENT_EDIT_OPTIONS_MOCK = {
    option1: {
        value: 'NotCopy',
        message: 'editpage.content.edit.content.in.all.pages.message',
        icon: 'dynamic_feed',
        label: 'editpage.content.edit.content.in.all.pages',
        buttonLabel: 'editpage.content.edit.content.in.all.pages.button.label'
    },
    option2: {
        value: 'Copy',
        message: 'editpage.content.edit.content.in.this.page.message',
        icon: 'article',
        label: 'editpage.content.edit.content.in.this.page',
        buttonLabel: 'editpage.content.edit.content.in.this.page.button.label'
    }
};

const DYNAMIC_DIALOG_CONFIG = {
    header: 'Edit Content',
    width: '37rem',
    data: { options: CONTENT_EDIT_OPTIONS_MOCK },
    contentStyle: { padding: '0px' }
};

describe('DotCopyContentModalService', () => {
    let service: DotCopyContentModalService;
    let dialogService: DialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotCopyContentModalService,
                DialogService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });
        service = TestBed.inject(DotCopyContentModalService);
        dialogService = TestBed.inject(DialogService);
    });

    it('should not return anything if the user close the modal without select an option', (done) => {
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: of('')
        } as DynamicDialogRef);

        service.open().subscribe(
            (res) => fail('This should not be called. Response: ' + res),
            (err) => fail('This should not be called. Error: ' + err),
            () => {
                expect(true).toBe(true);
                done();
            }
        );
    });

    it('should return false if the user select the first option', (done) => {
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: of(CONTENT_EDIT_OPTIONS_MOCK.option1.value)
        } as DynamicDialogRef);

        service.open().subscribe((res) => {
            expect(res.shouldCopy).toBe(false);
            done();
        });
    });

    it('should return true if the user select the second option', (done) => {
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: of(CONTENT_EDIT_OPTIONS_MOCK.option2.value)
        } as DynamicDialogRef);

        service.open().subscribe((res) => {
            expect(res.shouldCopy).toBe(true);
            done();
        });
    });

    it('should have been called one time with the correct data', () => {
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: of('')
        } as DynamicDialogRef);

        service.open().subscribe();

        expect(dialogService.open).toHaveBeenCalledTimes(1);
        expect(dialogService.open).toHaveBeenCalledWith(
            expect.any(Function),
            DYNAMIC_DIALOG_CONFIG
        );
    });
});
