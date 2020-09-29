import { of as observableOf } from 'rxjs';
import { CONTAINER_SOURCE, DotContainer } from '@models/container/dot-container.model';
import { By } from '@angular/platform-browser';
import { PaginatorService } from '@services/paginator/paginator.service';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ComponentFixture, fakeAsync, tick, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotContainerSelectorComponent } from './dot-container-selector.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TemplateContainersCacheService } from '@portlets/dot-edit-page/template-containers-cache.service';
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
} from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ContainerSelectorComponent', () => {
    let comp: DotContainerSelectorComponent;
    let fixture: ComponentFixture<DotContainerSelectorComponent>;
    let de: DebugElement;
    let searchableDropdownComponent;
    let containers: DotContainer[];

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
                TemplateContainersCacheService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils
            ]
        }).compileComponents();

        fixture = DOTTestBed.createComponent(DotContainerSelectorComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        searchableDropdownComponent = de.query(By.css('dot-searchable-dropdown')).componentInstance;

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
    });

    it('should show the hots name and container name', () => {
        comp.data = [
            {
                container: containers[0],
                uuid: '1'
            }
        ];

        fixture.detectChanges();

        const dataItem = de.query(By.css('.container-selector__list-item span'));
        expect(dataItem.nativeElement.textContent).toEqual('Container 1 (demo.dotcms.com)');
    });

    it('should change Page', fakeAsync(() => {
        const filter = 'filter';
        const page = 1;

        fixture.detectChanges();

        const paginatorService: PaginatorService = de.injector.get(PaginatorService);
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

        const paginatorService: PaginatorService = de.injector.get(PaginatorService);
        paginatorService.totalRecords = 2;
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([]));

        fixture.detectChanges();

        searchableDropdownComponent.filterChange.emit(filter);

        tick();
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual(filter);
    }));

    it('should add containers to containers list and emit a change event', () => {
        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);

        expect(comp.data[0].container).toEqual(containers[0]);
        expect(comp.data[0].uuid).not.toBeNull();
        expect(comp.data.length).toEqual(1);
    });

    it('should remove containers after click on trash icon', () => {
        const bodySelectorList = de.query(By.css('.container-selector__list'));
        const bodySelectorListItems = bodySelectorList.nativeElement.children;

        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);

        fixture.detectChanges();

        bodySelectorListItems[0].children[0].click();
        expect(comp.data.length).toEqual(0);
    });

    it('should not add duplicated containers to the list when multiple false', () => {
        comp.currentContainers = containers;

        searchableDropdownComponent.change.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.data.length).toEqual(1);

        searchableDropdownComponent.change.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.data.length).toEqual(1);
    });

    it('should add duplicated containers to the list when multiple true', () => {
        comp.currentContainers = containers;
        comp.multiple = true;

        searchableDropdownComponent.change.emit(containers[0]);
        searchableDropdownComponent.change.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.data.length).toEqual(2);
    });

    it('should set container list replacing the identifier for the path, if needed', () => {
        fixture.detectChanges();
        const paginatorService: PaginatorService = de.injector.get(PaginatorService);
        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf(containers));
        comp.handleFilterChange('');

        expect(comp.currentContainers[0].identifier).toEqual('427c47a4-c380-439f');
        expect(comp.currentContainers[1].identifier).toEqual('container/path');
    });
});
