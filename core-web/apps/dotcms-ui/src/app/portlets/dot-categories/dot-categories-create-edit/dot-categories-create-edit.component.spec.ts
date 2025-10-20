import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabViewModule } from 'primeng/tabview';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';
import { DotCategoriesCreateEditStore } from './store/dot-categories-create-edit.store';

import { DotCategoriesService } from '../../../api/services/dot-categories/dot-categories.service';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
import { DotCategoriesListComponent } from '../dot-categories-list/dot-categories-list.component';
import { DotCategoriesPermissionsModule } from '../dot-categories-permissions/dot-categories-permissions.module';

@Pipe({
    name: 'dm',
    standalone: false
})
class MockPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
describe('CategoriesCreateEditComponent', () => {
    let component: DotCategoriesCreateEditComponent;
    let fixture: ComponentFixture<DotCategoriesCreateEditComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [MockPipe],
            imports: [
                CommonModule,
                HttpClientTestingModule,
                DotMessagePipe,
                TabViewModule,
                DotCategoriesListComponent,
                DotPortletBaseComponent,
                DotCategoriesPermissionsModule,
                DotCategoriesCreateEditComponent
            ],
            providers: [
                DotCategoriesCreateEditStore,
                DotCategoriesService,
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoriesCreateEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
