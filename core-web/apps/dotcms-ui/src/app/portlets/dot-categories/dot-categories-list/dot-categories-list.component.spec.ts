/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableModule } from 'primeng/table';
import { Component, DebugElement } from '@angular/core';
import { DotCategoriesListComponent } from './dot-categories-list.component';
import { DotMenuModule } from '../../../view/components/_common/dot-menu/dot-menu.module';
import { SharedModule } from 'primeng/api';
import { MenuModule } from 'primeng/menu';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { InplaceModule } from 'primeng/inplace';
import { DotCategoriesService } from '@dotcms/app/api/services/dot-categories/dot-categories.service';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CheckboxModule } from 'primeng/checkbox';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { PaginatorModule } from 'primeng/paginator';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-categories-list></dot-categories-list>`
})
class TestHostComponent {}

fdescribe('DotCategoriesListingTableComponent', () => {
    // let comp: DotCategoriesListComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    // let hostComponent: TestHostComponent;
    let de: DebugElement;
    // let el: HTMLElement;
    // let coreWebService: CoreWebService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotCategoriesListComponent, TestHostComponent],
            imports: [
                SharedModule,
                MenuModule,
                DotMenuModule,
                HttpClientTestingModule,
                DotPipesModule,
                BreadcrumbModule,
                DotPortletBaseModule,
                ButtonModule,
                InputTextModule,
                TableModule,
                PaginatorModule,
                InplaceModule,
                InputNumberModule,
                DotActionMenuButtonModule,
                DotMessagePipeModule,
                CheckboxModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotCategoriesService
            ]
        });

        hostFixture = TestBed.createComponent(TestHostComponent);
        // hostComponent = hostFixture.componentInstance;
        // coreWebService = TestBed.inject(CoreWebService);
        // comp = hostFixture.debugElement.query(By.css('dot-categories-list')).componentInstance;
        de = hostFixture.debugElement.query(By.css('p-table'));
        // el = de.nativeElement;
    });

    it('should have default attributes', () => {
        expect(de.componentInstance.responsiveLayout).toBe('scroll');
        expect(de.componentInstance.lazy).toBe(true);
        expect(de.componentInstance.paginator).toBe(true);
        expect(de.componentInstance.reorderableColumns).toBe(true);
    });
});
