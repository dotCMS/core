import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef, DynamicDialogModule } from 'primeng/dynamicdialog';

import { catchError } from 'rxjs/operators';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotCopyContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCopyContent } from '@dotcms/dotcms-models';

export enum CONTENTLET_EDIT_MODE {
    CURRENT = 'CURRENT',
    ALL = 'ALL'
}

/**
 * Allow the user to select whether the edit will affect the main contentlet or the current page contentlet.
 * Create a copy of the contentlet if necessary and return the inode.
 *
 * @export
 * @class DotSelectEditContentletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-contentlet-edit-mode-selector',
    templateUrl: './dot-contentlet-edit-mode-selector.component.html',
    styleUrls: ['./dot-contentlet-edit-mode-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        ButtonModule,
        DynamicDialogModule,
        DotDialogModule,
        DotMessagePipeModule
    ],
    standalone: true
})
export class DotContentletEditModeSelectorComponent implements OnInit {
    private inode: string;
    private copyContent: DotCopyContent;

    public mode = CONTENTLET_EDIT_MODE.CURRENT;
    public readonly EDIT_MODES = CONTENTLET_EDIT_MODE;

    constructor(
        private readonly ref: DynamicDialogRef,
        private readonly config: DynamicDialogConfig,
        private readonly dotCopyContentService: DotCopyContentService,
        private readonly httpErrorManagerService: DotHttpErrorManagerService,
        private readonly cd: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        const { inode, copyContent } = this.config.data || {};

        this.inode = inode;
        this.copyContent = copyContent;
    }

    /**
     * Change Contentlet edit mode.
     *
     * @param {CONTENTLET_EDIT_MODE} mode
     * @memberof DotSelectEditContentletComponent
     */
    changeMode(mode: CONTENTLET_EDIT_MODE) {
        this.mode = mode;
        this.cd.markForCheck();
    }

    /**
     * Choose the contentlet edit mode based on the current option.
     *
     * @memberof DotSelectEditContentletComponent
     */
    editContentlet() {
        this.mode === CONTENTLET_EDIT_MODE.ALL
            ? this.editMasterContentlet()
            : this.editPageContentlet();
    }

    /**
     * Close Dialog without emiting an inode.
     *
     * @memberof DotSelectEditContentletComponent
     */
    closeDialog() {
        this.ref.close({});
    }

    /**
     * Create a copy of the content and close the Dialog emiting the new inode.
     *
     * @private
     * @memberof DotSelectEditContentletComponent
     */
    private editPageContentlet(): void {
        this.dotCopyContentService
            .copyContentInPage(this.copyContent)
            .pipe(catchError((error) => this.httpErrorManagerService.handle(error)))
            .subscribe(
                ({ inode }: DotCMSContentlet) => this.ref.close({ inode }),
                () => this.closeDialog()
            );
    }

    /**
     * Close the Dialog emiting the contenntlet inode.
     *
     * @private
     * @memberof DotSelectEditContentletComponent
     */
    private editMasterContentlet(): void {
        this.ref.close({ inode: this.inode });
    }
}
