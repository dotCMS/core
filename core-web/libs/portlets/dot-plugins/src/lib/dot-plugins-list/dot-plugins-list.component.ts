import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { BUNDLE_STATE, BundleMap, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPluginsListStore } from './store/dot-plugins-list.store';

import { DotPluginsExtraPackagesComponent } from '../dot-plugins-extra-packages/dot-plugins-extra-packages.component';
import { DotPluginsUploadComponent } from '../dot-plugins-upload/dot-plugins-upload.component';

const BUNDLE_STATE_LABELS: Record<number, string> = {
    [BUNDLE_STATE.UNINSTALLED]: 'plugins.state.uninstalled',
    [BUNDLE_STATE.INSTALLED]: 'plugins.state.installed',
    [BUNDLE_STATE.RESOLVED]: 'plugins.state.resolved',
    [BUNDLE_STATE.STARTING]: 'plugins.state.starting',
    [BUNDLE_STATE.STOPPING]: 'plugins.state.stopping',
    [BUNDLE_STATE.ACTIVE]: 'plugins.state.active'
};

@Component({
    selector: 'dot-plugins-list',
    standalone: true,
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        SelectModule,
        ConfirmDialogModule,
        ToastModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-plugins-list.component.html',
    providers: [DotPluginsListStore, DialogService, ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotPluginsListComponent {
    readonly store = inject(DotPluginsListStore);
    selectedJar: string | null = null;

    /** Pass-through config so the table fills 100% height when empty (empty state centered). */
    readonly $ptConfig = computed(() => ({
        table: {
            style: {
                'table-layout': 'fixed' as const,
                ...(this.store.bundles().length === 0 && {
                    height: '100%',
                    width: '100%'
                })
            }
        }
    }));
    private readonly dialogService = inject(DialogService);

    getAvailableJarOptions(): { label: string; value: string }[] {
        return this.store.availableJars().map((j) => ({ label: j, value: j }));
    }
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);

    getStateLabel(state: number): string {
        return BUNDLE_STATE_LABELS[state] ?? 'plugins.state.unknown';
    }

    refresh(): void {
        this.store.loadBundles();
        this.store.loadAvailablePlugins();
    }

    openUploadDialog(): void {
        const ref = this.dialogService.open(DotPluginsUploadComponent, {
            header: this.dotMessageService.get('plugins.upload.title'),
            width: '500px',
            closable: true,
            closeOnEscape: true
        });
        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.loadBundles();
                this.store.loadAvailablePlugins();
            }
        });
    }

    openExtraPackagesDialog(): void {
        const ref = this.dialogService.open(DotPluginsExtraPackagesComponent, {
            header: this.dotMessageService.get('plugins.extra-packages.title'),
            width: '600px',
            closable: true,
            closeOnEscape: true
        });
        ref?.onClose.pipe(take(1)).subscribe(() => {
            /* dialog closed */
        });
    }

    deploySelectedJar(jar: string): void {
        this.store.deploy(jar);
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;
        this.store.setPagination(page, rows);
    }

    confirmUndeploy(bundle: BundleMap): void {
        this.confirmationService.confirm({
            message: this.dotMessageService.get(
                'plugins.confirm.undeploy.message',
                bundle.symbolicName ?? bundle.jarFile
            ),
            header: this.dotMessageService.get('plugins.confirm.undeploy.header'),
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.undeploy(bundle.jarFile)
        });
    }

    confirmRestart(): void {
        this.confirmationService.confirm({
            message: this.dotMessageService.get('plugins.confirm.restart.message'),
            header: this.dotMessageService.get('plugins.restart'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-outlined',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () =>
                this.store.restart(() => {
                    this.messageService.add({
                        severity: 'success',
                        summary: this.dotMessageService.get('plugins.restart'),
                        detail: this.dotMessageService.get('plugins.restart.success'),
                        life: 3000
                    });
                })
        });
    }
}
