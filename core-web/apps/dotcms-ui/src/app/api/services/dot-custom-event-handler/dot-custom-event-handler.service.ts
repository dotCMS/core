import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotCMSEditPageEvent } from '@components/dot-contentlet-editor/components/dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import {
    DotContentTypeService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotLicenseService,
    DotPropertiesService,
    DotRouterService,
    DotIframeService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, DotUiColors } from '@dotcms/dotcms-js';
import { DotCMSContentType, DotContentCompareEvent, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';

export const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

/**
 * Handle Custom events
 *
 * @export
 * @class DotCustomEventHandlerService
 */
@Injectable()
export class DotCustomEventHandlerService {
    private handlers: Record<string, ($event: CustomEvent) => void>;

    constructor(
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotRouterService: DotRouterService,
        private dotUiColorsService: DotUiColorsService,
        private dotNavLogoService: DotNavLogoService,
        private dotContentletEditorService: DotContentletEditorService,
        private dotIframeService: DotIframeService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotDownloadBundleDialogService: DotDownloadBundleDialogService,
        private dotWorkflowEventHandlerService: DotWorkflowEventHandlerService,
        private dotGenerateSecurePasswordService: DotGenerateSecurePasswordService,
        private dotEventsService: DotEventsService,
        private dotLicenseService: DotLicenseService,
        private router: Router,
        private dotPropertiesService: DotPropertiesService,
        private dotContentTypeService: DotContentTypeService
    ) {
        this.dotPropertiesService
            .getKeys([FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED])
            .subscribe((response) => {
                const contentEditorFeatureFlag =
                    response[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === 'true';

                if (!this.handlers) {
                    this.handlers = {
                        'edit-page': this.goToEditPage.bind(this),
                        'edit-contentlet': contentEditorFeatureFlag
                            ? this.editContentlet.bind(this)
                            : this.editContentletLegacy.bind(this),
                        'edit-task': contentEditorFeatureFlag
                            ? this.editTask.bind(this)
                            : this.editTaskLegacy.bind(this),
                        'create-contentlet': contentEditorFeatureFlag
                            ? this.createContentlet.bind(this)
                            : this.createContentletLegacy.bind(this),
                        'create-contentlet-from-edit-page': this.createContentletLegacy.bind(this),
                        'company-info-updated': this.setPersonalization.bind(this),
                        'push-publish': this.pushPublishDialog.bind(this),
                        'download-bundle': this.downloadBundleDialog.bind(this),
                        'workflow-wizard': this.executeWorkflowWizard.bind(this),
                        'generate-secure-password': this.generateSecurePassword.bind(this),
                        'compare-contentlet': this.openCompareDialog.bind(this),
                        'license-changed': this.updateLicense.bind(this),

                        // THIS NEEDS TESTING
                        'edit-host': this.editContentletLegacy.bind(this),
                        'create-host': this.createContentletLegacy.bind(this)
                    };
                }
            });
    }

    /**
     * Handle custom events from the iframe portlets
     *
     * @param CustomEvent event
     * @memberof DotCustomEventHandlerService
     */
    handle(event: CustomEvent): void {
        if (event && this.handlers[event.detail.name]) {
            this.handlers[event.detail.name](event);
        }
    }

    private generateSecurePassword($event: CustomEvent): void {
        this.dotGenerateSecurePasswordService.open($event.detail.data);
    }

    private createContentletLegacy($event: CustomEvent): void {
        this.dotContentletEditorService.create({
            data: $event.detail.data
        });
        // TODO: Enabled this and remove previous 3 lines of code when endpoint gets updated
        // this.dotRouterService.goToCreateContent($event.detail.data);
    }

    private createContentlet($event: CustomEvent): void {
        this.dotContentTypeService
            .getContentType($event.detail.data.contentType)
            .pipe(take(1))
            .subscribe((contentType) => {
                if (this.shouldRedirectToOldContentEditor(contentType)) {
                    return this.createContentletLegacy($event);
                }

                this.router.navigate([`content/new/${$event.detail.data.contentType}`]);
            });
    }

    private goToEditPage($event: CustomEvent<DotCMSEditPageEvent>): void {
        this.dotLoadingIndicatorService.show();
        this.dotRouterService.goToEditPage({
            url: $event.detail.data.url,
            language_id: $event.detail.data.languageId,
            host_id: $event.detail.data.hostId
        });
    }

    private editContentletLegacy($event: CustomEvent): void {
        this.dotRouterService.goToEditContentlet($event.detail.data.inode);
    }

    private editContentlet($event: CustomEvent): void {
        this.dotContentTypeService
            .getContentType($event.detail.data.contentType)
            .pipe(take(1))
            .subscribe((contentType) => {
                if (this.shouldRedirectToOldContentEditor(contentType)) {
                    return this.editContentletLegacy($event);
                }

                this.router.navigate([`content/${$event.detail.data.inode}`]);
            });
    }

    private editTaskLegacy($event: CustomEvent): void {
        this.dotRouterService.goToEditTask($event.detail.data.inode);
    }

    private editTask($event: CustomEvent): void {
        this.dotContentTypeService
            .getContentType($event.detail.data.contentType)
            .pipe(take(1))
            .subscribe((contentType) => {
                if (this.shouldRedirectToOldContentEditor(contentType)) {
                    return this.editTaskLegacy($event);
                }

                this.router.navigate([`content/${$event.detail.data.inode}`]);
            });
    }

    private setPersonalization($event: CustomEvent): void {
        this.dotNavLogoService.setLogo($event.detail.payload.navBarLogo);

        this.dotUiColorsService.setColors(
            document.querySelector('html'),
            <DotUiColors>$event.detail.payload.colors
        );
        this.dotIframeService.reloadColors();
    }

    private pushPublishDialog($event: CustomEvent): void {
        this.dotPushPublishDialogService.open($event.detail.data);
    }

    private downloadBundleDialog($event: CustomEvent): void {
        this.dotDownloadBundleDialogService.open($event.detail.data);
    }

    private executeWorkflowWizard($event: CustomEvent): void {
        this.dotWorkflowEventHandlerService.open($event.detail.data);
    }

    private openCompareDialog($event: CustomEvent): void {
        this.dotEventsService.notify<DotContentCompareEvent>(
            COMPARE_CUSTOM_EVENT,
            $event.detail.data
        );
    }

    /**
     * Update license
     *
     * @private
     * @memberof DotCustomEventHandlerService
     */
    private updateLicense(): void {
        this.dotLicenseService.updateLicense();
    }

    /**
     * Check if the content type have the feature flag in the metadata.
     *
     * @private
     * @param {DotCMSContentType} contentType
     * @return {*}  {boolean}
     * @memberof DotCustomEventHandlerService
     */
    private shouldRedirectToOldContentEditor(contentType: DotCMSContentType): boolean {
        return !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];
    }
}
