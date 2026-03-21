import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { Table, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import {
    BUNDLE_STATE,
    DotMessageDisplayService,
    DotMessageService,
    PluginRow
} from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotMessagePipe } from '@dotcms/ui';

import { DotPluginsListStore } from './store/dot-plugins-list.store';

import { DotPluginsExtraPackagesComponent } from '../dot-plugins-extra-packages/dot-plugins-extra-packages.component';
import { DotPluginsUploadComponent } from '../dot-plugins-upload/dot-plugins-upload.component';

@Component({
    selector: 'dot-plugins-list',
    standalone: true,
    imports: [
        MenuModule,
        TableModule,
        ButtonModule,
        ChipModule,
        ConfirmDialogModule,
        ContextMenuModule,
        ToolbarModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
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
    isDragging = signal(false);
    private dragCounter = 0;
    private selectedBundle = signal<PluginRow | null>(null);
    readonly addToBundleIdentifier = signal<string | null>(null);
    readonly contextMenu = viewChild<ContextMenu>('contextMenu');
    readonly toolbarMenu = viewChild.required<Menu>('toolbarMenu');
    private readonly table = viewChild<Table>('dt');

    readonly contextMenuItems = computed<MenuItem[]>(() => {
        const bundle = this.selectedBundle();
        if (!bundle) return [];

        if (bundle.state === 'undeployed') {
            return [
                {
                    label: this.dotMessageService.get('plugins.deploy'),
                    command: () => this.store.deploy(bundle.jarFile)
                }
            ];
        }

        const stateAction: MenuItem =
            bundle.state === BUNDLE_STATE.ACTIVE
                ? {
                      label: this.dotMessageService.get('plugins.stop'),
                      command: () => this.store.stop(bundle.jarFile)
                  }
                : {
                      label: this.dotMessageService.get('plugins.start'),
                      command: () => this.store.start(bundle.jarFile)
                  };

        return [
            stateAction,
            {
                label: this.dotMessageService.get('plugins.undeploy'),
                command: () => this.confirmUndeploy(bundle)
            },
            { separator: true },
            {
                label: this.dotMessageService.get('plugins.process-exports'),
                command: () => this.store.processExports(bundle.symbolicName)
            },
            {
                label: this.dotMessageService.get('plugins.add-to-bundle'),
                command: () => this.addToBundleIdentifier.set(bundle.jarFile)
            }
        ];
    });

    readonly toolbarMenuItems = computed<MenuItem[]>(() => [
        {
            label: this.dotMessageService.get('plugins.restart-osgi'),
            command: () => this.confirmRestart()
        }
    ]);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotMessageDisplayService = inject(DotMessageDisplayService);

    filterTable(value: string): void {
        this.table()?.filterGlobal(value, 'contains');
    }

    refresh(): void {
        this.store.loadAll();
    }

    openUploadDialog(): void {
        const ref = this.dialogService.open(DotPluginsUploadComponent, {
            header: this.dotMessageService.get('plugins.upload.title'),
            width: '500px',
            closable: true,
            closeOnEscape: true,
            resizable: false,
            draggable: false
        });
        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.loadAll();
            }
        });
    }

    openExtraPackagesDialog(): void {
        const ref = this.dialogService.open(DotPluginsExtraPackagesComponent, {
            header: this.dotMessageService.get('plugins.extra-packages.title'),
            width: '600px',
            height: '600px',
            closable: true,
            closeOnEscape: true,
            resizable: false,
            draggable: false
        });
        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result === 'restart') {
                this.store.restart();
            }
        });
    }

    confirmUndeploy(bundle: PluginRow): void {
        this.confirmationService.confirm({
            message: this.dotMessageService.get(
                'plugins.confirm.undeploy.message',
                bundle.symbolicName ?? bundle.jarFile
            ),
            header: this.dotMessageService.get('plugins.confirm.undeploy.header'),
            acceptLabel: this.dotMessageService.get('Ok'),
            rejectLabel: this.dotMessageService.get('Cancel'),
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

    onToolbarMenuToggle(event: MouseEvent): void {
        this.toolbarMenu().toggle(event);
    }

    onContextMenu(event: MouseEvent, bundle: PluginRow): void {
        this.selectedBundle.set(bundle);
        this.contextMenu()?.show(event);
    }

    confirmRestart(): void {
        this.confirmationService.confirm({
            message: this.dotMessageService.get('plugins.confirm.restart.message'),
            header: this.dotMessageService.get('plugins.restart-osgi'),
            acceptLabel: this.dotMessageService.get('Ok'),
            rejectLabel: this.dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-outlined',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.restart()
        });
    }
}
