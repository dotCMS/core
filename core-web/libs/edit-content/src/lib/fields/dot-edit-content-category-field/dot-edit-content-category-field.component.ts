import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    DestroyRef,
    inject,
    input,
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

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentCategoryFieldSidebarComponent } from './components/dot-edit-content-category-field-sidebar/dot-edit-content-category-field-sidebar.component';
import { CLOSE_SIDEBAR_CSS_DELAY_MS } from './dot-edit-content-category-field.const';

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
        DotDynamicDirective
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
        '[class.dot-category-field__container--has-categories]': 'hasCategories()',
        '[class.dot-category-field__container]': '!hasCategories()'
    }
})
export class DotEditContentCategoryFieldComponent {
    disableSelectCategoriesButton = signal(false);
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;

    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     *
     * @name field
     * @description The variable represents a field of a DotCMS content type.
     */
    field = input.required<DotCMSContentTypeField>();

    // TODO: Replace with the content of the selected categories
    values = [];
    readonly #destroyRef = inject(DestroyRef);
    #componentRef: ComponentRef<DotEditContentCategoryFieldSidebarComponent>;

    /**
     * Checks if the object has categories.
     * @returns {boolean} - True if the object has categories, false otherwise.
     */
    hasCategories(): boolean {
        return this.values.length > 0;
    }

    /**
     * Open the "DotEditContentCategoryFieldDialogComponent" dialog to show categories.
     *
     * @returns {void}
     */
    showCategoriesSidebar(): void {
        this.disableSelectCategoriesButton.set(true);
        this.#componentRef = this.sidebarHost.viewContainerRef.createComponent(
            DotEditContentCategoryFieldSidebarComponent
        );

        this.setSidebarListener();
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
