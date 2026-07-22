import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    Injector,
    OnDestroy,
    afterNextRender,
    computed,
    forwardRef,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { popFormBridge, pushFormBridge } from '@dotcms/edit-content-bridge';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Renders the new Edit Content editor inside a right-to-left slide-in panel (`p-drawer`), as an
 * alternative to the full-screen route or the centered dialog.
 *
 * It reuses the overlay editor plumbing: it provides {@link OverlayEditContentHost} (identity from
 * the dialog config, in-place navigation, chrome no-ops) and, since it is not opened through
 * `DialogService`, supplies the {@link DynamicDialogConfig} the host reads identity from — built
 * from the {@link data} input. The header shows the content title plus an expand toggle (70% ↔
 * full width) and a close button.
 */
@Component({
    selector: 'dot-edit-content-side-panel',
    standalone: true,
    imports: [DrawerModule, ButtonModule, DotEditContentLayoutComponent],
    providers: [
        OverlayEditContentHost,
        { provide: EDIT_CONTENT_HOST, useExisting: OverlayEditContentHost },
        {
            // The overlay host reads the content identity from the dialog config; this panel is not
            // opened through DialogService, so feed it from the `data` input. The `data` getter is
            // lazy on purpose: it defers reading the input until the host actually resolves the
            // identity, by which point Angular has applied the input.
            provide: DynamicDialogConfig,
            useFactory: (panel: DotEditContentSidePanelComponent) => ({
                get data() {
                    return panel.data();
                }
            }),
            deps: [forwardRef(() => DotEditContentSidePanelComponent)]
        }
    ],
    template: `
        <p-drawer
            [visible]="!!data()"
            [modal]="false"
            [dismissible]="false"
            [closeOnEscape]="false"
            [closable]="false"
            [pt]="{
                root: {
                    style: {
                        width: $expanded() ? '100%' : '70%',
                        transition: 'width 250ms ease',
                        boxShadow: '-12px 0 24px rgb(0 0 0 / 20%)'
                    }
                },
                content: { style: { padding: '0' } }
            }"
            position="right">
            <ng-template #header>
                <div class="flex w-full items-center justify-between gap-4">
                    <span class="truncate text-lg font-semibold" data-testId="side-panel-title">
                        {{ data()?.title }}
                    </span>
                    <div class="flex items-center gap-1">
                        <p-button
                            [text]="true"
                            severity="secondary"
                            [icon]="$expanded() ? 'pi pi-window-minimize' : 'pi pi-window-maximize'"
                            [attr.aria-label]="$expanded() ? 'Collapse panel' : 'Expand panel'"
                            (onClick)="$expanded.set(!$expanded())"
                            data-testId="side-panel-expand" />
                        <p-button
                            [text]="true"
                            severity="secondary"
                            icon="pi pi-times"
                            aria-label="Close panel"
                            (onClick)="closed.emit()"
                            data-testId="side-panel-close" />
                    </div>
                </div>
            </ng-template>
            @for (item of $items(); track item.contentletInode ?? item.contentTypeId) {
                <dot-edit-content-form-layout />
            }
        </p-drawer>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidePanelComponent implements OnDestroy {
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);

    /** Identity (and header title) of the content to create/edit, or `null` when closed. */
    readonly data = input<EditContentDialogData | null>(null);

    /** Emitted when the user closes the panel, so the opener can clear its request. */
    readonly closed = output<void>();

    /** Emitted on each successful save, so the opener can refresh its view. */
    readonly saved = output<DotCMSContentlet>();

    /** Whether the panel is expanded to the full viewport width (vs the default ~70%). */
    protected readonly $expanded = signal(false);

    /**
     * `@for` source: a single-item list. Rendering the editor through `@for` (instead of directly)
     * defers its creation until the input has a value — the editor resolves its identity
     * synchronously on construction, so it must not be created before `data` is applied.
     */
    protected readonly $items = computed(() => {
        const data = this.data();

        return data ? [data] : [];
    });

    constructor() {
        // Give the editor a clean form-bridge slot; restore the previous one on close.
        pushFormBridge();

        // Forward each save to the opener so it can refresh its view. The overlay host is resolved
        // AFTER construction (afterNextRender) on purpose: resolving it in the constructor would
        // cycle through its `DynamicDialogConfig` factory, which depends on this component.
        afterNextRender(() => {
            this.#injector
                .get(OverlayEditContentHost)
                .saved$.pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((contentlet) => this.saved.emit(contentlet));
        });
    }

    ngOnDestroy(): void {
        popFormBridge();
    }
}
