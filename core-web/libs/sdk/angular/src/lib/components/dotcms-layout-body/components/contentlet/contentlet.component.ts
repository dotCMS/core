import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    Input,
    OnChanges,
    signal,
    ViewChild
} from '@angular/core';

import { DotCMSBasicContentlet, EditableContainerData } from '@dotcms/types';
import { DotContentletAttributes } from '@dotcms/types/internal';
import {
    CUSTOM_NO_COMPONENT,
    getAnalyticsContentletAttributes,
    getDotContentletAttributes
} from '@dotcms/uve/internal';

import { DynamicComponentEntity } from '../../../../models';
import { DotCMSStore } from '../../../../store/dotcms.store';
import { FallbackComponent } from '../fallback-component/fallback-component.component';

/**
 * @description Contentlet component that renders DotCMS content with development mode support
 *
 * @component
 * @param {DotCMSContentlet} contentlet - The contentlet to be rendered
 * @param {string} container - The container identifier
 * @class ContentletComponent
 */
@Component({
    selector: 'dotcms-contentlet',
    imports: [FallbackComponent, AsyncPipe, NgComponentOutlet],
    template: `
        @if ($UserComponent()) {
            <ng-container
                *ngComponentOutlet="
                    $UserComponent() | async;
                    inputs: { contentlet: $contentlet() ?? contentlet }
                " />
        } @else if ($isDevMode()) {
            <dotcms-fallback-component
                [UserNoComponent]="$UserNoComponent()"
                [contentlet]="$contentlet() ?? contentlet" />
        }
    `,
    // Editor-only metadata (data-dot-object, data-dot-container, etc.) is bound
    // only in edit mode. In live mode we keep the minimal set Analytics needs
    // via $dotAttributes (empty when Analytics is inactive).
    host: {
        '[attr.data-dot-object]': "$isDevMode() ? 'contentlet' : null",
        '[attr.data-dot-identifier]': "$dotAttributes()['data-dot-identifier'] ?? null",
        '[attr.data-dot-basetype]': "$dotAttributes()['data-dot-basetype'] ?? null",
        '[attr.data-dot-title]': "$dotAttributes()['data-dot-title'] ?? null",
        '[attr.data-dot-inode]': "$dotAttributes()['data-dot-inode'] ?? null",
        '[attr.data-dot-type]': "$dotAttributes()['data-dot-type'] ?? null",
        '[attr.data-dot-container]': '$isDevMode() ? getContainerAttribute() : null',
        '[attr.data-dot-on-number-of-pages]':
            "$dotAttributes()['data-dot-on-number-of-pages'] ?? null",
        '[attr.data-dot-style-properties]': "$dotAttributes()['data-dot-style-properties'] ?? null",
        '[style]': '$style()'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent implements OnChanges {
    @Input({ required: true }) contentlet!: DotCMSBasicContentlet;
    @Input({ required: true }) containerData!: EditableContainerData;
    @ViewChild('contentletRef') contentletRef!: ElementRef;

    #dotCMSStore = inject(DotCMSStore);

    $contentlet = signal<DotCMSBasicContentlet | null>(null);
    $UserComponent = signal<DynamicComponentEntity | null>(null);
    $UserNoComponent = signal<DynamicComponentEntity | null>(null);
    $isDevMode = this.#dotCMSStore.$isDevMode;
    $isAnalyticsActive = this.#dotCMSStore.$isAnalyticsActive;
    $haveContent = signal(false);
    $style = computed(() =>
        this.$isDevMode() && !this.$haveContent() ? { minHeight: '4rem' } : {}
    );
    $dotAttributes = computed<DotContentletAttributes>(() => {
        const contentlet = this.$contentlet();
        if (!contentlet) return {} as DotContentletAttributes;

        if (this.$isDevMode()) {
            return getDotContentletAttributes(contentlet, this.containerData.identifier);
        }

        if (this.$isAnalyticsActive()) {
            return getAnalyticsContentletAttributes(contentlet) as DotContentletAttributes;
        }

        return {} as DotContentletAttributes;
    });

    ngOnChanges() {
        this.$contentlet.set(this.contentlet);
        this.setupComponents();
    }

    /**
     * Serializes the container data for the `data-dot-container` editor attribute.
     * Only consumed by the host binding while in edit mode.
     */
    getContainerAttribute(): string {
        return JSON.stringify(this.containerData);
    }

    ngAfterViewInit() {
        this.checkContent();
    }

    private setupComponents() {
        const store = this.#dotCMSStore.store;
        if (!store) return;

        if (!store?.components) return;

        this.$UserComponent.set(store.components[this.contentlet?.contentType]);
        this.$UserNoComponent.set(store.components[CUSTOM_NO_COMPONENT]);
    }

    private checkContent() {
        const element = this.contentletRef?.nativeElement;
        if (element) {
            const hasContent = element.getBoundingClientRect().height > 0;
            this.$haveContent.set(hasContent);
        }
    }
}
