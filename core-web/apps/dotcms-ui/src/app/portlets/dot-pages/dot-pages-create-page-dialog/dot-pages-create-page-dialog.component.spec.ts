import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    ActivatedRouteMock,
    CoreWebServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotPagesCreatePageDialogComponent } from './dot-pages-create-page-dialog.component';

import { DotPageStore } from '../dot-pages-store/dot-pages.store';

const mockContentType: DotCMSContentType = {
    baseType: 'CONTENT',
    nEntries: 23,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'SYSTEM_HOST',
    iDate: 1667904275000,
    icon: 'event_note',
    id: 'ce930143870e11569f93f8a9fff5da19',
    layout: [],
    modDate: 1667904276000,
    multilingualable: false,
    name: 'Dot Favorite Page',
    system: false,
    systemActionMappings: {},
    variable: 'dotFavoritePage',
    versionable: true,
    workflows: []
};

class storeMock {
    get vm$() {
        return of({
            favoritePages: {
                items: [],
                showLoadMoreButton: false,
                total: 0
            },
            isEnterprise: true,
            environments: true,
            languages: [],
            loggedUser: {
                id: 'admin',
                canRead: { contentlets: true, htmlPages: true },
                canWrite: { contentlets: true, htmlPages: true }
            },
            pages: {
                actionMenuDomId: '',
                items: [],
                addToBundleCTId: 'test1'
            },
            languageOptions: [
                { label: 'En-en', value: 1 },
                { label: 'ES-es', value: 2 }
            ],
            languageLabels: { 1: 'En-en', 2: 'Es-es' },
            pageTypes: [mockContentType]
        });
    }

    getPageTypes(): void {
        /* */
    }
}

describe('DotPagesCreatePageDialogComponent', () => {
    let fixture: ComponentFixture<DotPagesCreatePageDialogComponent>;
    let de: DebugElement;
    let dialogRef: DynamicDialogRef;
    let dotRouterService: DotRouterService;
    let store: DotPageStore;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotPagesCreatePageDialogComponent, HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            contentTypeVariable: 'contentType',
                            onSave: jasmine.createSpy()
                        }
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: [{ ...mockContentType }]
                    }
                },
                { provide: DotPageStore, useClass: storeMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                { provide: DotRouterService, useClass: MockDotRouterService }
            ]
        }).compileComponents();

        store = TestBed.inject(DotPageStore);
        spyOn(store, 'getPageTypes');
        fixture = TestBed.createComponent(DotPagesCreatePageDialogComponent);
        de = fixture.debugElement;
        dotRouterService = TestBed.inject(DotRouterService);
        dialogRef = TestBed.inject(DynamicDialogRef);
        fixture.detectChanges();
    });

    it('should have html components with attributes', () => {
        expect(
            de.query(By.css(`[data-testId="dot-pages-create-page-filter-icon"]`)).componentInstance
                .name
        ).toBe('search');
        expect(
            de.query(By.css(`[data-testId="dot-pages-create-page-filter-icon"]`)).componentInstance
                .size
        ).toBe('24');
        expect(
            de.query(By.css(`[data-testId="dot-pages-create-page-dialog__keyword-input"]`))
                .attributes.placeholder
        ).toBe('Search');
        expect(
            de.query(By.css(`[data-testId="dot-pages-create-page-dialog__keyword-input"]`))
                .attributes.dotAutofocus
        ).toBeDefined();

        expect(
            de.query(By.css(`.dot-pages-create-page-dialog__page-item dot-icon`)).componentInstance
                .name
        ).toBe(mockContentType.icon);
        expect(
            de.query(By.css(`.dot-pages-create-page-dialog__page-item dot-icon`)).componentInstance
                .size
        ).toBe('18');
    });

    it('should set pages types data when init', () => {
        fixture.componentInstance.pageTypes$.subscribe((data) => {
            expect(data).toEqual([{ ...mockContentType }]);
        });
    });

    it('should redirect url when click on page', () => {
        const pageType = de.query(By.css(`.dot-pages-create-page-dialog__page-item`));
        pageType.triggerEventHandler('click', mockContentType.variable);
        expect(dotRouterService.goToURL).toHaveBeenCalledWith(
            `/pages/new/${mockContentType.variable}`
        );
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should call App filter on search', () => {
        const input = de.query(
            By.css(`[data-testId="dot-pages-create-page-dialog__keyword-input"]`)
        );
        input.nativeElement.value = 'Dot Favorite Page';
        input.nativeElement.dispatchEvent(new Event('keyup'));
        fixture.componentInstance.pageTypes$.subscribe((data) => {
            expect(data).toEqual([{ ...mockContentType }]);
        });
    });
});
