import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    effect,
    forwardRef,
    inject,
    input,
    viewChild
} from '@angular/core';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { DotMessagePipe, DotTruncatePathPipe } from '@dotcms/ui';

import { SiteFieldStore } from './site-field.store';

/**
 * Component for selecting a site from a tree structure.
 * Implements ControlValueAccessor to work with Angular forms.
 * Uses PrimeNG's TreeSelect component for the UI.
 */
@Component({
    selector: 'dot-site-field',
    imports: [ReactiveFormsModule, TreeSelectModule, DotTruncatePathPipe, DotMessagePipe],
    providers: [
        SiteFieldStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SiteFieldComponent)
        }
    ],
    templateUrl: './site-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SiteFieldComponent implements ControlValueAccessor, OnInit {
    /**
     * Store service that manages the site data and state.
     * Handles loading sites and managing selection state.
     */
    protected readonly store = inject(SiteFieldStore);

    /**
     * Form control for the site selection.
     * Binds to the TreeSelect component and manages the selected site value.
     */
    readonly siteControl = new FormControl<string>('');

    /**
     * View child for the TreeSelect component.
     * Allows access to the TreeSelect component's tree view child.
     */
    $treeSelect = viewChild<TreeSelect>(TreeSelect);

    /**
     * Context for pre-populating the site field (hostname and folder path).
     * Provided when the dialog is opened with initial filter context.
     */
    $siteContext = input<{ hostName: string; folderPath: string } | null>(null, {
        alias: 'siteContext'
    });

    /**
     * Reactive label of the currently selected node.
     * Used by the parent to compute chip labels reactively.
     */
    readonly $selectedNodeLabel = computed(() => this.store.nodeSelected()?.label ?? null);

    /**
     * Pending writeValue that couldn't be processed because $siteContext wasn't available yet.
     * Will be applied when $siteContext becomes available via effect.
     */
    #pendingWriteValue: string | null = null;

    /**
     * Creates an instance of SiteFieldComponent.
     * Sets up an effect to handle value changes and propagate them through the ControlValueAccessor.
     */
    constructor() {
        effect(() => {
            const valueToSave = this.store.valueToSave();
            // Call onChange for both selection (valueToSave is truthy) and deselection (valueToSave is null)
            this.onChange(valueToSave || '');
        });

        effect(() => {
            this.store.nodeExpanded();
            const treeSelect = this.$treeSelect();
            if (treeSelect.treeViewChild) {
                treeSelect.treeViewChild.updateSerializedValue();
                treeSelect.cd.detectChanges();
            }
        });

        // Sync TreeSelect display when nodeSelected changes (e.g., pre-population from writeValue).
        // The popover lazily renders content, so writeValue may fire before TreeSelect exists.
        // This effect retries once the TreeSelect ViewChild is available.
        effect(() => {
            const node = this.store.nodeSelected();
            const treeSelect = this.$treeSelect();

            if (node && treeSelect) {
                treeSelect.writeValue(node);
                treeSelect.cd.markForCheck();
            }
        });

        // Process pending writeValue when $siteContext becomes available.
        // The form directive may call writeValue before Angular binds the input.
        effect(() => {
            const ctx = this.$siteContext();
            const pending = this.#pendingWriteValue;

            if (ctx && pending) {
                this.#pendingWriteValue = null;
                this.#applyInitialSelection(pending, ctx);
            }
        });
    }

    /**
     * Lifecycle hook that runs after component initialization.
     * Triggers the loading of available sites through the store.
     */
    ngOnInit(): void {
        this.store.loadSites();
    }

    /**
     * Internal callback function for handling value changes.
     * Used by the ControlValueAccessor to propagate changes to the form model.
     */
    private onChange = (_value: string): void => {
        // noop
    };

    /**
     * Internal callback function for handling touched state.
     * Used by the ControlValueAccessor to mark the control as touched.
     */
    private onTouched = (): void => {
        // noop
    };

    /**
     * Writes a new value to the form control.
     * Implements ControlValueAccessor method to update the control's value programmatically.
     * Handles pre-populated values in "site:{id}" or "folder:{id}" format.
     */
    writeValue(value: string): void {
        if (!value) {
            this.siteControl.setValue('');
            this.store.clearSelection();
            this.#pendingWriteValue = null;

            return;
        }

        if (value.includes(':')) {
            const ctx = this.$siteContext();

            if (ctx) {
                this.#applyInitialSelection(value, ctx);
            } else {
                // $siteContext not available yet — store for later processing via effect
                this.#pendingWriteValue = value;
            }
        }
    }

    /**
     * Parses a "site:{id}" or "folder:{id}" value and sets the initial selection.
     */
    #applyInitialSelection(
        value: string,
        ctx: { hostName: string; folderPath: string }
    ): void {
        const [type, id] = value.split(':');

        if (id && (type === 'site' || type === 'folder')) {
            this.store.setInitialSelection({
                id,
                type,
                hostname: ctx.hostName,
                path: ctx.folderPath ?? ''
            });
        }
    }

    /**
     * Registers the callback function for value changes.
     * Implements ControlValueAccessor method to set up change notifications.
     */
    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers the callback function for touched state.
     * Implements ControlValueAccessor method to handle touch events.
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * Sets the disabled state of the form control.
     * Implements ControlValueAccessor method to handle control's disabled state.
     */
    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.siteControl.disable();
        } else {
            this.siteControl.enable();
        }
    }
}
