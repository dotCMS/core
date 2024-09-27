import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { ControlContainer } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropZoneComponent } from '@dotcms/ui';

import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';

import {
    BINARY_FIELD_MOCK,
    createFormGroupDirectiveMock,
    FILE_FIELD_MOCK,
    IMAGE_FIELD_MOCK
} from '../../utils/mocks';

describe('DotEditContentFileFieldComponent', () => {
    let spectator: Spectator<DotEditContentFileFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFileFieldComponent,
        detectChanges: false,
        componentProviders: [FileFieldStore, mockProvider(DotFileFieldUploadService)],
        providers: [provideHttpClient(), mockProvider(DotMessageService)],
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ]
    });

    describe('FileField', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        field: FILE_FIELD_MOCK
                    } as unknown
                }))
        );

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-new-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeFalsy();
        });
    });

    describe('ImageField', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        field: IMAGE_FIELD_MOCK
                    } as unknown
                }))
        );

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-new-file'))).toBeFalsy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeTruthy();
        });
    });

    describe('BinaryField', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        field: BINARY_FIELD_MOCK
                    } as unknown
                }))
        );

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeFalsy();
            expect(spectator.query(byTestId('action-new-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeTruthy();
        });
    });
});
