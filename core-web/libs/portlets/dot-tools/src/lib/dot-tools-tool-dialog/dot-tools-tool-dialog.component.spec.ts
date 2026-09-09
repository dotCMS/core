import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolsToolDialogComponent } from './dot-tools-tool-dialog.component';

import { DotToolsCatalogEntry, DotToolsCustomToolConfig } from '../models/dot-tools.models';

const MOCK_CONTENT_TYPES = [
    { variable: 'Blog', name: 'Blog' } as DotCMSContentType,
    { variable: 'Product', name: 'Product' } as DotCMSContentType
];

const MOCK_TOOL: DotToolsCatalogEntry = {
    id: 'c_press-releases',
    title: 'Press Releases',
    isCustom: true
};

const MOCK_PREFILL: DotToolsCustomToolConfig = {
    portletId: 'c_press-releases',
    portletName: 'Press Releases',
    baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
    contentTypes: ['Blog'],
    dataViewMode: 'Card'
};

describe('DotToolsToolDialogComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotToolsToolDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotContentTypeService, {
                    getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('loads and sorts content types from the service', () => {
            const options = spectator.component['$contentTypes']();
            expect(options).toEqual([
                { variable: 'Blog', label: 'Blog' },
                { variable: 'Product', label: 'Product' }
            ]);
            expect(spectator.component['$contentTypesLoading']()).toBe(false);
        });

        it('auto-slugs the id from the name until the user types their own', () => {
            spectator.component['form'].controls.portletName.setValue('Press Releases');
            spectator.component['onNameChange']('Press Releases');
            expect(spectator.component['form'].controls.portletId.value).toBe('press-releases');

            spectator.component['onIdTouched']();
            spectator.component['form'].controls.portletName.setValue('Something Else');
            spectator.component['onNameChange']('Something Else');
            // id stays put once user has interacted with it
            expect(spectator.component['form'].controls.portletId.value).toBe('press-releases');
        });

        it('shows warning on invalid submit and keeps the dialog open', () => {
            spectator.component['onSubmit']();
            spectator.detectChanges();
            expect(spectator.query(byTestId('tools-tool-form-error'))).not.toBeNull();
            expect(mockRef.close).not.toHaveBeenCalled();
        });

        it('closes with the form value on valid submit', () => {
            spectator.component['form'].patchValue({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'Card'
            });
            spectator.component['onSubmit']();
            expect(mockRef.close).toHaveBeenCalledWith(
                expect.objectContaining({
                    portletName: 'Press Releases',
                    portletId: 'press-releases',
                    dataViewMode: 'Card'
                })
            );
        });
    });

    describe('edit mode with prefill', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotToolsToolDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { tool: MOCK_TOOL, prefill: MOCK_PREFILL } }
                },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotContentTypeService, {
                    getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('prefills every field from the passed config', () => {
            expect(spectator.component['form'].getRawValue()).toEqual({
                portletName: 'Press Releases',
                portletId: 'c_press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'Card'
            });
            expect(spectator.component['isEdit']).toBe(true);
        });

        it('name changes no longer overwrite the id (touched-on-load)', () => {
            spectator.component['form'].controls.portletName.setValue('Renamed');
            spectator.component['onNameChange']('Renamed');
            expect(spectator.component['form'].controls.portletId.value).toBe('c_press-releases');
        });
    });

    describe('edit mode without prefill (catalog fallback)', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotToolsToolDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { tool: MOCK_TOOL } }
                },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotContentTypeService, {
                    getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('uses tool.title and tool.id when prefill is absent', () => {
            expect(spectator.component['form'].getRawValue().portletName).toBe('Press Releases');
            expect(spectator.component['form'].getRawValue().portletId).toBe('c_press-releases');
        });
    });
});
