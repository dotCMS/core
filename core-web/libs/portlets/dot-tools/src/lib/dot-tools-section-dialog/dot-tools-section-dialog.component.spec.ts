import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolsSectionDialogComponent } from './dot-tools-section-dialog.component';

import { DotToolsStore } from '../dot-tools-page/store/dot-tools.store';
import { DotToolsSection } from '../models/dot-tools.models';

const MOCK_SECTION: DotToolsSection = {
    id: 'site',
    name: 'Site',
    icon: 'language',
    tabOrder: 0,
    portletIds: [],
    portletTitles: []
};

describe('DotToolsSectionDialogComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotToolsSectionDialogComponent>;
        const mockRef = { close: vi.fn() };
        const createSectionSpy = vi.fn().mockReturnValue(
            of<DotToolsSection>({
                id: 'reporting',
                name: 'Reporting',
                icon: 'analytics',
                tabOrder: 0,
                portletIds: [],
                portletTitles: []
            })
        );

        const createComponent = createComponentFactory({
            component: DotToolsSectionDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotToolsStore, { createSection: createSectionSpy }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            createSectionSpy.mockClear();
            spectator = createComponent();
        });

        it('starts with an empty name and the default icon', () => {
            expect(spectator.component['form'].getRawValue()).toEqual({
                name: '',
                icon: 'widgets'
            });
            expect(spectator.query(byTestId('tools-section-name-error'))).toBeNull();
            expect(spectator.query(byTestId('tools-section-form-error'))).toBeNull();
        });

        it('shows the field error + footer warning on invalid submit', () => {
            spectator.component['onSubmit']();
            spectator.detectChanges();
            expect(spectator.query(byTestId('tools-section-name-error'))).not.toBeNull();
            expect(spectator.query(byTestId('tools-section-form-error'))).not.toBeNull();
            expect(mockRef.close).not.toHaveBeenCalled();
            expect(createSectionSpy).not.toHaveBeenCalled();
        });

        it('calls the store and closes on valid submit', () => {
            spectator.component['form'].patchValue({ name: 'Reporting', icon: 'analytics' });
            spectator.component['onSubmit']();
            expect(createSectionSpy).toHaveBeenCalledWith({
                name: 'Reporting',
                icon: 'analytics'
            });
            expect(mockRef.close).toHaveBeenCalledWith(true);
        });

        it('closes without a value on cancel', () => {
            spectator.component['onCancel']();
            expect(mockRef.close).toHaveBeenCalledWith();
        });

        it('renders inline server error on 400 and keeps the dialog open', () => {
            createSectionSpy.mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 400,
                            statusText: 'Bad Request',
                            error: { message: 'Section name already exists' }
                        })
                )
            );
            spectator.component['form'].patchValue({ name: 'Dup', icon: 'widgets' });
            spectator.component['onSubmit']();
            spectator.detectChanges();

            expect(mockRef.close).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('tools-section-submit-error'))?.textContent).toContain(
                'Section name already exists'
            );
        });

        it('routes 5xx through the global handler and closes', () => {
            const handler = spectator.inject(DotHttpErrorManagerService);
            createSectionSpy.mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' }))
            );
            spectator.component['form'].patchValue({ name: 'OK', icon: 'widgets' });
            spectator.component['onSubmit']();

            expect(handler.handle).toHaveBeenCalled();
            expect(mockRef.close).toHaveBeenCalledWith();
        });
    });

    describe('edit mode', () => {
        let spectator: Spectator<DotToolsSectionDialogComponent>;
        const mockRef = { close: vi.fn() };
        const updateSectionSpy = vi.fn().mockReturnValue(of(MOCK_SECTION));

        const createComponent = createComponentFactory({
            component: DotToolsSectionDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { section: MOCK_SECTION } }
                },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotToolsStore, { updateSection: updateSectionSpy }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            updateSectionSpy.mockClear();
            spectator = createComponent();
        });

        it('prefills the form from the incoming section', () => {
            expect(spectator.component['form'].getRawValue()).toEqual({
                name: 'Site',
                icon: 'language'
            });
            expect(spectator.component['isEdit']).toBe(true);
        });
    });
});
