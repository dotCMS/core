import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';

import { Location } from '@angular/common';
import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsAnalyticAppMisconfigurationComponent } from './dot-experiments-analytic-app-misconfiguration.component';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.analytics-app-no-configured.title': 'not configured title',
    'experiments.analytics-app-no-configured.subtitle': 'not configured subtitle',
    'experiments.analytics-app-misconfiguration.title': 'misconfiguration title',
    'experiments.analytics-app-misconfiguration.subtitle': 'misconfiguration subtitle'
});

describe('DotExperimentsAnalyticAppMisconfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsAnalyticAppMisconfigurationComponent>;
    let router: SpyObject<Router>;

    let dynamicState = { healthStatus: HealthStatusTypes.NOT_CONFIGURED };

    const createComponent = createComponentFactory({
        component: DotExperimentsAnalyticAppMisconfigurationComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(Router),
            mockProvider(Location, {
                getState: () => dynamicState
            })
        ]
    });

    describe('with healthStatus NOT_CONFIGURED', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('should DotEmptyContainerComponent exist in the component with not configuration label', () => {
            const dotEmptyContainerComponent: DotEmptyContainerComponent = spectator.query(
                DotEmptyContainerComponent
            );

            expect(dotEmptyContainerComponent.configuration).toEqual({
                title: 'not configured title',
                subtitle: 'not configured subtitle',
                icon: 'pi-chart-line'
            });
        });

        it('should have the correct title in  DotExperimentsUiHeaderComponent', () => {
            const headerComponent = spectator.query(DotExperimentsUiHeaderComponent);
            expect(headerComponent.$title()).toEqual('not configured title');
        });
    });

    describe('with healthStatus CONFIGURATION_ERROR', () => {
        beforeEach(() => {
            dynamicState = { healthStatus: HealthStatusTypes.CONFIGURATION_ERROR };
            spectator = createComponent();
            router = spectator.inject(Router);
        });

        it('should DotEmptyContainerComponent exist in the component with not configuration label', () => {
            const dotEmptyContainerComponent: DotEmptyContainerComponent = spectator.query(
                DotEmptyContainerComponent
            );

            expect(dotEmptyContainerComponent.configuration).toEqual({
                title: 'misconfiguration title',
                subtitle: 'misconfiguration subtitle',
                icon: 'pi-chart-line'
            });
        });

        it('should have the correct title in  DotExperimentsUiHeaderComponent and navigation', () => {
            const headerComponent = spectator.query(DotExperimentsUiHeaderComponent);
            expect(headerComponent.$title()).toEqual('misconfiguration title');

            spectator.triggerEventHandler(DotExperimentsUiHeaderComponent, 'goBack', true);

            expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                queryParamsHandling: 'merge'
            });
        });
    });
});
