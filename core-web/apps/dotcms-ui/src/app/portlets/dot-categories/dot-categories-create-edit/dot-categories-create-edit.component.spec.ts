import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { MenuItem } from 'primeng/api';
import { TabsModule } from 'primeng/tabs';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import { CoreWebService, DotcmsEventsService, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    DotcmsEventsServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotCategoriesCreateEditComponent } from './dot-categories-create-edit.component';
import { DotCategoriesCreateEditStore } from './store/dot-categories-create-edit.store';

import { DotCategoriesService } from '../../../api/services/dot-categories/dot-categories.service';
import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
import { DotCategoriesListComponent } from '../dot-categories-list/dot-categories-list.component';
import { DotCategoriesPermissionsComponent } from '../dot-categories-permissions/dot-categories-permissions.component';

describe('CategoriesCreateEditComponent', () => {
    let spectator: Spectator<DotCategoriesCreateEditComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoriesCreateEditComponent,
        imports: [
            HttpClientTestingModule,
            RouterTestingModule,
            DotMessagePipe,
            TabsModule,
            DotCategoriesListComponent,
            DotPortletBaseComponent,
            DotCategoriesPermissionsComponent
        ],
        providers: [
            DotCategoriesCreateEditStore,
            DotCategoriesService,
            DotIframeService,
            DotLoadingIndicatorService,
            IframeOverlayService,
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: DotUiColorsService, useClass: MockDotUiColorsService },
            { provide: DotcmsEventsService, useValue: new DotcmsEventsServiceMock() },
            LoggerService,
            StringUtils
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should expose vm$ from store with initial category', (done) => {
        spectator.component.vm$.subscribe((vm) => {
            expect(vm).toEqual({ category: { label: 'Top', id: '', tabindex: '0' } });
            done();
        });
    });

    it('should update vm$ when updateCategory is called', (done) => {
        const category: MenuItem = { label: 'Child Category', id: 'child-1', tabindex: '1' };
        const storeFromComponent = spectator.fixture.componentRef.injector.get(
            DotCategoriesCreateEditStore
        );
        jest.spyOn(storeFromComponent, 'updateCategory');

        spectator.component.updateCategory(category);

        expect(storeFromComponent.updateCategory).toHaveBeenCalledWith(category);
        // Also verify state: vm$ should eventually emit the new category
        spectator.component.vm$.subscribe((vm) => {
            if (vm.category.id === category.id) {
                expect(vm.category).toEqual(category);
                done();
            }
        });
    });

    it('should render portlet base and tabs', () => {
        expect(spectator.query('dot-portlet-base')).toBeTruthy();
        expect(spectator.query('.categories-create__tabs')).toBeTruthy();
        expect(spectator.query('p-tabs')).toBeTruthy();
    });

    it('should render three tab panels (children, properties, permissions)', () => {
        const tabPanels = spectator.queryAll('p-tabpanel');
        expect(tabPanels.length).toBe(3);
    });

    it('should render dot-categories-list in first tab panel', () => {
        expect(spectator.query('dot-categories-list')).toBeTruthy();
    });

    it('should render dot-categories-permissions with categoryId from vm', () => {
        const permissionsEl = spectator.debugElement.query(By.css('dot-categories-permissions'));
        expect(permissionsEl).toBeTruthy();
        const permissions = permissionsEl?.componentInstance as InstanceType<
            typeof DotCategoriesPermissionsComponent
        >;
        expect(permissions.categoryId).toBe('');
    });
});
