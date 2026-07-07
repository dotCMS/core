import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    forwardRef,
    inject,
    input,
    signal,
    viewChild
} from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Popover, PopoverModule } from 'primeng/popover';
import { ScrollerModule } from 'primeng/scroller';
import { TooltipModule } from 'primeng/tooltip';
import { TreeModule } from 'primeng/tree';

import { TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotTruncatePathPipe } from '@dotcms/ui';

import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';
import { HostFolderFiledStore } from '../../store/host-folder-field.store';

/**
 * Site/Folder selector field: a trigger showing the current selection that opens an
 * overlay with a Sites list and a lazily-loaded, paginated Folders tree. The selection
 * is staged in the overlay and only persisted to the form control when "Select" is
 * clicked; closing the overlay without selecting discards the pending change.
 *
 * @export
 * @class DotHostFolderFieldComponent
 */
@Component({
    selector: 'dot-host-folder-field',
    imports: [
        PopoverModule,
        ScrollerModule,
        TreeModule,
        ButtonModule,
        TooltipModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        DotMessagePipe,
        DotTruncatePathPipe
    ],
    templateUrl: './host-folder-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex items-center w-full' },
    providers: [
        HostFolderFiledStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotHostFolderFieldComponent)
        }
    ]
})
export class DotHostFolderFieldComponent extends BaseControlValueAccessor<string> {
    /**
     * A signal that holds the error state of the field.
     * It is used to display the error state of the field.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * A signal that holds the required state of the field.
     * It is used to display the required state of the field.
     */
    $isRequired = input.required<boolean>({ alias: 'isRequired' });
    /**
     * Reference to the overlay panel, used to close it programmatically after
     * committing a selection.
     */
    $overlay = viewChild<Popover>(Popover);
    /**
     * A readonly instance of the HostFolderFiledStore injected into the component.
     * This store is used to manage the state and actions related to the host folder field.
     */
    readonly store = inject(HostFolderFiledStore);

    /**
     * Whether the path was just copied, used to briefly show a check icon on the copy button.
     */
    readonly $pathCopied = signal(false);

    /**
     * The trigger button's width, applied to the overlay panel so it matches the field's width.
     */
    readonly $overlayWidth = signal<string | null>(null);

    /**
     * Removes PrimeNG's default popover content padding; sections manage their own spacing.
     */
    protected readonly popoverPt = {
        content: { class: '!p-0' }
    };

    /**
     * Removes PrimeNG's default tree padding; the folders section manages its own spacing.
     */
    protected readonly treePt = {
        root: { class: '!p-0' }
    };

    private readonly destroyRef = inject(DestroyRef);
    private $copyResetTimer: ReturnType<typeof setTimeout> | undefined;

    constructor() {
        super();
        this.handlePathToSaveChange(this.store.pathToSave);
        this.handleChangeValue(this.$value);
        this.destroyRef.onDestroy(() => clearTimeout(this.$copyResetTimer));
    }

    /**
     * Toggles the selector overlay, keeping the trigger and the store's `overlayOpen`
     * flag in sync (the overlay panel drives visibility; the store drives icon state).
     */
    toggleOverlay(event: Event): void {
        if (this.$isDisabled()) {
            return;
        }

        const trigger = event.currentTarget as HTMLElement;
        this.$overlayWidth.set(`${trigger.offsetWidth}px`);
        this.$overlay()?.toggle(event, trigger);
    }

    /**
     * Selects a site in the overlay's Sites list.
     */
    onSiteSelect(site: TreeNodeItem): void {
        this.store.selectSite(site);
    }

    /**
     * Stages a folder as the pending selection when clicked in the Folders tree.
     */
    onFolderSelect(event: TreeNodeSelectItem): void {
        this.store.setPendingNode(event.node);
    }

    /**
     * Lazily loads a folder's children the first time it's expanded.
     */
    onFolderExpand(event: TreeNodeSelectItem): void {
        this.store.expandNode(event);
    }

    /**
     * Forwards the search input's value to the store, which debounces and validates
     * the minimum length before querying the backend.
     */
    onSearchInput(event: Event): void {
        const value = (event.target as HTMLInputElement).value;
        this.store.search(value);
    }

    /**
     * Loads the next page of root-level folders for the currently selected site.
     */
    onLoadMore(): void {
        this.store.loadMore(null);
    }

    /**
     * Persists the pending selection and closes the overlay.
     */
    onSelect(): void {
        this.store.commit();
        this.$overlay()?.hide();
    }

    /**
     * Copies the full site/folder path to the clipboard.
     */
    copyPath(): void {
        const path = this.store.copyPath();
        if (!path) {
            return;
        }

        clearTimeout(this.$copyResetTimer);
        void navigator.clipboard.writeText(path).then(() => {
            this.$pathCopied.set(true);
            this.$copyResetTimer = setTimeout(() => this.$pathCopied.set(false), 1500);
        });
    }

    /**
     * A signal that handles the path to save change of the field.
     * It is used to save the path to the field.
     */
    readonly handlePathToSaveChange = signalMethod<string>((pathToSave) => {
        if (pathToSave === null || pathToSave === undefined || !this.onChange || !this.onTouched) {
            return;
        }

        this.onChange(pathToSave);
        this.onTouched();
    });

    /**
     * A signal that handles the change value of the field.
     * It is used to load the sites based on the current path.
     */
    readonly handleChangeValue = signalMethod<string>((currentPath) => {
        this.store.loadSites({
            path: currentPath,
            isRequired: this.$isRequired()
        });
    });
}
