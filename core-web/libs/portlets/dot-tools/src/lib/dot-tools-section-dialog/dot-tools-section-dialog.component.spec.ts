import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolsSectionDialogComponent } from './dot-tools-section-dialog.component';

import { DotToolsSection } from '../models/dot-tools.models';

const MOCK_SECTION: DotToolsSection = {
    id: 'site',
    name: 'Site',
    icon: 'language',
    tabOrder: 0,
    portletIds: []
};

describe('DotToolsSectionDialogComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotToolsSectionDialogComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotToolsSectionDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
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
        });

        it('closes with the form value on valid submit', () => {
            spectator.component['form'].patchValue({ name: 'Reporting', icon: 'analytics' });
            spectator.component['onSubmit']();
            expect(mockRef.close).toHaveBeenCalledWith({
                name: 'Reporting',
                icon: 'analytics'
            });
        });

        it('closes without a value on cancel', () => {
            spectator.component['onCancel']();
            expect(mockRef.close).toHaveBeenCalledWith();
        });
    });

    describe('edit mode', () => {
        let spectator: Spectator<DotToolsSectionDialogComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotToolsSectionDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { section: MOCK_SECTION } }
                },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
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
