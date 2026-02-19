import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
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
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-plugins-list.component.html',
    providers: [DotPluginsListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsListComponent {
    readonly store = inject(DotPluginsListStore);
    selectedJar: string | null = null;
    private readonly dialogService = inject(DialogService);

    getAvailableJarOptions(): { label: string; value: string }[] {
        return this.store.availableJars().map((j) => ({ label: j, value: j }));
    }
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

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
}
