import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    DestroyRef,
    inject,
    input,
    OnInit,
    signal,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ChipsModule } from 'primeng/chips';
import { TooltipModule } from 'primeng/tooltip';

import { delay } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldSidebarComponent } from './components/dot-category-field-sidebar/dot-category-field-sidebar.component';
import { CLOSE_SIDEBAR_CSS_DELAY_MS } from './dot-edit-content-category-field.const';
import { CategoriesService } from './services/categories.service';
import { CategoryFieldStore } from './store/content-category-field.store';

/**
 * @class
 * @name DotEditContentCategoryFieldComponent
 * @description Angular component for editing a content category field.
 *
 * The `DotEditContentCategoryFieldComponent` component provides functionality for editing a content category field.
 * It is responsible for handling user interactions and updating the state of the component.
 */
@Component({
    selector: 'dot-edit-content-category-field',
    standalone: true,
    imports: [
        ChipsModule,
        ReactiveFormsModule,
        ButtonModule,
        ChipModule,
        NgClass,
        TooltipModule,
        DotMessagePipe,
        DotDynamicDirective
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    styleUrl: './dot-edit-content-category-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.dot-category-field__container--has-categories]': 'hasSelectedCategories()',
        '[class.dot-category-field__container]': '!hasSelectedCategories()'
    },
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [CategoriesService, CategoryFieldStore]
})
export class DotEditContentCategoryFieldComponent implements OnInit {
    /**
     * Disable the button to open the sidebar
     */
    disableSelectCategoriesButton = signal(false);

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;

    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     * @description The variable represents a field of a DotCMS content type and is a required input.
     */
    field = input.required<DotCMSContentTypeField>();

    /**
     * Represents a DotCMS contentlet and is a required input
     * @description DotCMSContentlet input representing a DotCMS contentlet.
     */
    contentlet = input.required<DotCMSContentlet>();

    readonly store = inject(CategoryFieldStore);
    readonly #form = inject(ControlContainer).control as FormGroup;
    readonly #destroyRef = inject(DestroyRef);
    #componentRef: ComponentRef<DotCategoryFieldSidebarComponent>;

    /**
     * Determines if there are any selected categories.
     *
     * @returns {Boolean} - True if there are selected categories, false otherwise.
     */
    hasSelectedCategories(): boolean {
        return !!this.store.hasSelectedCategories();
    }

    /**
     * Open the "DotEditContentCategoryFieldDialogComponent" dialog to show the list of categories.
     *
     * @returns {void}
     */
    showCategoriesSidebar(): void {
        this.disableSelectCategoriesButton.set(true);
        this.#componentRef = this.sidebarHost.viewContainerRef.createComponent(
            DotCategoryFieldSidebarComponent
        );

        this.setSidebarListener();
    }

    ngOnInit(): void {
        this.store.load(this.field(), this.contentlet());
    }

    private setSidebarListener() {
        this.#componentRef.instance.closedSidebar
            .pipe(takeUntilDestroyed(this.#destroyRef), delay(CLOSE_SIDEBAR_CSS_DELAY_MS))
            .subscribe(() => {
                this.updateCategoryFieldControl();
                // enable the show sidebar button
                this.disableSelectCategoriesButton.set(false);
                this.removeDotCategoryFieldSidebarComponent();
            });
    }

    private updateCategoryFieldControl(): void {
        this.#form
            .get(this.store.fieldVariableName())
            .setValue(this.store.selectedCategoriesValues());
    }

    private removeDotCategoryFieldSidebarComponent() {
        this.sidebarHost.viewContainerRef.clear();
    }
}
