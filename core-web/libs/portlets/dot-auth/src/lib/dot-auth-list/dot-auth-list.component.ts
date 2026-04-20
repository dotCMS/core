import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_SYSTEM_HOST,
    DotAuthConfigPayload,
    DotAuthSiteRow,
    DotAuthStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthListStore } from './store/dot-auth-list.store';

import { DotAuthEditComponent } from '../dot-auth-edit/dot-auth-edit.component';

interface StatusTag {
    labelKey: string;
    severity: 'success' | 'info' | 'secondary';
}

@Component({
    selector: 'dot-auth-list',
    standalone: true,
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        TagModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        SkeletonModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-list.component.html',
    providers: [DotAuthListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotAuthListComponent {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthListStore);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    private readonly searchSubject = new Subject<string>();

    /** Status pill for the SYSTEM_HOST row. */
    readonly systemStatusTag = computed<StatusTag>(() =>
        this.store.system().configured
            ? { labelKey: 'dotauth.status.configured', severity: 'success' }
            : { labelKey: 'dotauth.status.not-configured', severity: 'secondary' }
    );

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    statusTag(status: DotAuthStatus): StatusTag {
        switch (status) {
            case 'SITE_OVERRIDE':
                return { labelKey: 'dotauth.status.site-override', severity: 'success' };
            case 'INHERITED':
                return { labelKey: 'dotauth.status.inherited', severity: 'info' };
            case 'NOT_CONFIGURED':
            default:
                return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
        }
    }

    openSystemDialog(): void {
        this.openDialog(
            this.SYSTEM_HOST,
            this.dotMessageService.get('dotauth.dialog.header.system')
        );
    }

    openSiteDialog(row: DotAuthSiteRow): void {
        this.openDialog(
            row.hostId,
            this.dotMessageService.get('dotauth.dialog.header.site', row.hostName)
        );
    }

    confirmClearSystem(): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('dotauth.confirm.clear.system.header'),
            message: this.dotMessageService.get('dotauth.confirm.clear.system.message'),
            acceptLabel: this.dotMessageService.get('dotauth.action.clear'),
            rejectLabel: this.dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.clearSite(this.SYSTEM_HOST)
        });
    }

    confirmClearSite(row: DotAuthSiteRow): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('dotauth.confirm.clear.site.header'),
            message: this.dotMessageService.get('dotauth.confirm.clear.site.message'),
            acceptLabel: this.dotMessageService.get('dotauth.action.clear'),
            rejectLabel: this.dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.clearSite(row.hostId)
        });
    }

    private openDialog(hostId: string, header: string): void {
        const ref = this.dialogService.open(DotAuthEditComponent, {
            header,
            width: '700px',
            data: { hostId },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotAuthConfigPayload | undefined) => {
            if (result) {
                this.store.saveSite(hostId, result);
            }
        });
    }
}
