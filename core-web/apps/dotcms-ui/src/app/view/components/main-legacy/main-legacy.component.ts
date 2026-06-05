import { Component, DestroyRef, OnInit, ViewEncapsulation, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';

import { DrawerModule } from 'primeng/drawer';

import { HashbrownChatBridgeService, HashbrownChatComponent } from '@dotcms/dot-ai-chat';
import { DotContentCompareDialogComponent } from '@dotcms/portlets/dot-ema/ui';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotAlertConfirmComponent } from '../_common/dot-alert-confirm/dot-alert-confirm';
import { DotDownloadBundleDialogComponent } from '../_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotGenerateSecurePasswordComponent } from '../_common/dot-generate-secure-password/dot-generate-secure-password.component';
import { DotPushPublishDialogComponent } from '../_common/dot-push-publish-dialog/dot-push-publish-dialog.component';
import { DotWizardComponent } from '../_common/dot-wizard/dot-wizard.component';
import { DotCreateContentletComponent } from '../dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
// Import standalone components
import { DotLargeMessageDisplayComponent } from '../dot-large-message-display/dot-large-message-display.component';
import { DotMessageDisplayComponent } from '../dot-message-display/dot-message-display.component';
import { DotNavigationComponent } from '../dot-navigation/dot-navigation.component';
import { DotToolbarComponent } from '../dot-toolbar/dot-toolbar.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-main-component',
    templateUrl: './main-legacy.component.html',
    imports: [
        RouterOutlet,
        DotCreateContentletComponent,
        DotMessageDisplayComponent,
        DotNavigationComponent,
        DotToolbarComponent,
        DrawerModule,
        HashbrownChatComponent,
        DotLargeMessageDisplayComponent,
        DotAlertConfirmComponent,
        DotPushPublishDialogComponent,
        DotDownloadBundleDialogComponent,
        DotWizardComponent,
        DotGenerateSecurePasswordComponent,
        DotContentCompareDialogComponent
    ]
})
export class MainComponentLegacyComponent implements OnInit {
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hashbrownChatBridgeService = inject(HashbrownChatBridgeService);
    readonly aiChatVisible = signal(false);

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
        document.body.style.backgroundPosition = '';
        document.body.style.backgroundRepeat = '';
        document.body.style.backgroundSize = '';

        this.hashbrownChatBridgeService.openChat$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this.aiChatVisible.set(true);
            });
    }

    /**
     * Handle the custom events emmited by the Create Contentlet
     *
     * @param CustomEvent $event
     * @memberof MainComponentLegacyComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotCustomEventHandlerService.handle($event);
    }

    onAiChatVisibleChange(visible: boolean): void {
        this.aiChatVisible.set(visible);
    }

    closeAiChat(): void {
        this.aiChatVisible.set(false);
    }
}
