import { Injectable } from '@angular/core';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotCMSEditPageEvent } from '@components/dot-contentlet-editor/components/dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotPushPublishDialogService, DotUiColors } from '@dotcms/dotcms-js';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';
/**
 * Handle Custom events
 *
 * @export
 * @class DotCustomEventHandlerService
 */
@Injectable()
export class DotCustomEventHandlerService {
    private readonly handlers;

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
        private dotGenerateSecurePasswordService: DotGenerateSecurePasswordService
    ) {
        if (!this.handlers) {
            this.handlers = {
                'edit-page': this.goToEditPage.bind(this),
                'edit-contentlet': this.editContentlet.bind(this),
                'edit-task': this.editTask.bind(this),
                'create-contentlet': this.createContentlet.bind(this),
                'company-info-updated': this.setPersonalization.bind(this),
                'push-publish': this.pushPublishDialog.bind(this),
                'download-bundle': this.downloadBundleDialog.bind(this),
                'workflow-wizard': this.executeWorkflowWizard.bind(this),
                'generate-secure-password': this.generateSecurePassword.bind(this)
            };
        }
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
        this.dotGenerateSecurePasswordService.open($event.detail.data)
    }

    private createContentlet($event: CustomEvent): void {
        this.dotContentletEditorService.create({
            data: $event.detail.data
        });
        // TODO: Enabled this and remove previous 3 lines of code when endpoint gets updated
        // this.dotRouterService.goToCreateContent($event.detail.data);
    }

    private goToEditPage($event: CustomEvent<DotCMSEditPageEvent>): void {
        this.dotLoadingIndicatorService.show();
        this.dotRouterService.goToEditPage({
            url: $event.detail.data.url,
            language_id: $event.detail.data.languageId,
            host_id: $event.detail.data.hostId
        });
    }

    private editContentlet($event: CustomEvent): void {
        this.dotRouterService.goToEditContentlet($event.detail.data.inode);
    }

    private editTask($event: CustomEvent): void {
        this.dotRouterService.goToEditTask($event.detail.data.inode);
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
}
