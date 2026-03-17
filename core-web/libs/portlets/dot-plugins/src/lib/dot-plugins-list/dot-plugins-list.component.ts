import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import {
    BUNDLE_STATE,
    BundleMap,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotMessagePipe } from '@dotcms/ui';

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
        ContextMenuModule,
        ToolbarModule,
        DotMessagePipe,
        DotAddToBundleComponent
    ],
    templateUrl: './dot-plugins-list.component.html',
    providers: [DotPluginsListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotPluginsListComponent {
    readonly store = inject(DotPluginsListStore);
    protected readonly BUNDLE_STATE = BUNDLE_STATE;
    selectedJar: string | null = null;
    isDragging = signal(false);
    private dragCounter = 0;
    private selectedBundle = signal<BundleMap | null>(null);
    readonly addToBundleIdentifier = signal<string | null>(null);
    readonly contextMenu = viewChild<ContextMenu>('contextMenu');

    readonly availableJarOptions = computed(() =>
        this.store.availableJars().map((j) => ({ label: j, value: j }))
    );

    readonly contextMenuItems = computed<MenuItem[]>(() => {
        const bundle = this.selectedBundle();
        if (!bundle) return [];
        return [
            {
                label: this.dotMessageService.get('plugins.process-exports'),
                icon: 'pi pi-cog',
                command: () => this.store.processExports(bundle.symbolicName)
            },
            {
                label: this.dotMessageService.get('plugins.add-to-bundle'),
                icon: 'pi pi-box',
                command: () => this.addToBundleIdentifier.set(bundle.jarFile)
            }
        ];
    });

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
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotMessageDisplayService = inject(DotMessageDisplayService);

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
        this.dialogService.open(DotPluginsExtraPackagesComponent, {
            header: this.dotMessageService.get('plugins.extra-packages.title'),
            width: '600px',
            closable: true,
            closeOnEscape: true
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

    onDragEnter(event: DragEvent): void {
        event.preventDefault();
        this.dragCounter++;
        this.isDragging.set(true);
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault();
        this.dragCounter--;
        if (this.dragCounter === 0) {
            this.isDragging.set(false);
        }
    }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
    }

    onDrop(event: DragEvent): void {
        event.preventDefault();
        this.dragCounter = 0;
        this.isDragging.set(false);

        const allFiles = Array.from(event.dataTransfer?.files ?? []);
        if (allFiles.length === 0) return;

        const jarFiles = allFiles.filter((f) => f.name.toLowerCase().endsWith('.jar'));

        if (jarFiles.length === 0) {
            this.dotMessageDisplayService.push({
                life: 5000,
                message: this.dotMessageService.get('plugins.drag-and-drop.invalid-files.detail'),
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
            return;
        }

        this.store.uploadBundles(jarFiles);
    }

    onContextMenu(event: MouseEvent, bundle: BundleMap): void {
        if (bundle.isSystem) return;
        this.selectedBundle.set(bundle);
        this.contextMenu()?.show(event);
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
            accept: () => this.store.restart()
        });
    }
}
