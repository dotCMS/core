import { Component, OnInit, ViewEncapsulation, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DotContentCompareDialogComponent } from '@dotcms/portlets/dot-ema/ui';

import { DotDownloadBundleDialogComponent } from '../_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotGenerateSecurePasswordComponent } from '../_common/dot-generate-secure-password/dot-generate-secure-password.component';
import { DotPushPublishDialogComponent } from '../_common/dot-push-publish-dialog/dot-push-publish-dialog.component';
import { DotWizardComponent } from '../_common/dot-wizard/dot-wizard.component';
// Import standalone components
import { DotLargeMessageDisplayComponent } from '../dot-large-message-display/dot-large-message-display.component';
import { DotMessageDisplayComponent } from '../dot-message-display/dot-message-display.component';
import { DotNavigationComponent } from '../dot-navigation/dot-navigation.component';
import { DotSidePanelHostComponent } from '../dot-side-panel-host/dot-side-panel-host.component';
import { DotToolbarComponent } from '../dot-toolbar/dot-toolbar.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-main-component',
    templateUrl: './main-legacy.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    // No PrimeNG MessageService here: this component is an ancestor of every routed portlet, so one
    // provided here would hide the page editor's route-level one and double its toasts (#38303).
    // The side panels and their toast live in <dot-side-panel-host>.
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
        DotSidePanelHostComponent
    ]
})
export class MainComponentLegacyComponent implements OnInit {
    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
        document.body.style.backgroundPosition = '';
        document.body.style.backgroundRepeat = '';
        document.body.style.backgroundSize = '';
    }
}
