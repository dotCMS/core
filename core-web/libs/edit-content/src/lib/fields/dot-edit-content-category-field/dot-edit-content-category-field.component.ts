import { KeyValuePipe, NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit,
    signal,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

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
 * Component for editing content category field.
 *
 * @class
 * @name DotEditContentCategoryFieldComponent
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
        DotDynamicDirective,
        KeyValuePipe
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    styleUrl: './dot-edit-content-category-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    // eslint-disable-next-line @angular-eslint/no-host-metadata-property
    host: {
        '[class.dot-category-field__container--has-categories]': 'hasSelectedCategories()',
        '[class.dot-category-field__container]': '!hasSelectedCategories()'
    },
    providers: [CategoriesService, CategoryFieldStore]
})
export class DotEditContentCategoryFieldComponent implements OnInit {
    disableSelectCategoriesButton = signal(false);
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;

    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     *
     * @name field
     * @description The variable represents a field of a DotCMS content type.
     */
    field = input.required<DotCMSContentTypeField>();

    /**
     * Represents a DotCMS contentlet.
     *
     */
    contentlet = input.required<DotCMSContentlet>();

    readonly store = inject(CategoryFieldStore);

    hasSelectedCategories = computed(() => {
        return !!this.store.selectedCategories().length;
    });

    readonly #destroyRef = inject(DestroyRef);
    #componentRef: ComponentRef<DotCategoryFieldSidebarComponent>;

    /**
     * Open the "DotEditContentCategoryFieldDialogComponent" dialog to show categories.
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
                this.disableSelectCategoriesButton.set(false);
                this.sidebarHost.viewContainerRef.clear();
            });
    }
}
