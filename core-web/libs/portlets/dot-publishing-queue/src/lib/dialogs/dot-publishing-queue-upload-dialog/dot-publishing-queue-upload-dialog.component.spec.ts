import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueUploadDialogComponent } from './dot-publishing-queue-upload-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueUploadDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueUploadDialogComponent>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;
    let store: ReturnType<typeof storeFactory>;

    const uploadInFlight = signal(false);

    function storeFactory() {
        return {
            uploadInFlight,
            uploadBundle: jest.fn((_file: File, cb?: () => void) => cb?.())
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueUploadDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, storeFactory()),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        uploadInFlight.set(false);
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
        store = spectator.inject(DotPublishingQueueStore) as unknown as ReturnType<
            typeof storeFactory
        >;
        jest.clearAllMocks();
    });

    it('disables submit until a file is selected', () => {
        expect(spectator.component.selectedFile()).toBeNull();
    });

    it('stores the selected file', () => {
        const file = new File(['x'], 'bundle.tar.gz', { type: 'application/gzip' });
        spectator.component.onSelect({ files: [file] } as never);
        expect(spectator.component.selectedFile()).toBe(file);
    });

    it('clears file on onClear', () => {
        spectator.component.onSelect({
            files: [new File(['x'], 'b.tar.gz')]
        } as never);
        spectator.component.onClear();
        expect(spectator.component.selectedFile()).toBeNull();
    });

    it('submit calls store.uploadBundle + closes the dialog with uploaded:true', () => {
        const file = new File(['x'], 'bundle.tar.gz');
        spectator.component.onSelect({ files: [file] } as never);
        spectator.component.onSubmit();
        expect(store.uploadBundle).toHaveBeenCalledWith(file, expect.any(Function));
        expect(dialogRef.close).toHaveBeenCalledWith({ uploaded: true });
    });

    it('submit is a no-op when no file is selected', () => {
        spectator.component.onSubmit();
        expect(store.uploadBundle).not.toHaveBeenCalled();
    });

    it('cancel closes the dialog', () => {
        spectator.component.onCancel();
        expect(dialogRef.close).toHaveBeenCalled();
    });
});
