import { DotContentCompareComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotDialogComponent, DotIconComponent } from '@dotcms/ui';

import { AppComponent } from './app.component';
import { DotActionButtonComponent } from './view/components/_common/dot-action-button/dot-action-button.component';
import { DotAlertConfirmComponent } from './view/components/_common/dot-alert-confirm/dot-alert-confirm';
import { DotDownloadBundleDialogComponent } from './view/components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotGenerateSecurePasswordComponent } from './view/components/_common/dot-generate-secure-password/dot-generate-secure-password.component';
import { DotPushPublishDialogComponent } from './view/components/_common/dot-push-publish-dialog/dot-push-publish-dialog.component';
import { DotSiteSelectorComponent } from './view/components/_common/dot-site-selector/dot-site-selector.component';
import { DotTextareaContentComponent } from './view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotWizardComponent } from './view/components/_common/dot-wizard/dot-wizard.component';
import { IframeComponent } from './view/components/_common/iframe/iframe-component/iframe.component';
import { IframePortletLegacyComponent } from './view/components/_common/iframe/iframe-porlet-legacy/iframe-porlet-legacy.component';
import { SearchableDropdownComponent } from './view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotEditContentletComponent } from './view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotCrumbtrailComponent } from './view/components/dot-crumbtrail/dot-crumbtrail.component';
import { DotLargeMessageDisplayComponent } from './view/components/dot-large-message-display/dot-large-message-display.component';
import { DotListingDataTableComponent } from './view/components/dot-listing-data-table/dot-listing-data-table.component';
import { DotMessageDisplayComponent } from './view/components/dot-message-display/dot-message-display.component';
import { DotThemeSelectorDropdownComponent } from './view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.component';
import { DotToolbarComponent } from './view/components/dot-toolbar/dot-toolbar.component';
import { DotWorkflowTaskDetailComponent } from './view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { GlobalSearchComponent } from './view/components/global-search/global-search';
import { DotLogOutContainerComponent } from './view/components/login/dot-logout-container-component/dot-log-out-container';
import { DotLoginPageComponent } from './view/components/login/main/dot-login-page.component';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from './view/components/main-legacy/main-legacy.component';

// Non-standalone components (traditional NgModule components)
export const COMPONENTS = [DotLogOutContainerComponent, GlobalSearchComponent];

// Standalone components (migrated to standalone)
export const STANDALONE_COMPONENTS = [
    AppComponent,
    MainComponentLegacyComponent,
    MainCoreLegacyComponent,
    DotAlertConfirmComponent,
    DotLoginPageComponent,
    DotToolbarComponent,
    DotActionButtonComponent,
    DotEditContentletComponent,
    DotDialogComponent,
    DotIconComponent,
    DotTextareaContentComponent,
    DotWorkflowTaskDetailComponent,
    DotMessageDisplayComponent,
    IframeComponent,
    IframePortletLegacyComponent,
    DotListingDataTableComponent,
    SearchableDropdownComponent,
    DotSiteSelectorComponent,
    DotLargeMessageDisplayComponent,
    DotPushPublishDialogComponent,
    DotContentCompareComponent,
    DotDownloadBundleDialogComponent,
    DotWizardComponent,
    DotGenerateSecurePasswordComponent,
    DotThemeSelectorDropdownComponent,
    DotCrumbtrailComponent
];
