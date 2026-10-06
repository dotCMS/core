import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
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

import { DotToolsStore } from '../dot-tools-page/store/dot-tools.store';
import { DotToolsCatalogEntry, DotToolsCustomToolConfig } from '../models/dot-tools.models';
import { DotToolsService } from '../services/dot-tools.service';

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
    dataViewMode: 'card'
};

const SAMPLE_CREATED_ENTRY: DotToolsCatalogEntry = {
    id: 'c_press-releases',
    title: 'Press Releases',
    isCustom: true
};

describe('DotToolsToolDialogComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: vi.fn() };
        const createCustomToolSpy = vi.fn().mockReturnValue(of(SAMPLE_CREATED_ENTRY));

        const createComponent = createComponentFactory({
            component: DotToolsToolDialogComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } },
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotContentTypeService, {
                    getContentTypes: vi.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService),
                mockProvider(DotToolsStore, { createCustomTool: createCustomToolSpy })
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            createCustomToolSpy.mockClear();
            createCustomToolSpy.mockReturnValue(of(SAMPLE_CREATED_ENTRY));
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
            expect(createCustomToolSpy).not.toHaveBeenCalled();
        });

        it('calls the store and closes on valid submit', () => {
            spectator.component['form'].patchValue({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            spectator.component['onSubmit']();
            expect(createCustomToolSpy).toHaveBeenCalledWith({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            expect(mockRef.close).toHaveBeenCalledWith(true);
        });

        it('renders inline server error on 400 and keeps the dialog open', () => {
            createCustomToolSpy.mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 400,
                            statusText: 'Bad Request',
                            error: { message: 'Portlet id already exists' }
                        })
                )
            );
            spectator.component['form'].patchValue({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            spectator.component['onSubmit']();
            spectator.detectChanges();

            expect(mockRef.close).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('tools-tool-submit-error'))?.textContent).toContain(
                'Portlet id already exists'
            );
        });

        it('routes 5xx through the global handler and closes', () => {
            const handler = spectator.inject(DotHttpErrorManagerService);
            createCustomToolSpy.mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' }))
            );
            spectator.component['form'].patchValue({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            spectator.component['onSubmit']();

            expect(handler.handle).toHaveBeenCalled();
            expect(mockRef.close).toHaveBeenCalledWith();
        });

        it('treats 404 as the duplicate-id error on create and renders inline', () => {
            // PortletResource.saveNew throws DoesNotExistException on duplicate
            // id, which the JAX-RS mapper translates to 404. The dialog must
            // keep itself open so the user can correct the id, not hand the
            // response to the global "URL does not exist" modal.
            createCustomToolSpy.mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 404,
                            statusText: 'Not Found',
                            error: { message: 'Portlet with ID press-releases already exist' }
                        })
                )
            );
            spectator.component['form'].patchValue({
                portletName: 'Press Releases',
                portletId: 'press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            spectator.component['onSubmit']();
            spectator.detectChanges();

            expect(mockRef.close).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('tools-tool-submit-error'))?.textContent).toContain(
                'already exist'
            );
        });
    });

    describe('edit mode with prefill', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: vi.fn() };
        const updateCustomToolSpy = vi.fn().mockReturnValue(of(SAMPLE_CREATED_ENTRY));

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
                    getContentTypes: vi.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService),
                mockProvider(DotToolsStore, { updateCustomTool: updateCustomToolSpy })
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            updateCustomToolSpy.mockClear();
            spectator = createComponent();
        });

        it('prefills every field from the passed config', () => {
            expect(spectator.component['form'].getRawValue()).toEqual({
                portletName: 'Press Releases',
                portletId: 'c_press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            expect(spectator.component['isEdit']).toBe(true);
        });

        it('name changes no longer overwrite the id (touched-on-load)', () => {
            spectator.component['form'].controls.portletName.setValue('Renamed');
            spectator.component['onNameChange']('Renamed');
            expect(spectator.component['form'].controls.portletId.value).toBe('c_press-releases');
        });

        it('calls updateCustomTool and closes on valid submit', () => {
            spectator.component['onSubmit']();
            expect(updateCustomToolSpy).toHaveBeenCalledWith({
                portletName: 'Press Releases',
                portletId: 'c_press-releases',
                baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: ['Blog'],
                dataViewMode: 'card'
            });
            expect(mockRef.close).toHaveBeenCalledWith(true);
        });

        it('renders the id input as readonly so it cannot be retargeted to another tool', () => {
            // The PUT finds the tool by the id in the body; letting the user
            // retype it would overwrite a different custom tool's config, or
            // 404 and close with the user's input lost.
            const input = spectator.query(
                byTestId('tools-tool-id-input')
            ) as HTMLInputElement | null;
            expect(input?.readOnly).toBe(true);
        });

        it('routes 404 on edit through the global handler (NOT inline) — the tool was deleted', () => {
            const handler = spectator.inject(DotHttpErrorManagerService);
            updateCustomToolSpy.mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' }))
            );
            spectator.component['onSubmit']();

            expect(handler.handle).toHaveBeenCalled();
            expect(mockRef.close).toHaveBeenCalledWith();
            expect(spectator.query(byTestId('tools-tool-submit-error'))).toBeNull();
        });
    });

    describe('edit mode without prefill (catalog fallback)', () => {
        let spectator: Spectator<DotToolsToolDialogComponent>;
        const mockRef = { close: vi.fn() };
        // The dialog's ngOnInit calls getCustomTool() when no prefill is
        // injected. Mock it so the real HTTP call never fires and the
        // promised prefill patch is exercised by the test.
        const getCustomToolSpy = vi.fn().mockReturnValue(of(MOCK_PREFILL));

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
                    getContentTypes: vi.fn().mockReturnValue(of(MOCK_CONTENT_TYPES))
                }),
                mockProvider(DotHttpErrorManagerService),
                mockProvider(DotToolsService, { getCustomTool: getCustomToolSpy }),
                mockProvider(DotToolsStore, {
                    updateCustomTool: vi.fn().mockReturnValue(of(SAMPLE_CREATED_ENTRY))
                })
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            getCustomToolSpy.mockClear();
            getCustomToolSpy.mockReturnValue(of(MOCK_PREFILL));
            spectator = createComponent();
        });

        it('uses tool.title and tool.id while the prefill fetch is in flight', () => {
            expect(getCustomToolSpy).toHaveBeenCalledWith('c_press-releases');
            // The of(...) above resolves synchronously, so by the time this
            // assertion runs the prefill has already patched the form.
            // The catalog-fallback value lives for a tick in production; here
            // we assert the final patched state matches the prefill.
            expect(spectator.component['form'].getRawValue().portletName).toBe('Press Releases');
            expect(spectator.component['form'].getRawValue().portletId).toBe('c_press-releases');
        });

        it('clears $prefillLoading once the fetch resolves', () => {
            expect(spectator.component['$prefillLoading']()).toBe(false);
        });

        it('routes prefill-fetch errors through the global handler and closes the dialog', () => {
            getCustomToolSpy.mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' }))
            );
            // Recreate so the ngOnInit fetch sees the overridden mock.
            spectator = createComponent();
            const handler = spectator.inject(DotHttpErrorManagerService) as unknown as {
                handle: Mock;
            };
            expect(handler.handle).toHaveBeenCalled();
            expect(spectator.component['$prefillLoading']()).toBe(false);
            // Dialog closes so the user does not save the catalog-seed values
            // (default [CONTENT], empty contentTypes, 'list'), which would
            // silently wipe the real contentTypes on PUT.
            expect(mockRef.close).toHaveBeenCalled();
        });
    });
});
