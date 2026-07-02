import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    effect,
    forwardRef,
    inject,
    input,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { skipWhile } from 'rxjs/operators';

import { TreeNodeItem } from '@dotcms/dotcms-models';
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

    readonly siteControl = new FormControl<TreeNodeItem | null>(null);

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

    readonly #destroyRef = inject(DestroyRef);

    constructor() {
        // Propagate selection changes to the parent form.
        // skipWhile(null) ignores the initial null from the new store;
        // once a real value arrives (pre-populated or user-selected), all emissions propagate.
        toObservable(this.store.valueToSave)
            .pipe(
                skipWhile((v) => v === null),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((valueToSave) => {
                this.#onChange(valueToSave || '');
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

            if (node && this.siteControl.value !== node) {
                this.siteControl.setValue(node);
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

    #onChange = (_value: string): void => {
        // noop
    };

    // eslint-disable-next-line no-unused-private-class-members -- ControlValueAccessor callback stored via registerOnTouched
    #onTouched = (): void => {
        // noop
    };

    /**
     * Writes a new value to the form control.
     * Handles pre-populated values in "site:{id}" or "folder:{id}" format.
     */
    writeValue(value: string): void {
        if (!value) {
            this.siteControl.setValue(null);
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

                // Propagate synchronously so the parent form reflects the pre-populated value
                // without relying on the async toObservable subscription.
                this.#onChange(value);
            }
        }
    }

    registerOnChange(fn: (value: string) => void): void {
        this.#onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.#onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.siteControl.disable();
        } else {
            this.siteControl.enable();
        }
    }
}
