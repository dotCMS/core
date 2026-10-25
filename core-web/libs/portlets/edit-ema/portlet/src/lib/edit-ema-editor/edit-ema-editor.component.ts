import { injectDispatch } from '@ngrx/signals/events';

import { NgClass, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    WritableSignal,
    computed,
    inject,
    signal
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';

import {
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentlet,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotPaletteListStore, DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { UVE_MODE } from '@dotcms/types';

import { DotUveBrowserToolbarComponent } from './components/dot-uve-browser-toolbar/dot-uve-browser-toolbar.component';
import { DotUveCanvasComponent } from './components/dot-uve-canvas/dot-uve-canvas.component';
import { DotUveContentEditorComponent } from './components/dot-uve-content-editor/dot-uve-content-editor.component';
import { uveContentEditorEvents } from './components/dot-uve-content-editor/dot-uve-content-editor.events';
import { DotUveEditPanelComponent } from './components/dot-uve-edit-panel/dot-uve-edit-panel.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';

import { UVEStore } from '../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../store/features/editor/models';

@Component({
    selector: 'dot-edit-ema-editor',
    templateUrl: './edit-ema-editor.component.html',
    styleUrls: ['./edit-ema-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgClass,
        NgStyle,
        FormsModule,
        ReactiveFormsModule,
        ConfirmDialogModule,
        DotResultsSeoToolComponent,
        DotUveToolbarComponent,
        DotUvePaletteComponent,
        InputGroupModule,
        InputGroupAddonModule,
        DotUveContentEditorComponent,
        DotUveBrowserToolbarComponent,
        DotUveEditPanelComponent,
        DotUveCanvasComponent
    ],
    providers: [
        DotPaletteListStore,
        DotCopyContentService,
        DotHttpErrorManagerService,
        DotContentletService
    ]
})
export class EditEmaEditorComponent implements OnInit {
    protected readonly uveStore = inject(UVEStore);

    private readonly contentEditor = injectDispatch(uveContentEditorEvents);

    readonly $ogTags: WritableSignal<SeoMetaTags> = signal(undefined);

    protected readonly $seoResults = computed(() => {
        const socialMedia = this.uveStore.viewSocialMedia();
        const ogTags = this.uveStore.editorOgTags();
        const shouldShowSeoResults = socialMedia && ogTags;

        return shouldShowSeoResults
            ? {
                  ogTags,
                  socialMedia
              }
            : null;
    });

    readonly $mode = this.uveStore.viewMode;

    // Component-level computed to avoid cross-feature dependency
    readonly $editorContentStyles = computed<Record<string, string>>(() => {
        const socialMedia = this.uveStore.viewSocialMedia();
        return {
            display: socialMedia ? 'none' : 'grid'
        };
    });

    readonly ogTagsResults$ = toObservable<SeoMetaTagsResult[]>(this.uveStore.viewOgTagsResults);

    get $editPanelOpen() {
        return this.uveStore.editorEditPanelOpen();
    }

    readonly UVE_MODE = UVE_MODE;
    readonly DotCMSClazzes = DotCMSClazzes;

    ngOnInit(): void {
        // The store outlives the editor, so only the editor knows when it is created again.
        this.uveStore.editorSetEditPanelTab(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
    }

    /**
     * Asks the content editor to edit a contentlet. Public because DotEmaShellComponent calls
     * it for the "Properties" nav action (editing the page's own contentlet), through the
     * router-outlet `(activate)` reference to this component. No permission check, as Properties
     * never had one.
     *
     * @param {DotCMSContentlet} contentlet - The contentlet to edit.
     * @memberof EditEmaEditorComponent
     */
    openContentForEdit(contentlet: DotCMSContentlet): void {
        this.contentEditor.uveEditContentRequested({ contentlet });
    }
}
