import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabViewModule } from 'primeng/tabview';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';

import { DotCategoriesListingModule } from '../dot-categories-list/dot-categories-list.module';

@Pipe({ name: 'dm' })
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
            declarations: [DotCategoriesCreateEditComponent, MockPipe],
            imports: [DotPortletBaseModule, DotCategoriesListingModule, TabViewModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoriesCreateEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
