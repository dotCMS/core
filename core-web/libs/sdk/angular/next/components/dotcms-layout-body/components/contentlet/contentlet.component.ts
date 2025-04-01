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

import { CUSTOM_NO_COMPONENT, getDotContentletAttributes } from '@dotcms/uve/internal';
import { DotCMSContentlet, DotContentletAttributes } from '@dotcms/uve/types';

import { DynamicComponentEntity } from '../../../../models';
import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';
import { FallbackComponent } from '../fallback-component/fallback-component.component';

/**
 * Contentlet component that renders DotCMS content with development mode support
 *
 * @component
 * @param {DotCMSContentlet} contentlet - The contentlet to be rendered
 * @param {string} container - The container identifier
 */
@Component({
    selector: 'dotcms-contentlet',
    standalone: true,
    imports: [FallbackComponent],
    template: `
        @if (UserComponent) {
            <ng-container
                *ngComponentOutlet="
                    UserComponent | async;
                    inputs: { contentlet: contentletSignal() ?? contentlet }
                " />
        } @else if (UserNoComponent && isDevMode()) {
            <dotcms-fallback-component
                [UserNoComponent]="UserNoComponent"
                [contentlet]="contentletSignal() ?? contentlet" />
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent implements OnChanges {
    @Input({ required: true }) contentlet!: DotCMSContentlet;
    @Input({ required: true }) container!: string;
    @ViewChild('contentletRef') contentletRef!: ElementRef;
    @HostBinding('attr.data-dot-object') dotObject = 'contentlet';

    #dotcmsContextService = inject(DotCMSContextService);

    contentletSignal = signal<DotCMSContentlet | null>(null);
    UserComponent: DynamicComponentEntity | null = null;
    UserNoComponent: DynamicComponentEntity | null = null;
    isDevMode = signal(false);
    haveContent = signal(false);
    style = computed(() => (this.isDevMode() && this.haveContent() ? { minHeight: '4rem' } : {}));
    dotAttributes = computed<DotContentletAttributes>(() => {
        const contentlet = this.contentletSignal();
        if (!contentlet) return {} as DotContentletAttributes;

        return getDotContentletAttributes(contentlet, this.container);
    });

    @HostBinding('attr.data-dot-identifier') identifier =
        this.dotAttributes()['data-dot-identifier'];
    @HostBinding('attr.data-dot-basetype') basetype = this.dotAttributes()['data-dot-basetype'];
    @HostBinding('attr.data-dot-title') title = this.dotAttributes()['data-dot-title'];
    @HostBinding('attr.data-dot-inode') inode = this.dotAttributes()['data-dot-inode'];
    @HostBinding('attr.data-dot-type') type = this.dotAttributes()['data-dot-type'];
    @HostBinding('attr.data-dot-container') containerAttribute =
        this.dotAttributes()['data-dot-container'];
    @HostBinding('attr.data-dot-on-number-of-pages') onNumberOfPages =
        this.dotAttributes()['data-dot-on-number-of-pages'];
    @HostBinding('style') styleAttribute = this.style();

    ngOnChanges() {
        this.contentletSignal.set(this.contentlet);
        this.isDevMode.set(this.#dotcmsContextService.isDevMode());
        this.setupComponents();
    }

    ngAfterViewInit() {
        this.checkContent();
    }

    private setupComponents() {
        const context = this.#dotcmsContextService.context;
        if (!context) return;

        if (!context?.components) return;

        this.UserComponent = context.components[this.contentlet?.contentType];
        this.UserNoComponent = context.components[CUSTOM_NO_COMPONENT];
    }

    private checkContent() {
        const element = this.contentletRef?.nativeElement;
        if (element) {
            const hasContent = element.children.length > 0;
            this.haveContent.set(hasContent);
        }
    }
}
