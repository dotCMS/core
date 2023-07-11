/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SharedModule } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InplaceModule } from 'primeng/inplace';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';

import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesService } from '@dotcms/app/api/services/dot-categories/dot-categories.service';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotCategoriesListComponent } from './dot-categories-list.component';

import { DotMenuModule } from '../../../view/components/_common/dot-menu/dot-menu.module';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-categories-list></dot-categories-list>`
})
class TestHostComponent {}

xdescribe('DotCategoriesListingTableComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let coreWebService: CoreWebService;
    const items: DotCategory[] = [
        {
            categoryId: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
            categoryName: 'Age or Gender',
            categoryVelocityVarName: 'ageOrGender',
            identifier: '1212',
            inode: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
            key: 'ageOrGender',
            live: false,
            sortOrder: 1,
            working: false,
            name: 'dsdsd',
            friendlyName: 'dfdf',
            type: 'ASD'
        },
        {
            categoryId: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
            categoryName: 'Age or Gender',
            categoryVelocityVarName: 'ageOrGender',
            identifier: '1212',
            inode: '9e882f2a-ada2-47e3-a441-bdf9a7254216',
            key: 'ageOrGender',
            live: false,
            sortOrder: 1,
            working: false,
            name: 'dsdsd',
            friendlyName: 'dfdf',
            type: 'ASD'
        }
    ];
    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'message.category.search': 'Type to Filter',
            'No-Results-Found': 'No Results Found',
            'message.category.empty.title': 'Your category list is empty',
            'message.category.empty.content':
                'You have not added anything yet, start by clicking the button below',
            'message.category.empty.button.label': 'Add New Category'
        });
        TestBed.configureTestingModule({
            declarations: [DotCategoriesListComponent, TestHostComponent],
            imports: [
                SharedModule,
                MenuModule,
                DotMenuModule,
                HttpClientTestingModule,
                DotPipesModule,
                DotMessagePipe,
                BreadcrumbModule,
                DotPortletBaseModule,
                ButtonModule,
                InputTextModule,
                TableModule,
                PaginatorModule,
                InplaceModule,
                InputNumberModule,
                DotActionMenuButtonModule,

                CheckboxModule,
                DotEmptyStateModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                DotCategoriesService
            ]
        });

        hostFixture = TestBed.createComponent(TestHostComponent);
        coreWebService = TestBed.inject(CoreWebService);
    });

    it('should have default attributes', fakeAsync(() => {
        hostFixture.detectChanges();
        tick();
        hostFixture.detectChanges();
        de = hostFixture.debugElement.query(By.css('p-table'));
        expect(de.componentInstance.lazy).toBe(true);
        expect(de.componentInstance.paginator).toBe(true);
        expect(de.componentInstance.reorderableColumns).toBe(true);
        expect(de.componentInstance.rows).toBe(40);
    }));

    it('renderer basic datatable component', fakeAsync(() => {
        setRequestSpy(items);

        hostFixture.detectChanges();
        tick();
        hostFixture.detectChanges();
        de = hostFixture.debugElement.query(By.css('p-table'));
        el = de.nativeElement;
        const rows = el.querySelectorAll('[data-testId="testTableRow"]');
        expect(items.length).toEqual(rows.length);

        const headRow = el.querySelector('[data-testId="testHeadTableRow"]');
        const headers = headRow.querySelectorAll('th');
        expect(8).toEqual(headers.length);

        headers.forEach((_col, index) => {
            const sortableIcon = headers[index].querySelector('p-sortIcon');
            index === 0 || index === headers.length - 1
                ? expect(sortableIcon).toBeNull()
                : expect(sortableIcon).toBeDefined();
        });
    }));

    it('should renders the dot empty state component if items array is empty', fakeAsync(() => {
        setRequestSpy([]);
        hostFixture.detectChanges();
        tick();
        hostFixture.detectChanges();
        de = hostFixture.debugElement.query(By.css('p-table'));
        const emptyState = de.query(By.css('[data-testid="title"]'));
        expect(emptyState.nativeElement.innerText).toBe('Your category list is empty');
    }));

    function setRequestSpy(response: any): void {
        spyOn<any>(coreWebService, 'requestView').and.returnValue(
            of({
                entity: response,
                header: (type) => (type === 'Link' ? 'test;test=test' : '40')
            })
        );
    }
});
