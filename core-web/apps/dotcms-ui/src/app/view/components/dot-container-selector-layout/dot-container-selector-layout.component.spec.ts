import { of as observableOf } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
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
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotContainerSelectorLayoutComponent } from './dot-container-selector-layout.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';

@Component({
    selector: 'dot-icon-button',
    template: ''
})
class MockDotIconButtonComponent {}

describe('DotContainerSelectorLayoutComponent', () => {
    let comp: DotContainerSelectorLayoutComponent;
    let fixture: ComponentFixture<DotContainerSelectorLayoutComponent>;
    let de: DebugElement;
    let dotContainerSelector;
    let containers: DotContainer[];

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            addcontainer: 'Add a Container'
        });

        TestBed.configureTestingModule({
            declarations: [DotContainerSelectorLayoutComponent, MockDotIconButtonComponent],
            imports: [
                SearchableDropDownModule,
                BrowserAnimationsModule,
                CommonModule,
                FormsModule,
                ButtonModule,
                DotPipesModule,
                DotMessagePipe,
                HttpClientTestingModule,
                DotContainerSelectorModule
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
                StringUtils
            ]
        }).compileComponents();

        fixture = DOTTestBed.createComponent(DotContainerSelectorLayoutComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        dotContainerSelector = de.query(By.css('dot-container-selector')).componentInstance;

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

    it('should show the hots name and container name', async () => {
        comp.data = [
            {
                container: containers[0],
                uuid: '1'
            }
        ];

        fixture.detectChanges();
        await fixture.whenStable();

        const dataItem = de.query(By.css('.container-selector__list-item-text'));
        expect(dataItem.nativeNode.textContent).toEqual('Container 1 (demo.dotcms.com)');
    });

    it('should pass the innerClass', () => {
        expect(dotContainerSelector.innerClass).toBe('d-secondary');
    });

    it('should add containers to containers list and emit a change event', () => {
        comp.currentContainers = containers;

        dotContainerSelector.swap.emit(containers[0]);

        expect(comp.data[0].container).toEqual(containers[0]);
        expect(comp.data[0].uuid).not.toBeNull();
        expect(comp.data.length).toEqual(1);
    });

    it('should remove containers after click on trash icon', () => {
        const bodySelectorList = de.query(By.css('.container-selector__list'));

        const bodySelectorListItems = bodySelectorList.nativeElement.children;

        comp.currentContainers = containers;

        dotContainerSelector.swap.emit(containers[0]);

        fixture.detectChanges();

        bodySelectorListItems[0].children[0].click();
        expect(comp.data.length).toEqual(0);
    });

    it('should not add duplicated containers to the list when multiple false', () => {
        comp.currentContainers = containers;

        dotContainerSelector.swap.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.data.length).toEqual(1);

        dotContainerSelector.swap.emit(containers[0]);
        fixture.detectChanges();

        expect(comp.data.length).toEqual(1);
    });

    it('should add duplicated containers to the list when multiple true', () => {
        comp.currentContainers = containers;
        comp.multiple = true;

        dotContainerSelector.swap.emit(containers[0]);
        dotContainerSelector.swap.emit(containers[0]);
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
