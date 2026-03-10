/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';

import {
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import * as dotUtils from '@dotcms/utils/lib/dot-utils';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotDownloadBundleDialogComponent } from './dot-download-bundle-dialog.component';

import { DotDownloadBundleDialogService } from '../../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

const mockFilters: DotPushPublishFilter[] = [
    { defaultFilter: false, key: '1', title: 'Only Select items' },
    { defaultFilter: true, key: '2', title: 'Cotent, Assets and Page' },
    { defaultFilter: false, key: '3', title: 'Force Push' }
];

const BUNDLE_ID = 'XXZC4';

const DOWNLOAD_OPTIONS = [
    { label: 'Publish', value: 'publish' },
    { label: 'Unpublish', value: 'unpublish' }
];

const FILTERS_SORTED = [
    { label: 'Cotent, Assets and Page', value: '2' },
    { label: 'Force Push', value: '3' },
    { label: 'Only Select items', value: '1' }
];

const messageServiceMock = new MockDotMessageService({
    'download.bundle.header': 'Download Bundle',
    'download.bundle.filter': 'Filter',
    'download.bundle.i.want': 'I want to Download for',
    'download.bundle.publish': 'Publish',
    'download.bundle.unPublish': 'Unpublish',
    'download.bundle.download': 'Download',
    'download.bundle.downloading': 'Downloading...',
    'dot.common.cancel': 'Cancel',
    'download.bundle.error': 'Error Building Bundle'
});

const mockFiltersService = {
    get: () => of(mockFilters)
};

describe('DotDownloadBundleDialogComponent', () => {
    let spectator: Spectator<DotDownloadBundleDialogComponent>;
    let component: DotDownloadBundleDialogComponent;
    let dotDownloadBundleDialogService: DotDownloadBundleDialogService;

    const createComponent = createComponentFactory({
        component: DotDownloadBundleDialogComponent,
        imports: [SelectButtonModule, SelectModule, NoopAnimationsModule, DotMessagePipe],
        providers: [
            DotDownloadBundleDialogService,
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        componentProviders: [
            { provide: DotPushPublishFiltersService, useValue: mockFiltersService }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        jest.spyOn(mockFiltersService, 'get');
        spectator = createComponent();
        component = spectator.component;
        dotDownloadBundleDialogService = spectator.inject(DotDownloadBundleDialogService);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should hide by default', () => {
        expect(component.showDialog).toBe(false);
    });

    it('should set correct header label', () => {
        spectator.detectChanges();
        const dialogEl = spectator.query('p-dialog');
        expect(dialogEl).toBeTruthy();
        expect(component.showDialog).toBe(false);
    });

    it('should hide error message by default', () => {
        const errorElement = spectator.query('.download-bundle__error');
        expect(errorElement).toBeNull();
    });

    describe('on showDialog', () => {
        beforeEach(fakeAsync(() => {
            spectator.fixture.autoDetectChanges(true);
            dotDownloadBundleDialogService.open(BUNDLE_ID);
            tick(0);
            tick(0);
        }));

        it('should set download options', () => {
            expect(component.downloadOptions).toEqual(DOWNLOAD_OPTIONS);
            expect(component.form.get('downloadOptionSelected')?.value).toEqual(
                DOWNLOAD_OPTIONS[0].value
            );
        });

        it('should load filters list and set (default)', () => {
            expect(mockFiltersService.get).toHaveBeenCalledTimes(1);
            expect(component.filterOptions).toEqual(FILTERS_SORTED);
        });

        it('should call filter endpoint just once', () => {
            component.close();
            dotDownloadBundleDialogService.open(BUNDLE_ID);
            expect(mockFiltersService.get).toHaveBeenCalledTimes(1);
        });

        it('should load form values', () => {
            expect(component.form.value).toEqual({
                downloadOptionSelected: 'publish',
                filterKey: '2',
                bundleId: BUNDLE_ID
            });
        });

        describe('actions', () => {
            it('should disable filters dropdown when unpublish is selected', () => {
                component.form.patchValue({ downloadOptionSelected: 'unpublish' });
                expect(component.form.get('filterKey')?.disabled).toBe(true);
            });

            it('should enable filters when is publish again', () => {
                component.form.patchValue({ downloadOptionSelected: 'unpublish' });
                component.form.patchValue({ downloadOptionSelected: 'publish' });
                expect(component.form.get('filterKey')?.disabled).toBe(false);
            });

            it('should close dialog on Cancel', () => {
                component.dialogActions.cancel.action();
                expect(component.showDialog).toBe(false);
            });

            it('should close dialog on hide Action', () => {
                component.close();
                expect(component.showDialog).toBe(false);
            });

            describe('on submit', () => {
                const blobMock = new Blob(['']);
                const fileName = 'asd-01EDSTVT6KGQ8CQ80PPA8717AN.tar.gz';
                const mockResponse = {
                    headers: {
                        get: (_header: string) => `attachment; filename=${fileName}`
                    },
                    blob: () => blobMock
                };
                let anchor: HTMLAnchorElement;

                beforeEach(() => {
                    (window as any).fetch = jest
                        .fn()
                        .mockReturnValue(Promise.resolve(mockResponse));
                    anchor = document.createElement('a');
                    jest.spyOn(anchor, 'click');
                    jest.spyOn(dotUtils, 'getDownloadLink').mockReturnValue(anchor);
                });

                it('should disable buttons and change to label to downloading...', () => {
                    component.handleSubmit();
                    expect(component.dialogActions.accept.disabled).toBe(true);
                    expect(component.dialogActions.cancel.disabled).toBe(true);
                    expect(component.dialogActions.accept.label).toBe('Downloading...');
                });

                it('should fetch to the correct url when publish', fakeAsync(() => {
                    (dotUtils.getDownloadLink as jest.Mock).mockClear();

                    component.handleSubmit();
                    tick(0);
                    tick(100);

                    expect((window as any).fetch).toHaveBeenCalledWith(`/api/bundle/_generate`, {
                        method: 'POST',
                        mode: 'cors',
                        cache: 'no-cache',
                        headers: { 'Content-Type': 'application/json' },
                        body: '{"bundleId":"XXZC4","operation":"0","filterKey":"2"}'
                    });
                    expect(dotUtils.getDownloadLink).toHaveBeenCalledWith(blobMock, fileName);
                    expect(dotUtils.getDownloadLink).toHaveBeenCalledTimes(1);
                    expect(anchor.click).toHaveBeenCalledTimes(1);
                    expect(component.showDialog).toBe(false);
                }));

                it('should set location to the correct url when unplublish', fakeAsync(() => {
                    component.form.patchValue({ downloadOptionSelected: 'unpublish' });
                    component.form.get('filterKey')?.disable();
                    component.handleSubmit();
                    tick(0);
                    expect((window as any).fetch).toHaveBeenCalledWith(`/api/bundle/_generate`, {
                        method: 'POST',
                        mode: 'cors',
                        cache: 'no-cache',
                        headers: { 'Content-Type': 'application/json' },
                        body: '{"bundleId":"XXZC4","operation":"1"}'
                    });
                }));
            });

            describe('on error', () => {
                beforeEach(() => {
                    (window as any).fetch = jest
                        .fn()
                        .mockReturnValue(Promise.reject(new Error('error')));
                });

                it('should enable buttons and display error message', fakeAsync(() => {
                    component.handleSubmit();
                    tick(0);
                    tick(100);
                    expect(component.dialogActions?.accept?.disabled).toBe(false);
                    expect(component.dialogActions?.cancel?.disabled).toBe(false);
                    expect(component.errorMessage).toBe('Error Building Bundle');
                }));
            });
        });
    });
});
