import { MarkdownComponent } from 'ngx-markdown';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    forwardRef,
    inject,
    input,
    signal
} from '@angular/core';
import {
    ControlValueAccessor,
    FormsModule,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validator
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    createUveRoute,
    hasUveRouteErrors,
    isHttpUrl,
    parseUveConfig,
    serializeUveConfig,
    UVE_CATCH_ALL_PATTERN,
    UveRoute,
    validateUveRoute
} from './dot-apps-uve-config.utils';

import { DotAppsCodeBlocksDirective } from '../../directives/dot-apps-code-block.directive';
import {
    DotAppsConfigurationDetailJsonFieldComponent,
    getJsonParseError
} from '../dot-apps-configuration-detail-json-field/dot-apps-configuration-detail-json-field.component';

type EditorMode = 'form' | 'json';

/**
 * A route card title. `from` and `to` are shown as two halves that truncate on their own;
 * `to` is null for a route with nothing filled in, which shows `from` alone.
 */
interface UveRouteTitle {
    from: string;
    to: string | null;
    full: string;
}

/**
 * Form control for the UVE `configuration` param. Instead of raw JSON, it shows one card per
 * URL route (path pattern, server URL, allowed dev URLs) that can be added, removed and
 * reordered. A JSON tab keeps the raw editor for pasting or for keys the form doesn't know.
 * The value given to the form is always the JSON string the UVE reads.
 */
@Component({
    selector: 'dot-apps-uve-config-field',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        SelectButtonModule,
        TagModule,
        TooltipModule,
        DotMessagePipe,
        MarkdownComponent,
        DotAppsCodeBlocksDirective,
        DotAppsConfigurationDetailJsonFieldComponent
    ],
    templateUrl: './dot-apps-uve-config-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotAppsUveConfigFieldComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DotAppsUveConfigFieldComponent),
            multi: true
        }
    ]
})
export class DotAppsUveConfigFieldComponent implements ControlValueAccessor, Validator {
    readonly #dotMessageService = inject(DotMessageService);

    /** Id given to the field wrapper, so the field label can point at it. */
    $fieldId = input.required<string>({ alias: 'fieldId' });

    /** Accessible name of the whole route editor, which a `<label for>` can't provide. */
    $label = input<string>('', { alias: 'label' });

    /** Markdown help for the raw JSON, shown only on the JSON tab; the route form explains itself. */
    $hint = input<string>('', { alias: 'hint' });

    readonly catchAllPattern = UVE_CATCH_ALL_PATTERN;

    readonly $mode = signal<EditorMode>('form');
    readonly $routes = signal<UveRoute[]>([]);
    readonly $jsonText = signal<string>('');
    readonly $isDisabled = signal<boolean>(false);
    /** Errors stay hidden until the user leaves a field, so a new route isn't red at once. */
    readonly $showErrors = signal<boolean>(false);
    /** Set when the JSON can't be shown as routes, to explain why the form tab is blocked. */
    readonly $formBlocked = signal<boolean>(false);

    readonly $routeErrors = computed(() => this.$routes().map((route) => validateUveRoute(route)));

    /**
     * For each route, the number of an earlier catch-all route that matches every page first,
     * which means this route is never used. `null` when the route can be reached.
     */
    readonly $shadowedBy = computed(() => {
        const catchAll = this.$routes().findIndex(
            (route) => route.pattern.trim() === UVE_CATCH_ALL_PATTERN
        );

        return this.$routes().map((_, i) => (catchAll >= 0 && i > catchAll ? catchAll + 1 : null));
    });

    /** Card titles that say what each route does, e.g. "/blogs/(.*) → myspa.blogs.com". */
    readonly $titles = computed(() => this.$routes().map((route) => this.#getRouteTitle(route)));

    readonly $modeOptions = computed(() => [
        {
            label: this.#dotMessageService.get('apps.uve.mode.form'),
            value: 'form',
            disabled: this.$formBlocked()
        },
        { label: this.#dotMessageService.get('apps.uve.mode.json'), value: 'json' }
    ]);

    #onChange = (_value: string) => {
        // Replaced by registerOnChange
    };

    #onTouched = () => {
        // Replaced by registerOnTouched
    };

    #onValidatorChange = () => {
        // Replaced by registerOnValidatorChange
    };

    writeValue(value: string): void {
        const text = value || '';
        const routes = parseUveConfig(text);
        this.$jsonText.set(text);

        if (routes === null) {
            // Not a shape the form understands: show the raw JSON so nothing is lost.
            this.$formBlocked.set(true);
            this.$mode.set('json');

            return;
        }

        this.$formBlocked.set(false);
        this.$mode.set('form');
        this.$routes.set(routes.length ? routes : [createUveRoute(UVE_CATCH_ALL_PATTERN)]);
    }

    registerOnChange(fn: (value: string) => void): void {
        this.#onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.#onTouched = fn;
    }

    registerOnValidatorChange(fn: () => void): void {
        this.#onValidatorChange = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /** Invalid while any route has a problem, or while the JSON tab holds broken JSON. */
    validate(): ValidationErrors | null {
        if (this.$mode() === 'json') {
            const error = getJsonParseError(this.$jsonText());

            return error ? { invalidJson: { message: error } } : null;
        }

        if (!this.$routes().length) {
            return { uveNoRoutes: true };
        }

        return this.$routeErrors().some(hasUveRouteErrors) ? { uveInvalidRoutes: true } : null;
    }

    /**
     * Switches between the route cards and the raw JSON editor, carrying the current value over.
     *
     * @param mode the tab the user picked
     */
    protected onModeChange(mode: EditorMode | null): void {
        if (!mode || mode === this.$mode()) {
            return;
        }

        if (mode === 'json') {
            this.$jsonText.set(serializeUveConfig(this.$routes()));
            this.$mode.set('json');
            this.#onValidatorChange();

            return;
        }

        const routes = parseUveConfig(this.$jsonText());
        if (routes === null) {
            this.$formBlocked.set(true);

            return;
        }

        this.$routes.set(routes.length ? routes : [createUveRoute(UVE_CATCH_ALL_PATTERN)]);
        this.$mode.set('form');
        this.#emitRoutes();
    }

    /**
     * Keeps the raw JSON in sync and re-checks whether the form tab can open it.
     *
     * @param value the editor content
     */
    protected onJsonChange(value: string): void {
        this.$jsonText.set(value);
        this.$formBlocked.set(parseUveConfig(value) === null);
        this.#onChange(value);
        this.#onTouched();
    }

    /** Adds an empty route at the end of the list. */
    protected addRoute(): void {
        this.$routes.set([...this.$routes(), createUveRoute()]);
        this.#emitRoutes();
    }

    /**
     * Removes a route.
     *
     * @param index position of the route
     */
    protected removeRoute(index: number): void {
        this.$routes.set(this.$routes().filter((_, i) => i !== index));
        this.#emitRoutes();
    }

    /**
     * Moves a route one place up or down, since routes are matched top to bottom.
     *
     * @param index position of the route
     * @param offset -1 to move up, 1 to move down
     */
    protected moveRoute(index: number, offset: -1 | 1): void {
        const target = index + offset;
        const routes = [...this.$routes()];
        if (target < 0 || target >= routes.length) {
            return;
        }

        [routes[index], routes[target]] = [routes[target], routes[index]];
        this.$routes.set(routes);
        this.#emitRoutes();
    }

    /**
     * Updates the pattern or the server URL of a route.
     *
     * @param index position of the route
     * @param key the field being edited
     * @param value the new text
     */
    protected updateRoute(index: number, key: 'pattern' | 'url', value: string): void {
        this.#patchRoute(index, (route) => ({ ...route, [key]: value }));
    }

    /**
     * Adds an empty dev URL row to a route.
     *
     * @param index position of the route
     */
    protected addDevUrl(index: number): void {
        this.#patchRoute(index, (route) => ({
            ...route,
            allowedDevURLs: [...route.allowedDevURLs, '']
        }));
    }

    /**
     * Updates one dev URL of a route.
     *
     * @param index position of the route
     * @param urlIndex position of the dev URL
     * @param value the new text
     */
    protected updateDevUrl(index: number, urlIndex: number, value: string): void {
        this.#patchRoute(index, (route) => ({
            ...route,
            allowedDevURLs: route.allowedDevURLs.map((devUrl, i) =>
                i === urlIndex ? value : devUrl
            )
        }));
    }

    /**
     * Removes one dev URL of a route.
     *
     * @param index position of the route
     * @param urlIndex position of the dev URL
     */
    protected removeDevUrl(index: number, urlIndex: number): void {
        this.#patchRoute(index, (route) => ({
            ...route,
            allowedDevURLs: route.allowedDevURLs.filter((_, i) => i !== urlIndex)
        }));
    }

    /** Called when the user leaves any input: from now on, errors are shown. */
    protected onBlur(): void {
        this.$showErrors.set(true);
        this.#onTouched();
    }

    #getRouteTitle(route: UveRoute): UveRouteTitle {
        const pattern = route.pattern.trim();
        const url = route.url.trim();

        if (!pattern && !url) {
            const text = this.#dotMessageService.get('apps.uve.route.new');

            return { from: text, to: null, full: text };
        }

        const from =
            pattern === UVE_CATCH_ALL_PATTERN
                ? this.#dotMessageService.get('apps.uve.route.all.pages')
                : pattern || '…';
        const to = isHttpUrl(url) ? new URL(url).host : url || '…';

        return { from, to, full: `${from} → ${to}` };
    }

    #patchRoute(index: number, update: (route: UveRoute) => UveRoute): void {
        this.$routes.set(this.$routes().map((route, i) => (i === index ? update(route) : route)));
        this.#emitRoutes();
    }

    #emitRoutes(): void {
        const json = serializeUveConfig(this.$routes());
        this.$jsonText.set(json);
        this.#onChange(json);
        this.#onValidatorChange();
    }
}
