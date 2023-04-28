import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyContentModalService, OPTIONS } from './dot-copy-content-modal.service';

const messageServiceMock = new MockDotMessageService({
    'Edit-Content': 'Edit Content'
});

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

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should not return anything if the user close the modal without select an option', (done) => {
        service.open().subscribe(
            (res) => fail('This should not be called. Response: ' + res),
            (err) => fail('This should not be called. Error: ' + err),
            () => {
                expect(true).toBe(true);
                done();
            }
        );

        service.dialogRef.close();
    });

    it('should return true if the user select the first option', (done) => {
        service.open().subscribe((res) => {
            expect(res.shouldCopy).toBe(true);
            done();
        });

        service.dialogRef.close(OPTIONS.option1.value);
    });

    it('should return false if the user select the second option', (done) => {
        service.open().subscribe((res) => {
            expect(res.shouldCopy).toBe(false);
            done();
        });

        service.dialogRef.close(OPTIONS.option2.value);
    });

    it('shoud have be called one time with the correct data', () => {
        spyOn(dialogService, 'open').and.returnValue({
            onClose: of('')
        } as DynamicDialogRef);
        service.open().subscribe();
        expect(dialogService.open).toHaveBeenCalledTimes(1);

        expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
            header: 'Edit Content',
            width: '37rem',
            data: { options: OPTIONS },
            contentStyle: { padding: '0px' }
        });
    });
});
