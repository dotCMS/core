import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DotDownloadBundleDialogComponent } from './dot-download-bundle-dialog.component';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import {
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { Dropdown, DropdownModule, SelectButton, SelectButtonModule } from 'primeng/primeng';
import { DebugElement } from '@angular/core';
import * as dotUtils from '@shared/dot-utils';

const mockFilters: DotPushPublishFilter[] = [
    {
        defaultFilter: false,
        key: '1',
        title: 'Only Select items'
    },
    {
        defaultFilter: true,
        key: '2',
        title: 'Cotent, Assets and Page'
    },
    {
        defaultFilter: false,
        key: '3',
        title: 'Force Push'
    }
];

const BUNDLE_ID = 'XXZC4';

const DOWNLOAD_OPTIONS = [
    {
        label: 'Publish',
        value: 'publish'
    },
    {
        label: 'Unpublish',
        value: 'unpublish'
    }
];

const FILTERS_SORTED = [
    {
        label: 'Cotent, Assets and Page',
        value: '2'
    },
    {
        label: 'Force Push',
        value: '3'
    },
    {
        label: 'Only Select items',
        value: '1'
    }
];

const DOWNLOAD_URL =
    '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadUnpushedBundle/bundleId/';

describe('DotDownloadBundleDialogComponent', () => {
    let component: DotDownloadBundleDialogComponent;
    let fixture: ComponentFixture<DotDownloadBundleDialogComponent>;
    let dotDialogComponent: DotDialogComponent;
    let dotPushPublishFiltersService: DotPushPublishFiltersService;
    let dotDownloadBundleDialogService: DotDownloadBundleDialogService;

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

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotDownloadBundleDialogComponent],
            imports: [DotDialogModule, SelectButtonModule, DropdownModule],
            providers: [
                DotDownloadBundleDialogService,
                DotPushPublishFiltersService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotDownloadBundleDialogComponent);
        component = fixture.componentInstance;
        dotDialogComponent = fixture.debugElement.query(By.css('dot-dialog')).componentInstance;
        dotPushPublishFiltersService = fixture.debugElement.injector.get(
            DotPushPublishFiltersService
        );
        dotDownloadBundleDialogService = fixture.debugElement.injector.get(
            DotDownloadBundleDialogService
        );
        fixture.detectChanges();
    });

    it('should hide by default', () => {
        expect(dotDialogComponent.visible).toEqual(false);
    });

    it('should set correct header label', () => {
        expect(dotDialogComponent.header).toEqual('Download Bundle');
    });

    it('should hide error message by default', () => {
        const errorElement = fixture.debugElement.query(By.css('.download-bundle__error'));
        expect(errorElement).toBeNull();
    });

    describe('on showDialog', () => {
        let selectButton: SelectButton;

        beforeEach(() => {
            spyOn(dotPushPublishFiltersService, 'get').and.returnValue(of(mockFilters));
            dotDownloadBundleDialogService.open(BUNDLE_ID);
            fixture.detectChanges();
            selectButton = fixture.debugElement.query(By.css('p-selectButton')).componentInstance;
        });

        it('should set download options', () => {
            expect(selectButton.options).toEqual(DOWNLOAD_OPTIONS);
            expect(selectButton.value).toEqual(DOWNLOAD_OPTIONS[0].value);
        });

        it('should load filters list and set (default)', () => {
            expect(dotPushPublishFiltersService.get).toHaveBeenCalledTimes(1);
            expect(component.filterOptions).toEqual(FILTERS_SORTED);
        });

        it('should call filter endpoint just once', () => {
            component.close();
            dotDownloadBundleDialogService.open(BUNDLE_ID);
            expect(dotPushPublishFiltersService.get).toHaveBeenCalledTimes(1);
        });

        it('should load form values', () => {
            expect(component.form.value).toEqual({
                downloadOptionSelected: 'publish',
                filterKey: '2',
                bundleId: BUNDLE_ID
            });
        });

        describe('actions', () => {
            let dropdown: Dropdown;
            let buttons: DebugElement[];
            let unPublishButton;
            let cancelButton;
            let downloadButton;

            beforeEach(() => {
                dropdown = fixture.debugElement.query(By.css('p-dropdown')).componentInstance;
                buttons = fixture.debugElement.queryAll(By.css('.ui-selectbutton .ui-button'));
                unPublishButton = buttons[1].nativeElement;
                cancelButton = fixture.debugElement.query(By.css('.dialog__button-cancel'))
                    .nativeElement;
                downloadButton = fixture.debugElement.query(By.css('.dialog__button-accept'))
                    .nativeElement;
            });
            it('should disable filters dropdown when unpublish is selected', () => {
                unPublishButton.click();
                fixture.detectChanges();
                expect(dropdown.disabled).toEqual(true);
            });
            it('should enable filters when is publish again', () => {
                const publishButton = buttons[0].nativeElement;
                unPublishButton.click();
                fixture.detectChanges();
                publishButton.click();
                fixture.detectChanges();
                expect(dropdown.disabled).toEqual(false);
            });
            it('should close dialog on Cancel', () => {
                cancelButton.click();
                fixture.detectChanges();
                expect(dotDialogComponent.visible).toEqual(false);
            });
            it('should close dialog on hide Action', () => {
                dotDialogComponent.close();
                fixture.detectChanges();
                expect(dotDialogComponent.visible).toEqual(false);
                expect(component.showDialog).toEqual(false);
            });

            describe('on submit', () => {
                const blobMock = new Blob(['']);
                const fileName = 'asd-01EDSTVT6KGQ8CQ80PPA8717AN.tar.gz';
                const mockResponse = {
                    headers: {
                        get: (_header: string) => {
                            return `attachment; filename=${fileName}`;
                        }
                    },
                    blob: () => {
                        return blobMock;
                    }
                };
                let anchor: HTMLAnchorElement;

                beforeEach(() => {
                    spyOn(window, 'fetch').and.returnValue(Promise.resolve(mockResponse));
                    anchor = document.createElement('a');
                    spyOn(anchor, 'click');
                    spyOn(dotUtils, 'getDownloadLink').and.returnValue(anchor);

                });
                it('should disable buttons and change to label to downloading...', () => {
                    downloadButton.click();
                    fixture.detectChanges();
                    expect(downloadButton.disabled).toEqual(true);
                    expect(cancelButton.disabled).toEqual(true);
                });

                it(
                    'should fetch to the correct url when publish',
                    fakeAsync(() => {
                        downloadButton.click();
                        tick(1);
                        fixture.detectChanges();
                        expect(window.fetch).toHaveBeenCalledWith(
                            `${DOWNLOAD_URL}${BUNDLE_ID}/operation/publish/filterKey/2`
                        );
                        expect(dotUtils.getDownloadLink).toHaveBeenCalledWith(
                            blobMock,
                            fileName
                        );
                        expect(anchor.click).toHaveBeenCalledTimes(1);
                        expect(dotDialogComponent.visible).toEqual(false);
                    })
                );
                it('should set location to the correct url when unplublish', () => {
                    unPublishButton.click();
                    fixture.detectChanges();
                    downloadButton.click();
                    expect(window.fetch).toHaveBeenCalledWith(
                        `${DOWNLOAD_URL}${BUNDLE_ID}/operation/unpublish`
                    );
                });
            });

            describe('on error', () => {
                beforeEach(() => {
                    spyOn(window, 'fetch').and.returnValue(Promise.resolve(throwError('error')));
                });

                it(
                    'should enable buttons and display error message',
                    fakeAsync(() => {
                        downloadButton.click();
                        tick(1);
                        fixture.detectChanges();
                        expect(downloadButton.disabled).toEqual(false);
                        expect(cancelButton.disabled).toEqual(false);
                        const errorElement = fixture.debugElement.query(
                            By.css('.download-bundle__error')
                        );
                        expect(errorElement.nativeElement.innerText).toEqual(
                            'Error Building Bundle'
                        );
                    })
                );
            });
        });
    });
});
