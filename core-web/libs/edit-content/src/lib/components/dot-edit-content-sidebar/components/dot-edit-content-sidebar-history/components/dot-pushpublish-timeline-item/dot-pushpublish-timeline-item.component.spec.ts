import { createComponentFactory, Spectator, byTestId, mockProvider } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService, DotFormatDateService } from '@dotcms/data-access';
import {
    DotCopyButtonComponent,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotGravatarDirective
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPushpublishTimelineItemComponent } from './dot-pushpublish-timeline-item.component';

import { DotPushPublishHistoryItem } from '../../../../../../models/dot-edit-content.model';

describe('DotPushpublishTimelineItemComponent', () => {
    let spectator: Spectator<DotPushpublishTimelineItemComponent>;

    const mockPushPublishHistoryItem: DotPushPublishHistoryItem = {
        bundleId: '01K6NY6Z8V92T6SAF582WMTKYQ',
        environment: 'production-receiver',
        pushDate: Date.now() - 86400000, // 1 day ago
        pushedBy: 'Admin User'
    };

    const createComponent = createComponentFactory({
        component: DotPushpublishTimelineItemComponent,
        imports: [
            AvatarModule,
            TooltipModule,
            DotCopyButtonComponent,
            DotMessagePipe,
            DotRelativeDatePipe,
            DotGravatarDirective
        ],
        providers: [
            DatePipe,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'edit.content.sidebar.pushpublish.bundle': 'Bundle',
                    'edit.content.sidebar.pushpublish.environment': 'Environment'
                })
            },
            mockProvider(DotFormatDateService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false // Don't auto-detect changes
        });
        spectator.setInput('item', mockPushPublishHistoryItem);
        spectator.detectChanges(); // Now detect changes after input is set
    });

    describe('Data Display', () => {
        it('should display correct user name', () => {
            const userName = spectator.query(byTestId('pushpublish-user'));
            expect(userName.textContent?.trim()).toBe('Admin User');
        });

        it('should display truncated bundle ID', () => {
            const bundleIdText = spectator.query(byTestId('bundle-id-text'));
            expect(bundleIdText.textContent?.trim()).toBe('01K6NY'); // First 6 characters
        });

        it('should display correct user avatar label', () => {
            const avatarElement = spectator.query(byTestId('user-avatar'));
            expect(avatarElement).toBeTruthy();

            // Access the PrimeNG Avatar component instance to verify label property
            const avatarDebugElement = spectator.debugElement.query(
                By.css('[data-testid="user-avatar"]')
            );
            const avatarComponent = avatarDebugElement?.componentInstance;

            expect(avatarComponent).toBeTruthy();
            expect(avatarComponent.label).toBe('A'); // First letter of 'Admin User'
        });

        it('should pass full bundle ID to copy button', () => {
            const copyButtonElement = spectator.query(byTestId('copy-bundle-id'));
            expect(copyButtonElement).toBeTruthy();

            // Access the DotCopyButtonComponent instance to verify copy property
            const copyButtonDebugElement = spectator.debugElement.query(
                By.css('[data-testid="copy-bundle-id"]')
            );
            const copyButtonComponent = copyButtonDebugElement?.componentInstance;

            expect(copyButtonComponent).toBeTruthy();
            expect(copyButtonComponent.copy()).toBe('01K6NY6Z8V92T6SAF582WMTKYQ');
        });
    });

    describe('Different Environments', () => {
        it('should handle different environment names', () => {
            const testEnvironments = ['staging', 'development', 'qa-environment'];

            testEnvironments.forEach((env) => {
                spectator.setInput('item', {
                    ...mockPushPublishHistoryItem,
                    environment: env
                });
                spectator.detectChanges();

                // Environment data should be available in component
                expect(spectator.component.$item().environment).toBe(env);
            });
        });
    });

    describe('Computed Signals', () => {
        it('should compute truncated bundle ID correctly', () => {
            expect(spectator.component.$truncatedBundleId()).toBe('01K6NY');
        });
    });

    describe('Component Inputs', () => {
        it('should handle complete data changes', () => {
            const newItem: DotPushPublishHistoryItem = {
                bundleId: 'XYZ789NEWBUNDLE123',
                environment: 'staging-server',
                pushDate: Date.now() - 3600000, // 1 hour ago
                pushedBy: 'System Administrator'
            };

            spectator.setInput('item', newItem);
            spectator.detectChanges();

            const userName = spectator.query(byTestId('pushpublish-user'));
            const bundleIdText = spectator.query(byTestId('bundle-id-text'));

            expect(userName.textContent?.trim()).toBe('System Administrator');
            expect(bundleIdText.textContent?.trim()).toBe('XYZ789');

            // Access the PrimeNG Avatar component instance to verify label property
            const avatarDebugElement = spectator.debugElement.query(
                By.css('[data-testid="user-avatar"]')
            );
            const avatarComponent = avatarDebugElement?.componentInstance;
            expect(avatarComponent).toBeTruthy();
            expect(avatarComponent.label).toBe('S');

            // Access the DotCopyButtonComponent instance to verify copy property
            const copyButtonDebugElement = spectator.debugElement.query(
                By.css('[data-testid="copy-bundle-id"]')
            );
            const copyButtonComponent = copyButtonDebugElement?.componentInstance;
            expect(copyButtonComponent).toBeTruthy();
            expect(copyButtonComponent.copy()).toBe('XYZ789NEWBUNDLE123');
        });
    });
});
