import { exposeComponent } from '@hashbrownai/angular';
import { s } from '@hashbrownai/core';

import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

@Component({
    selector: 'app-content-type-card',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [TagModule, ButtonModule, RouterLink],
    template: `
        <article class="flex h-full flex-col rounded-lg border border-surface-200 bg-surface-0 p-3">
            <div class="flex items-start gap-2">
                <span
                    class="mt-1 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-100">
                    <i [class]="iconClass()"></i>
                </span>
                <div class="min-w-0 flex-1 space-y-1">
                    <h3 class="m-0 text-base font-semibold text-surface-900">{{ name() }}</h3>

                    @if (groupLabel()) {
                        <p-tag [value]="groupLabel()" severity="secondary" />
                    }

                    <p class="m-0 text-sm text-surface-700">
                        <strong>Variable:</strong>
                        {{ variable() }}
                    </p>
                    <p class="m-0 text-sm text-surface-700">
                        <strong>Type:</strong>
                        {{ type() }}
                    </p>
                </div>
            </div>

            <div class="mt-auto flex justify-center pt-3">
                <button
                    pButton
                    type="button"
                    [routerLink]="detailRoute()"
                    label="View details"
                    size="small"
                    severity="primary"
                    icon="pi pi-arrow-right"></button>
            </div>
        </article>
    `
})
export class ContentTypeCardComponent {
    private readonly defaultDetailRoute =
        '/content-types-angular/edit/f6259cc9-5d78-453e-8167-efd7b72b2e96';

    readonly name = input.required<string>();
    readonly variable = input.required<string>();
    readonly type = input.required<string>();
    readonly action = input<string>('');
    readonly groupLabel = input<string>('');
    readonly iconClass = computed(() => this.getIconClass(this.type()));

    detailRoute(): string {
        return this.action() || this.defaultDetailRoute;
    }

    private getIconClass(type: string): string {
        const normalizedType = type.toUpperCase();

        switch (normalizedType) {
            case 'CONTENT':
                return 'pi pi-file text-blue-600';
            case 'WIDGET':
                return 'pi pi-th-large text-purple-600';
            case 'FILE':
            case 'FILEASSET':
                return 'pi pi-image text-emerald-600';
            case 'PAGE':
                return 'pi pi-globe text-orange-600';
            case 'PERSONA':
                return 'pi pi-user text-cyan-600';
            default:
                return 'pi pi-box text-surface-600';
        }
    }
}

export const AiContentTypeCardComponent = exposeComponent(ContentTypeCardComponent, {
    description:
        'Displays a dotCMS content type card with metadata and a details navigation button',
    input: {
        name: s.string('The content type name'),
        variable: s.string('The content type variable'),
        type: s.string('The content type base type label'),
        action: s.string('Optional Angular route for content type details'),
        groupLabel: s.string('Optional content type group label')
    }
});
