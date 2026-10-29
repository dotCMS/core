import {
    Component,
    Injector,
    OnInit,
    ViewEncapsulation,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotContentCompareDialogComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotSidePanelOutletComponent } from '@dotcms/ui';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogComponent } from '../_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotGenerateSecurePasswordComponent } from '../_common/dot-generate-secure-password/dot-generate-secure-password.component';
import { DotPushPublishDialogComponent } from '../_common/dot-push-publish-dialog/dot-push-publish-dialog.component';
import { DotWizardComponent } from '../_common/dot-wizard/dot-wizard.component';
// Import standalone components
import { DotLargeMessageDisplayComponent } from '../dot-large-message-display/dot-large-message-display.component';
import { DotMessageDisplayComponent } from '../dot-message-display/dot-message-display.component';
import { DotNavigationComponent } from '../dot-navigation/dot-navigation.component';
import { DotToolbarComponent } from '../dot-toolbar/dot-toolbar.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-main-component',
    templateUrl: './main-legacy.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    // PrimeNG's MessageService for the <p-toast> beside the outlet (#38303). Only a panel opened with
    // an injector under this component reaches it: the outlet resolves a panel's content through the
    // injector it was opened with, so the root injector of a root service never passes through here.
    providers: [MessageService],
    imports: [
        RouterOutlet,
        DotMessageDisplayComponent,
        DotNavigationComponent,
        DotToolbarComponent,
        DotLargeMessageDisplayComponent,
        DotPushPublishDialogComponent,
        DotDownloadBundleDialogComponent,
        DotWizardComponent,
        DotGenerateSecurePasswordComponent,
        DotContentCompareDialogComponent,
        DotSidePanelOutletComponent,
        ToastModule
    ]
})
export class MainComponentLegacyComponent implements OnInit {
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);

    /**
     * Hands the custom-event handler this component's injector, so the side panels the legacy
     * portlets' events open live under it and find the MessageService and toast above (#38303).
     */
    constructor() {
        this.dotCustomEventHandlerService.setInjector(inject(Injector));
    }

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
        document.body.style.backgroundPosition = '';
        document.body.style.backgroundRepeat = '';
        document.body.style.backgroundSize = '';
    }
}
