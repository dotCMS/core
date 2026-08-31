import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, inject, input, Signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';

/** One variant as the dialog lists it: the same name and colour the Variants card draws. */
export interface DotExperimentsChangePageDialogVariant {
    id: string;
    name: string;
    color: string;
}

/**
 * What the dialog is opened with, as PrimeNG {@link DynamicDialogConfig}.inputValues.
 *
 * The two states are handed over as *signals* rather than as values: `inputValues` reaches the
 * component through `setInput` once, when it is created, so a boolean would be frozen at whatever
 * it was the moment the dialog opened. The signal keeps reading the store.
 */
export interface DotExperimentsChangePageDialogInputs {
    /** Path of the page the variants belong to, which the warning names. */
    pagePath: string;
    /** The variants the change deletes. Never empty — the dialog is only opened over them. */
    variants: DotExperimentsChangePageDialogVariant[];
    /** Whether the deletions are on the wire. */
    deleting: Signal<boolean>;
    /** Whether the last run was refused, which is what the inline message states. */
    failed: Signal<boolean>;
}

/** `true` once the variants are gone and the page is free to change; `undefined` means cancelled. */
export type DotExperimentsChangePageDialogResult = true;

/** Copy of the warning, which names the count and therefore has to agree with it. */
const WARNING_ONE_KEY = 'experiments.configure.page.change.warning.one';
const WARNING_MANY_KEY = 'experiments.configure.page.change.warning.many';

/**
 * Confirmation the Page card raises before changing the page of a draft that has variants.
 *
 * The page is only free to move while the experiment's single variant is the control: a
 * non-control variant holds a copy of *this* page, so repointing the experiment has to delete them
 * first. That is a destructive, irreversible step over content the user made, so it is named — with
 * the page it belongs to and every variant by name — rather than folded into the button that
 * triggers it.
 *
 * It reports the go-ahead and renders what it is given, and that is all: the deletions are the
 * store's, and the wait and the failure arrive as signals over `inputValues`. Closing is the
 * caller's too — the Page card holds the `DynamicDialogRef` and closes it with `true` once the
 * store says the variants are gone, which is that card's cue to open the page picker. Cancel, the X
 * and ESC close with `undefined` and leave the experiment exactly as it was.
 *
 * Opened with `DialogService.open(..., { inputValues: DotExperimentsChangePageDialogInputs, header:
 * dm('experiments.configure.page.change.header'), width: CHANGE_PAGE_DIALOG_WIDTH, closable: true,
 * closeOnEscape: true })`: the title and the X are PrimeNG's dialog chrome, so the header is the
 * caller's to pass.
 */
@Component({
    selector: 'dot-experiments-change-page-dialog',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-experiments-change-page-dialog.component.html'
})
export class DotExperimentsChangePageDialogComponent {
    readonly $pagePath = input.required<string>({ alias: 'pagePath' });
    readonly $variants = input.required<DotExperimentsChangePageDialogVariant[]>({
        alias: 'variants'
    });

    /**
     * Signals, so they are read rather than snapshotted — see
     * {@link DotExperimentsChangePageDialogInputs}. Hence the double call at every use.
     */
    readonly $deleting = input.required<Signal<boolean>>({ alias: 'deleting' });
    readonly $failed = input.required<Signal<boolean>>({ alias: 'failed' });

    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    protected readonly $isDeleting = computed<boolean>(() => this.$deleting()());
    protected readonly $hasFailed = computed<boolean>(() => this.$failed()());

    /** Singular and plural are different sentences, not a count spliced into one. */
    protected readonly $warningKey = computed<string>(() =>
        this.$variants().length === 1 ? WARNING_ONE_KEY : WARNING_MANY_KEY
    );

    protected readonly $warningArgs = computed<string[]>(() => [
        this.$pagePath(),
        String(this.$variants().length)
    ]);

    /** Reports the go-ahead. What happens next — including this dialog closing — is the card's. */
    protected confirm(): void {
        if (this.$isDeleting()) {
            return;
        }

        this.#dispatch.pageChangeConfirmed();
    }

    /** Closes with nothing, same as the X and ESC: no variant is deleted and no page is picked. */
    protected cancel(): void {
        this.#dialogRef.close();
    }
}
