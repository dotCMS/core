import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    Input,
    OnChanges,
    HostBinding,
    signal,
    ViewChild
} from '@angular/core';

import { DotCMSBasicContentlet, EditableContainerData } from '@dotcms/types';
import { DotContentletAttributes } from '@dotcms/types/internal';
import { CUSTOM_NO_COMPONENT, getDotContentletAttributes } from '@dotcms/uve/internal';

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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent implements OnChanges {
    @Input({ required: true }) contentlet!: DotCMSBasicContentlet;
    @Input({ required: true }) containerData!: EditableContainerData;
    @ViewChild('contentletRef') contentletRef!: ElementRef;
    @HostBinding('attr.data-dot-object') dotObject = 'contentlet';

    #dotCMSStore = inject(DotCMSStore);

    $contentlet = signal<DotCMSBasicContentlet | null>(null);
    $UserComponent = signal<DynamicComponentEntity | null>(null);
    $UserNoComponent = signal<DynamicComponentEntity | null>(null);
    $isDevMode = this.#dotCMSStore.$isDevMode;
    $haveContent = signal(false);
    $style = computed(() =>
        this.$isDevMode() && !this.$haveContent() ? { minHeight: '4rem' } : {}
    );
    $dotAttributes = computed<DotContentletAttributes>(() => {
        const contentlet = this.$contentlet();
        if (!contentlet || !this.$isDevMode()) return {} as DotContentletAttributes;

        return getDotContentletAttributes(contentlet, this.containerData.identifier);
    });

    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;
    @HostBinding('attr.data-dot-basetype') basetype: string | null = null;
    @HostBinding('attr.data-dot-title') title: string | null = null;
    @HostBinding('attr.data-dot-inode') inode: string | null = null;
    @HostBinding('attr.data-dot-type') type: string | null = null;
    @HostBinding('attr.data-dot-container') containerAttribute: string | null = null;
    @HostBinding('attr.data-dot-on-number-of-pages') onNumberOfPages: string | null = null;
    @HostBinding('style') styleAttribute: { [key: string]: unknown } | null = null;

    ngOnChanges() {
        this.$contentlet.set(this.contentlet);
        this.setupComponents();

        this.identifier = this.$dotAttributes()['data-dot-identifier'];
        this.basetype = this.$dotAttributes()['data-dot-basetype'];
        this.title = this.$dotAttributes()['data-dot-title'];
        this.inode = this.$dotAttributes()['data-dot-inode'];
        this.type = this.$dotAttributes()['data-dot-type'];
        this.containerAttribute = JSON.stringify(this.containerData);
        this.onNumberOfPages = this.$dotAttributes()['data-dot-on-number-of-pages'];
        this.styleAttribute = this.$style();
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
