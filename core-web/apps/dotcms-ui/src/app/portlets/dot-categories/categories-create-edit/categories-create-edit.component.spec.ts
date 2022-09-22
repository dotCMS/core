import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoriesCreateEditComponent } from './categories-create-edit.component';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CategoriesListModule } from '../categories-list/categories-list.module';
import { TabViewModule } from 'primeng/tabview';

@Pipe({ name: 'dm' })
class MockPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
describe('CategoriesCreateEditComponent', () => {
    let component: CategoriesCreateEditComponent;
    let fixture: ComponentFixture<CategoriesCreateEditComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CategoriesCreateEditComponent, MockPipe],
            imports: [DotPortletBaseModule, CategoriesListModule, TabViewModule]
        }).compileComponents();

        fixture = TestBed.createComponent(CategoriesCreateEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
