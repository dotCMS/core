import { of as observableOf } from 'rxjs';
import { CONTAINER_SOURCE, DotContainer } from '@models/container/dot-container.model';
import { By } from '@angular/platform-browser';
import { PaginatorService } from '@services/paginator/paginator.service';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { ComponentFixture, fakeAsync, tick, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotContainerSelectorComponent } from './dot-container-selector.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { FormsModule } from '@angular/forms';
import {
    CoreWebService,
    ApiRoot,
    UserModel,
    LoggerService,
    StringUtils,
    BrowserUtil
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '@components/_common/searchable-dropdown/component';

describe('ContainerSelectorComponent', () => {
    let fixture: ComponentFixture<DotContainerSelectorComponent>;
    let comp: DotContainerSelectorComponent;
    let de: DebugElement;
    let searchableDropdownComponent;
    let containers: DotContainer[];
    let paginatorService: PaginatorService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            addcontainer: 'Add a Container'
        });

        TestBed.configureTestingModule({
            declarations: [DotContainerSelectorComponent],
            imports: [
                SearchableDropDownModule,
                BrowserAnimationsModule,
                CommonModule,
                FormsModule,
                ButtonModule,
                DotPipesModule,
                HttpClientTestingModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                BrowserUtil,
                IframeOverlayService,
                PaginatorService,
                DotTemplateContainersCacheService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                PaginatorService
            ]
        }).compileComponents();

        containers = [
            {
                categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e',
                deleted: false,
                friendlyName: 'Friendly Container name',
                identifier: '427c47a4-c380-439f',
                name: 'Container 1',
                type: 'Container',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            },
            {
                categoryId: '40204d-c380-439f-a6d0-97d8sdeed57e',
                deleted: false,
                friendlyName: 'Friendly Container2 name',
                identifier: '427c47a4-c380-439f',
                name: 'Container 2',
                type: 'Container',
                source: CONTAINER_SOURCE.FILE,
                path: 'container/path',
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        ];

        fixture = TestBed.createComponent(DotContainerSelectorComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        paginatorService = de.injector.get(PaginatorService);
        searchableDropdownComponent = de.query(By.css('dot-searchable-dropdown')).componentInstance;
    });

    it('should set onInit Pagination Service with right values', () => {
        spyOn(paginatorService, 'setExtraParams');
        comp.ngOnInit();
        expect(paginatorService.setExtraParams).toHaveBeenCalled();
    });

    it('should pass all the right attr', () => {
        fixture.detectChanges();
        const searchable = de.query(By.css('[data-testId="searchableDropdown"]'));
        expect(searchable.attributes).toEqual(
            jasmine.objectContaining({
                'ng-reflect-label-property-name': 'name,parentPermissionable.host',
                'ng-reflect-multiple': 'true',
                'ng-reflect-page-link-size': '5',
                'ng-reflect-persistent-placeholder': 'true',
                'ng-reflect-placeholder': 'editpage.container.add.label',
                'ng-reflect-rows': '5',
                'ng-reflect-width': '172px',
                overlayWidth: '440px',
                persistentPlaceholder: 'true',
                width: '172px'
            })
        );
    });

    it('should change Page', fakeAsync(() => {
        const filter = 'filter';

        const page = 1;

        fixture.detectChanges();

        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));

        fixture.detectChanges();

        searchableDropdownComponent.pageChange.emit({
            filter: filter,
            first: 10,
            page: page,
            pageCount: 10,
            rows: 0
        });

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
    }));

    it('should paginate when the filter change', fakeAsync(() => {
        const filter = 'filter';

        fixture.detectChanges();

        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));

        fixture.detectChanges();

        searchableDropdownComponent.filterChange.emit(filter);

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    }));

    it('should set container list replacing the identifier for the path, if needed', () => {
        fixture.detectChanges();
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf(containers));
        const searchable: SearchableDropdownComponent = de.query(
            By.css('[data-testId="searchableDropdown"]')
        ).componentInstance;
        searchable.pageChange.emit({ filter: '', first: 0 } as PaginationEvent);
        fixture.detectChanges();
        expect(searchable.data[0].identifier).toEqual('427c47a4-c380-439f');
        expect(searchable.data[1].identifier).toEqual('container/path');
    });
});
