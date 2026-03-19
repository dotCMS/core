import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of as observableOf } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    LoggerService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { CONTAINER_SOURCE, DotContainer } from '@dotcms/dotcms-models';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContainerSelectorComponent } from './dot-container-selector.component';

import { DotTemplateContainersCacheService } from '../../../api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '../_common/searchable-dropdown/component/searchable-dropdown.component';

describe('ContainerSelectorComponent', () => {
    let spectator: Spectator<DotContainerSelectorComponent>;
    let searchableDropdownComponent: SearchableDropdownComponent | null;
    let containers: DotContainer[];
    let paginatorService: PaginatorService;

    const messageServiceMock = new MockDotMessageService({
        addcontainer: 'Add a Container'
    });

    const createComponent = createComponentFactory({
        component: DotContainerSelectorComponent,
        detectChanges: false,
        imports: [BrowserAnimationsModule, HttpClientTestingModule],
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
            StringUtils
        ]
    });

    beforeEach(() => {
        containers = [
            {
                categoryId: '427c47a4-c380-439f-a6d0-97d81deed57e',
                deleted: false,
                friendlyName: 'Friendly Container name',
                identifier: '427c47a4-c380-439f',
                name: 'Container 1',
                type: 'Container',
                source: CONTAINER_SOURCE.DB,
                hostName: 'demo.dotcms.com'
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
                hostName: 'demo.dotcms.com'
            }
        ];

        spectator = createComponent();
        paginatorService = spectator.component.paginationService;
        searchableDropdownComponent = null;
    });

    it('should set onInit Pagination Service with right values', () => {
        jest.spyOn(paginatorService, 'setExtraParams');
        spectator.component.ngOnInit();
        expect(paginatorService.setExtraParams).toHaveBeenCalled();
    });

    it('should pass all the right attr', () => {
        spectator.detectChanges();
        const searchable = spectator.debugElement.query(
            By.css('[data-testid="searchableDropdown"]')
        );
        const searchableComponent = searchable.componentInstance as SearchableDropdownComponent;

        expect(searchableComponent.labelPropertyName).toEqual(['name', 'hostName']);
        expect(searchableComponent.multiple).toBe(true);
        expect(searchableComponent.pageLinkSize).toBe(5);
        expect(searchableComponent.persistentPlaceholder).toBeTruthy();
        expect(searchableComponent.placeholder).toBe('editpage.container.add.label');
        expect(searchableComponent.rows).toBe(5);
        expect(searchableComponent.width).toBe('fit-content');

        expect(searchable.attributes).toEqual(
            expect.objectContaining({
                overlayWidth: '440px',
                persistentPlaceholder: 'true',
                width: 'fit-content'
            })
        );
    });

    it('should change Page', fakeAsync(() => {
        const filter = 'filter';
        const page = 1;

        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf([]));
        spectator.detectChanges();
        searchableDropdownComponent = spectator.debugElement.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance as SearchableDropdownComponent;

        searchableDropdownComponent.pageChange.emit({
            filter: filter,
            first: 10,
            page: page,
            pageCount: 10,
            rows: 0
        });

        tick();
        spectator.fixture.detectChanges(false);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
        expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
    }));

    it('should paginate when the filter change', fakeAsync(() => {
        const filter = 'filter';

        paginatorService.totalRecords = 2;
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf([]));
        spectator.detectChanges();
        searchableDropdownComponent = spectator.debugElement.query(
            By.css('dot-searchable-dropdown')
        ).componentInstance as SearchableDropdownComponent;

        searchableDropdownComponent.filterChange.emit(filter);

        tick();
        spectator.fixture.detectChanges(false);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
        expect(paginatorService.filter).toEqual(filter);
    }));

    it('should set container list replacing the identifier for the path, if needed', fakeAsync(() => {
        spectator.detectChanges();
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(observableOf(containers));
        const searchable = spectator.debugElement.query(
            By.css('[data-testid="searchableDropdown"]')
        ).componentInstance as SearchableDropdownComponent;
        searchable.pageChange.emit({ filter: '', first: 0 } as PaginationEvent);
        tick();
        spectator.detectChanges();
        expect(searchable.data[0].identifier).toEqual('427c47a4-c380-439f');
        expect(searchable.data[1].identifier).toEqual('container/path');
    }));
});
