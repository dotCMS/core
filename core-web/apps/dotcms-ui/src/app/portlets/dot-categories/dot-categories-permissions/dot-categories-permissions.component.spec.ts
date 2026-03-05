import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { RouterTestingModule } from '@angular/router/testing';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import { DotcmsEventsService, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import { DotcmsEventsServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';

import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';

describe('CategoriesPermissionsComponent', () => {
    let spectator: Spectator<DotCategoriesPermissionsComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoriesPermissionsComponent,
        imports: [RouterTestingModule],
        providers: [
            DotIframeService,
            DotLoadingIndicatorService,
            IframeOverlayService,
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: DotUiColorsService, useClass: MockDotUiColorsService },
            { provide: DotcmsEventsService, useValue: new DotcmsEventsServiceMock() },
            LoggerService,
            StringUtils
        ]
    });

    it('should create', () => {
        spectator = createComponent({ props: { categoryId: '' } });
        expect(spectator.component).toBeTruthy();
    });

    describe('permissions', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: { categoryId: '123' },
                detectChanges: true
            });
        });

        it('should set iframe permissions url', () => {
            const iframe = spectator.query(byTestId('permissionsIframe'));
            expect(iframe).toBeTruthy();
            expect(spectator.component.permissionsUrl).toBe(
                '/html/categories/permissions.jsp?categoryId=123'
            );
        });
    });

    describe('ngOnChanges', () => {
        it('should build permissionsUrl when categoryId is empty', () => {
            spectator = createComponent({ props: { categoryId: '' }, detectChanges: true });
            expect(spectator.component.permissionsUrl).toBe(
                '/html/categories/permissions.jsp?categoryId='
            );
        });

        it('should update permissionsUrl when categoryId changes', () => {
            spectator = createComponent({ props: { categoryId: '456' }, detectChanges: true });
            expect(spectator.component.permissionsUrl).toBe(
                '/html/categories/permissions.jsp?categoryId=456'
            );
        });
    });
});
