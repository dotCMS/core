import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';

import DotAnalyticsErrorComponent from './dot-analytics-error.component';

describe('DotAnalyticsErrorComponent', () => {
    let spectator: Spectator<DotAnalyticsErrorComponent>;

    const messageService = {
        get: jest.fn().mockImplementation((key: string) => `Translated ${key}`)
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsErrorComponent,
        mocks: [DotMessageService]
    });

    const createComponentWithParams = (status: HealthStatusTypes, isEnterprise: boolean) => {
        return createComponent({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageService
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            queryParams: {
                                status,
                                isEnterprise: isEnterprise.toString()
                            }
                        }
                    }
                }
            ]
        });
    };

    describe('Component Initialization', () => {
        beforeEach(() => {
            spectator = createComponentWithParams(HealthStatusTypes.NOT_CONFIGURED, true);
        });

        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have analytics error container', () => {
            const errorContainer = spectator.query(byTestId('analytics-error'));
            expect(errorContainer).toExist();
        });

        it('should render dot-empty-container', () => {
            const emptyContainer = spectator.query(DotEmptyContainerComponent);
            expect(emptyContainer).toExist();
        });

        it('should pass correct configuration to dot-empty-container', () => {
            const emptyContainer = spectator.query(DotEmptyContainerComponent);
            expect(emptyContainer).toExist();
            expect(emptyContainer.configuration).toBeDefined();
            expect(emptyContainer.hideContactUsLink).toBe(true);
        });
    });

    describe('Enterprise License Configurations', () => {
        describe('NOT_CONFIGURED status', () => {
            beforeEach(() => {
                spectator = createComponentWithParams(HealthStatusTypes.NOT_CONFIGURED, true);
            });

            it('should show NOT_CONFIGURED error for enterprise users', () => {
                const config = spectator.component['$errorConfig']();
                expect(config.title).toBe('Translated analytics.search.no.configured');
                expect(config.subtitle).toBe('Translated analytics.search.no.configured.subtitle');
                expect(config.icon).toBe('pi-cog');
            });
        });

        describe('CONFIGURATION_ERROR status', () => {
            beforeEach(() => {
                spectator = createComponentWithParams(HealthStatusTypes.CONFIGURATION_ERROR, true);
            });

            it('should show CONFIGURATION_ERROR for enterprise users', () => {
                const config = spectator.component['$errorConfig']();
                expect(config.title).toBe('Translated analytics.search.config.error');
                expect(config.subtitle).toBe('Translated analytics.search.config.error.subtitle');
                expect(config.icon).toBe('pi-exclamation-triangle');
            });
        });

        describe('OK status (edge case)', () => {
            beforeEach(() => {
                spectator = createComponentWithParams(HealthStatusTypes.OK, true);
            });

            it('should show unexpected error for OK status', () => {
                const config = spectator.component['$errorConfig']();
                expect(config.title).toBe('Translated analytics.search.unexpected.error');
                expect(config.subtitle).toBe(
                    'Translated analytics.search.unexpected.error.subtitle'
                );
                expect(config.icon).toBe('pi-times-circle');
            });
        });
    });

    describe('Non-Enterprise License Configuration', () => {
        describe('NOT_CONFIGURED status', () => {
            beforeEach(() => {
                spectator = createComponentWithParams(HealthStatusTypes.NOT_CONFIGURED, false);
            });

            it('should show license error for non-enterprise users regardless of health status', () => {
                const config = spectator.component['$errorConfig']();
                expect(config.title).toBe('Translated analytics.search.no.license');
                expect(config.subtitle).toBe('Translated analytics.search.no.license.subtitle');
                expect(config.icon).toBe('pi-lock');
            });
        });

        describe('OK status', () => {
            beforeEach(() => {
                spectator = createComponentWithParams(HealthStatusTypes.OK, false);
            });

            it('should show license error even when status is OK for non-enterprise', () => {
                const config = spectator.component['$errorConfig']();
                expect(config.title).toBe('Translated analytics.search.no.license');
                expect(config.subtitle).toBe('Translated analytics.search.no.license.subtitle');
                expect(config.icon).toBe('pi-lock');
            });
        });
    });

    describe('Default Fallback', () => {
        beforeEach(() => {
            spectator = createComponentWithParams('UNKNOWN_STATUS' as HealthStatusTypes, true);
        });

        it('should fallback to CONFIGURATION_ERROR for unknown status', () => {
            const config = spectator.component['$errorConfig']();
            expect(config.title).toBe('Translated analytics.search.config.error');
            expect(config.subtitle).toBe('Translated analytics.search.config.error.subtitle');
            expect(config.icon).toBe('pi-exclamation-triangle');
        });
    });
});
