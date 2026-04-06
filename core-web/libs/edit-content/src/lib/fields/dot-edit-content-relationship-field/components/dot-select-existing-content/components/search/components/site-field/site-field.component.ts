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
    protected readonly store = inject(SiteFieldStore);

    readonly siteControl = new FormControl<string>('');

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
     * Used by the parent SearchComponent to compute chip labels reactively.
     */
    readonly $selectedNodeLabel = computed(() => this.store.nodeSelected()?.label ?? null);

    constructor() {
        // Propagate selection changes to the parent form.
        // Skips the first emission (null from new store) to avoid overwriting pre-populated values.
        let skipInitial = true;

        effect(() => {
            const valueToSave = this.store.valueToSave();

            if (skipInitial) {
                skipInitial = false;

                return;
            }

            this.onChange(valueToSave || '');
        });

        // Update PrimeNG TreeSelect when a node is expanded (loaded children).
        effect(() => {
            this.store.nodeExpanded();
            const treeSelect = this.$treeSelect();
            if (treeSelect?.treeViewChild) {
                treeSelect.treeViewChild.updateSerializedValue();
                treeSelect.cd.detectChanges();
            }
        });

        // Sync siteControl when nodeSelected changes (pre-population or resolve).
        // Uses siteControl (not treeSelect.writeValue) because ng-content is eagerly
        // instantiated but the TreeSelect DOM renders lazily with the popover.
        effect(() => {
            const node = this.store.nodeSelected();

            if (node) {
                this.siteControl.setValue(node as never);
            }
        });
    }

    ngOnInit(): void {
        this.store.loadSites();
    }

    /**
     * Called when the TreeSelect overlay opens.
     * Re-serializes the tree to ensure expanded nodes and children are visible
     * (virtualScroll requires serialized data to be up-to-date).
     */
    onTreeSelectShow(): void {
        const treeSelect = this.$treeSelect();
        if (treeSelect?.treeViewChild) {
            treeSelect.treeViewChild.updateSerializedValue();
        }
    }

    private onChange = (_value: string): void => {
        // noop
    };

    private onTouched = (): void => {
        // noop
    };

    /**
     * Writes a new value to the form control.
     * Handles pre-populated values in "site:{id}" or "folder:{id}" format.
     */
    writeValue(value: string): void {
        if (!value) {
            this.siteControl.setValue('');
            this.store.clearSelection();

            return;
        }

        if (value.includes(':')) {
            const [type, id] = value.split(':');
            const ctx = this.$siteContext();

            if (id && ctx && (type === 'site' || type === 'folder')) {
                this.store.setInitialSelection({
                    id,
                    type,
                    hostname: ctx.hostName,
                    path: ctx.folderPath ?? ''
                });
            }
        }
    }

    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.siteControl.disable();
        } else {
            this.siteControl.enable();
        }
    }
}
