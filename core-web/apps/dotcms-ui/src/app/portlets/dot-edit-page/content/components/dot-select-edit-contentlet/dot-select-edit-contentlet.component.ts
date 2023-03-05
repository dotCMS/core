import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    Output
} from '@angular/core';

import { catchError } from 'rxjs/operators';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotCopyContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCopyContent } from '@dotcms/dotcms-models';

enum SELECT_OPTIONS {
    CURRENT = 'CURRENT',
    ALL = 'ALL'
}

@Component({
    selector: 'dot-select-edit-contentlet',
    templateUrl: './dot-select-edit-contentlet.component.html',
    styleUrls: ['./dot-select-edit-contentlet.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonModule, DotDialogModule, DotMessagePipeModule],
    standalone: true
})
export class DotSelectEditContentletComponent {
    @Input()
    inode: string;

    @Input()
    copyContent: DotCopyContent;

    @Input()
    show = false;

    @Output()
    hide = new EventEmitter<void>();

    @Output()
    accept = new EventEmitter<{ inode: string }>();

    @Output()
    cancel = new EventEmitter<void>();

    public selected = SELECT_OPTIONS.CURRENT;
    public readonly options = SELECT_OPTIONS;

    dialogActions = {
        accept: {
            label: 'Accept',
            disabled: false,
            action: () => this.copyAndEditAction()
        },
        cancel: {
            label: 'Cancel',
            disabled: false,
            action: this.cancel.emit()
        }
    };

    constructor(
        private readonly dotCopyContentService: DotCopyContentService,
        private readonly httpErrorManagerService: DotHttpErrorManagerService,
        private readonly cd: ChangeDetectorRef
    ) {}

    changeSelection(selected: SELECT_OPTIONS) {
        this.selected = selected;

        this.dialogActions = {
            ...this.dialogActions,
            accept: {
                ...this.dialogActions.accept,
                action: () => {
                    selected === SELECT_OPTIONS.ALL
                        ? this.globalEditAction()
                        : this.copyAndEditAction();
                }
            }
        };

        this.cd.markForCheck();
    }

    private copyAndEditAction(): void {
        this.dotCopyContentService
            .copyContentInPage(this.copyContent)
            .pipe(catchError((error) => this.httpErrorManagerService.handle(error)))
            .subscribe(({ inode }: DotCMSContentlet) => {
                this.hide.emit();
                this.accept.emit({ inode });
            });
    }

    private globalEditAction(): void {
        this.hide.emit();
        this.accept.emit({ inode: this.inode });
    }
}
