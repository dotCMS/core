import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, MockProviders } from 'ng-mocks';
import { of as observableOf } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import { DotcmsEventsService, LoggerService } from '@dotcms/dotcms-js';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe.component';

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { IframeComponent } from '../../../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { IframeOverlayService } from '../../../../../../view/components/_common/iframe/service/iframe-overlay.service';

describe('DotLegacyAdditionalActionsComponent', () => {
    let spectator: Spectator<DotLegacyTemplateAdditionalActionsComponent>;
    let dotMenuService: DotMenuService;
    let getDotMenuIdSpy: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotLegacyTemplateAdditionalActionsComponent,
        imports: [HttpClientTestingModule],
        overrideComponents: [
            [
                DotLegacyTemplateAdditionalActionsComponent,
                {
                    remove: { imports: [IframeComponent] },
                    add: { imports: [MockComponent(IframeComponent)] }
                }
            ]
        ],
        providers: [
            MockProviders(
                IframeOverlayService,
                DotIframeService,
                DotRouterService,
                DotUiColorsService,
                DotcmsEventsService,
                LoggerService,
                DotLoadingIndicatorService
            ),
            {
                provide: DotMenuService,
                useValue: {
                    getDotMenuId: jest.fn().mockReturnValue(observableOf('2'))
                }
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    params: observableOf({ id: '1', tabName: 'properties' })
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotMenuService = spectator.inject(DotMenuService);
        getDotMenuIdSpy = dotMenuService.getDotMenuId as unknown as jest.SpyInstance;
        spectator.detectChanges();
    });

    it('should set additionalPropertiesURL right', () => {
        let urlResult;

        // Subscribe to the observable to trigger the combineLatest
        spectator.component.url.subscribe((url) => (urlResult = url));

        // Verify the service was called with correct parameters
        expect(getDotMenuIdSpy).toHaveBeenCalledWith('templates');
        expect(getDotMenuIdSpy).toHaveBeenCalledTimes(1);

        // Verify the URL is constructed correctly
        expect(urlResult).toEqual(
            // tslint:disable-next-line:max-line-length
            `c/portal/layout?p_l_id=2&p_p_id=templates&p_p_action=1&p_p_state=maximized&p_p_mode=view&_templates_struts_action=%2Fext%2Ftemplates%2Fedit_template&_templates_cmd=edit&inode=1&drawed=false&selectedTab=properties`
        );
    });
});
