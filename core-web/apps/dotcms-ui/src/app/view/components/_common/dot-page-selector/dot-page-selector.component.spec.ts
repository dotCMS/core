import { Observable, of as observableOf } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, Injectable, inject, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    NG_VALUE_ACCESSOR
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, createFakeEvent } from '@dotcms/utils-testing';

import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DotFolder, DotPageSelectorItem } from './models/dot-page-selector.models';
import { DotPageAsset, DotPageSelectorService } from './service/dot-page-selector.service';
import {
    expectedFolderMap,
    expectedPagesMap,
    expectedSitesMap
} from './service/dot-page-selector.service.spec';

import { DotDirectivesModule } from '../../../../shared/dot-directives.module';
import { DotFieldHelperComponent } from '../../dot-field-helper/dot-field-helper.component';

export const mockDotPageSelectorResults = {
    type: 'page',
    query: 'about-us',
    data: [
        {
            label: '//demo.dotcms.com/about-us',
            payload: {
                template: '8660b482-1ef6-4d00-9459-3996e703ba19',
                owner: 'dotcms.org.1',
                identifier: 'c12fe7e6-d338-49d5-973b-2d974d57015b',
                friendlyname: 'About Us',
                modDate: '2018-05-21 09:52:38.461',
                cachettl: '0',
                pagemetadata: 'dotCMS',
                languageId: 1,
                title: 'About Us',
                showOnMenu: 'true',
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a',
                seodescription: 'dotCMS Content Management System demo site - About Quest',
                folder: '1049e7fe-1553-4731-bdf9-ba069f1dc08b',
                __DOTNAME__: 'About Us',
                sortOrder: 0,
                path: '/about-us',
                seokeywords: 'dotCMS Content Management System',
                modUser: 'dotcms.org.1',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                lastReview: '2018-05-18 15:30:34.428',
                stInode: 'c541abb1-69b3-4bc5-8430-5e09e5239cc8',
                url: '/about-us/index',
                hostName: 'demo.dotcms.com'
            }
        }
    ]
};

@Injectable()
class MockDotPageSelectorService {
    getPageById(_param: string): Observable<DotPageSelectorItem> {
        return observableOf(mockDotPageSelectorResults.data[0]);
    }

    getPages(_param: string): Observable<DotPageSelectorItem[]> {
        return observableOf(expectedPagesMap);
    }

    getFolders(_param: string): Observable<DotPageSelectorItem[]> {
        return observableOf(expectedFolderMap);
    }

    getSites(_param: string): Observable<DotPageSelectorItem[]> {
        return observableOf(expectedSitesMap);
    }
}

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-page-selector
                [style]="{ width: '100%' }"
                formControlName="page"></dot-page-selector>
        </form>
    `,
    standalone: false
})
class FakeFormComponent {
    private fb = inject(UntypedFormBuilder);

    form: UntypedFormGroup;

    constructor() {
        /*
        This should go in the ngOnInit but I don't want to detectChanges everytime for
        this fake test component
    */
        this.form = this.fb.group({
            page: [{ value: 'c12fe7e6-d338-49d5-973b-2d974d57015b', disabled: false }]
        });
    }
}

const messageServiceMock = new MockDotMessageService({
    'page.selector.no.sites.results': 'Search for sites have no results',
    'page.selector.no.page.results': 'Search for pages have no results',
    'page.selector.no.folder.results': 'Search for folders have no results',
    'page.selector.placeholder': 'Start typing for suggestions',
    'page.selector.folder.hint': 'Folder hint',
    'page.selector.hint': 'Page hint',
    'page.selector.folder.permissions': 'Folder Permissions',
    'page.selector.folder.new': 'new folder'
});

let hostDe: DebugElement;
let component: DotPageSelectorComponent;
let de: DebugElement;
let autocomplete: DebugElement;
let dotPageSelectorService: DotPageSelectorService;

describe('DotPageSelectorComponent', () => {
    let hostFixture: ComponentFixture<FakeFormComponent>;
    const searchPageObj = { originalEvent: { target: { value: 'demo' } }, query: 'demo' };
    const searchFolderObj = { originalEvent: { target: { value: 'folder' } }, query: 'folder' };
    const invalidSearchPageObj = { originalEvent: { target: { value: 'd' } }, query: 'd' };
    const searchHostObj = { originalEvent: { target: { value: '//' } }, query: '//' };
    const specialSearchObj = {
        originalEvent: { target: { value: 'd#e mo$%' } },
        query: 'd#e mo$%'
    };
    const whiteSpaceHosts = {
        originalEvent: { target: { value: '//new site' } },
        query: '//new site'
    };
    const fullSearchObj = {
        originalEvent: { target: { value: '//demo/folder' } },
        query: '//demo/folder'
    };
    const completeHostSearch = {
        originalEvent: { target: { value: '//demo/' } },
        query: '//demo/'
    };

    beforeEach(waitForAsync(() => {
        // Mock matchMedia for PrimeNG components - needs to be set before TestBed configuration
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });

        TestBed.configureTestingModule({
            declarations: [FakeFormComponent],
            imports: [
                DotPageSelectorComponent,
                DotDirectivesModule,
                DotFieldHelperComponent,
                DotSafeHtmlPipe,
                DotMessagePipe,
                AutoCompleteModule,
                FormsModule,
                CommonModule,
                ReactiveFormsModule,
                BrowserAnimationsModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        })
            .overrideComponent(DotPageSelectorComponent, {
                set: {
                    providers: [
                        {
                            multi: true,
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => DotPageSelectorComponent)
                        },
                        { provide: DotPageSelectorService, useClass: MockDotPageSelectorService }
                    ]
                }
            })
            .compileComponents();
    }));

    beforeEach(async () => {
        hostFixture = TestBed.createComponent(FakeFormComponent);
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-page-selector'));
        component = de.componentInstance;
        dotPageSelectorService = de.injector.get(DotPageSelectorService);

        jest.spyOn(component.selected, 'emit');
        jest.spyOn(component, 'writeValue');

        hostFixture.detectChanges();
        await hostFixture.whenStable();
        autocomplete = de.query(By.css('[data-testId="p-autoComplete"]'));
    });

    describe('AutoComplete properties', () => {
        it('should have placeholder', () => {
            const input: HTMLInputElement = de.query(By.css('.p-autocomplete-input')).nativeElement;
            expect(input.placeholder).toEqual('Start typing for suggestions');
        });
    });

    describe('Search Types', () => {
        it('should search for pages', () => {
            jest.spyOn(dotPageSelectorService, 'getPages');
            autocomplete.triggerEventHandler('completeMethod', searchPageObj);
            expect(dotPageSelectorService.getPages).toHaveBeenCalledWith(searchPageObj.query);
            expect(dotPageSelectorService.getPages).toHaveBeenCalledTimes(1);
        });

        it('should not search for pages if has less than 2 characters', () => {
            jest.spyOn(dotPageSelectorService, 'getPages');
            autocomplete.triggerEventHandler('completeMethod', invalidSearchPageObj);
            expect(dotPageSelectorService.getPages).not.toHaveBeenCalled();
        });

        it('should search for host', () => {
            jest.spyOn(dotPageSelectorService, 'getSites');
            autocomplete.triggerEventHandler('completeMethod', searchHostObj);
            expect(dotPageSelectorService.getSites).toHaveBeenCalledWith('');
            expect(dotPageSelectorService.getSites).toHaveBeenCalledTimes(1);
        });

        it('should allow white spaces in host', () => {
            jest.spyOn(dotPageSelectorService, 'getSites');
            autocomplete.triggerEventHandler('completeMethod', whiteSpaceHosts);
            expect(dotPageSelectorService.getSites).toHaveBeenCalledWith('new site');
            expect(dotPageSelectorService.getSites).toHaveBeenCalledTimes(1);
        });

        it('should search for pages when the host is complete', () => {
            jest.spyOn(dotPageSelectorService, 'getSites');
            jest.spyOn(dotPageSelectorService, 'getPages');
            autocomplete.triggerEventHandler('completeMethod', completeHostSearch);
            expect(dotPageSelectorService.getSites).toHaveBeenCalledWith('demo', true);
            expect(dotPageSelectorService.getSites).toHaveBeenCalledTimes(1);
            expect(dotPageSelectorService.getPages).toHaveBeenCalledWith('//demo/');
            expect(dotPageSelectorService.getPages).toHaveBeenCalledTimes(1);
        });

        it('should remove special characters when searching for pages', () => {
            jest.spyOn(dotPageSelectorService, 'getPages');
            autocomplete.triggerEventHandler('completeMethod', specialSearchObj);
            expect(dotPageSelectorService.getPages).toHaveBeenCalledWith('demo');
            expect(dotPageSelectorService.getPages).toHaveBeenCalledTimes(1);
        });

        it('should display error when no results in pages', () => {
            jest.spyOn(dotPageSelectorService, 'getPages').mockReturnValue(observableOf([]));
            autocomplete.triggerEventHandler('completeMethod', {
                originalEvent: { target: { value: 'invalidPage' } },
                query: 'invalidPage'
            });
            hostFixture.detectChanges();
            const message = de.query(By.css('[data-testId="message"]'));
            expect(message.nativeNode.textContent).toEqual('Search for pages have no results');
            expect(message.nativeNode).toHaveClass('p-invalid');
        });

        it('should display error when no results in hosts', () => {
            jest.spyOn(dotPageSelectorService, 'getSites').mockReturnValue(observableOf([]));
            autocomplete.triggerEventHandler('completeMethod', {
                originalEvent: { target: { value: '//invalid' } },
                query: '//invalid'
            });
            hostFixture.detectChanges();
            const message = de.query(By.css('[data-testId="message"]'));
            expect(message.nativeNode.textContent).toEqual('Search for sites have no results');
            expect(message.nativeNode).toHaveClass('p-invalid');
        });

        describe('folder search ', () => {
            beforeEach(() => {
                component.folderSearch = true;
                hostFixture.detectChanges();
            });

            it('should search for folders', () => {
                jest.spyOn(dotPageSelectorService, 'getFolders');
                autocomplete.triggerEventHandler('completeMethod', searchFolderObj);
                expect(dotPageSelectorService.getFolders).toHaveBeenCalledWith(
                    searchFolderObj.query
                );
            });

            it('should show message new folder will be created', () => {
                jest.spyOn(dotPageSelectorService, 'getSites');
                jest.spyOn(dotPageSelectorService, 'getFolders').mockReturnValue(observableOf([]));
                autocomplete.triggerEventHandler('completeMethod', fullSearchObj);
                hostFixture.detectChanges();
                const message = de.query(By.css('[data-testId="message"]'));
                expect(message.nativeNode.textContent).toEqual('new folder');
                expect(message.nativeNode).toHaveClass('p-info');
            });

            it('should show message of permissions', () => {
                jest.spyOn(dotPageSelectorService, 'getFolders');
                autocomplete.triggerEventHandler('completeMethod', searchFolderObj);
                autocomplete.triggerEventHandler('onSelect', {
                    originalEvent: createFakeEvent('onSelect'),
                    value: expectedFolderMap[1]
                });
                hostFixture.detectChanges();
                const message = de.query(By.css('[data-testId="message"]'));
                expect(message.nativeNode.textContent).toEqual('Folder Permissions');
                expect(message.nativeNode).toHaveClass('p-invalid');
            });

            it('should display error when no results in folders', () => {
                jest.spyOn(dotPageSelectorService, 'getFolders').mockReturnValue(observableOf([]));
                autocomplete.triggerEventHandler('completeMethod', {
                    originalEvent: { target: { value: 'invalid' } },
                    query: 'invalid'
                });
                hostFixture.detectChanges();
                const message = de.query(By.css('[data-testId="message"]'));
                expect(message.nativeNode.textContent).toEqual(
                    'Search for folders have no results'
                );
                expect(message.nativeNode).toHaveClass('p-invalid');
            });
        });
    });

    describe('ControlValueAccessor', () => {
        beforeEach(() => {
            jest.spyOn(component, 'propagateChange');
        });

        it('should emit selected page and propagate changes', () => {
            jest.spyOn(dotPageSelectorService, 'getPages');
            autocomplete.triggerEventHandler('completeMethod', searchPageObj);
            autocomplete.triggerEventHandler('onSelect', {
                originalEvent: createFakeEvent('onSelect'),
                value: expectedPagesMap[0]
            });
            expect(component.selected.emit).toHaveBeenCalledWith(
                expectedPagesMap[0].payload as DotPageAsset
            );
            expect(component.propagateChange).toHaveBeenCalledWith(
                (expectedPagesMap[0].payload as DotPageAsset).identifier
            );
        });

        it('should write value', () => {
            expect(component.writeValue).toHaveBeenCalledWith(
                'c12fe7e6-d338-49d5-973b-2d974d57015b'
            );
            expect((component.val.payload as DotPageAsset).identifier).toEqual(
                'c12fe7e6-d338-49d5-973b-2d974d57015b'
            );
        });

        it('should clear model and suggestions', () => {
            component.suggestions$.subscribe((value) => {
                expect(value).toEqual([]);
            });
            autocomplete.triggerEventHandler('onClear', {});
            expect(component.propagateChange).toHaveBeenCalledWith(null);
            expect(component.propagateChange).toHaveBeenCalledTimes(1);
        });

        it('should emit selected folder and propagate changes', () => {
            component.folderSearch = true;
            const folder = <DotFolder>expectedFolderMap[0].payload;
            jest.spyOn(dotPageSelectorService, 'getFolders');
            autocomplete.triggerEventHandler('completeMethod', searchFolderObj);
            autocomplete.triggerEventHandler('onSelect', {
                originalEvent: createFakeEvent('onSelect'),
                value: expectedFolderMap[0]
            });
            expect(component.selected.emit).toHaveBeenCalledWith(
                `//${folder.hostName}${folder.path}`
            );
            expect(component.propagateChange).toHaveBeenCalledWith(
                `//${folder.hostName}${folder.path}`
            );
        });
    });
});
