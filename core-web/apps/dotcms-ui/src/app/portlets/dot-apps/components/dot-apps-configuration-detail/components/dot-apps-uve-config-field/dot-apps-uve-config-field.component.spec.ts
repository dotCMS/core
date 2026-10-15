import {
    byTestId,
    createComponentFactory,
    createHostFactory,
    mockProvider,
    Spectator,
    SpectatorHost
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { MarkdownComponent } from 'ngx-markdown';

import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { DotAppsUveConfigFieldComponent } from './dot-apps-uve-config-field.component';

import { DotAppsConfigurationDetailJsonFieldComponent } from '../dot-apps-configuration-detail-json-field/dot-apps-configuration-detail-json-field.component';

import type { Mock } from 'vitest';

const SAMPLE = JSON.stringify({
    config: [
        { pattern: '/blogs/(.*)', url: 'https://myspa.blogs.com:3000' },
        { pattern: '.*', url: 'https://myspa.com:3000' }
    ]
});

describe('DotAppsUveConfigFieldComponent', () => {
    let spectator: Spectator<DotAppsUveConfigFieldComponent>;
    let onChange: Mock<(value: string) => void>;

    const createComponent = createComponentFactory({
        component: DotAppsUveConfigFieldComponent,
        overrideComponents: [
            [
                DotAppsUveConfigFieldComponent,
                {
                    remove: {
                        imports: [DotAppsConfigurationDetailJsonFieldComponent, MarkdownComponent]
                    },
                    add: {
                        imports: [
                            MockComponent(DotAppsConfigurationDetailJsonFieldComponent),
                            MockComponent(MarkdownComponent)
                        ]
                    }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })],
        detectChanges: false
    });

    /** The last JSON the field sent to the form, parsed. */
    const lastEmitted = (): { config: { pattern: string; url: string; options?: unknown }[] } =>
        JSON.parse(onChange.mock.calls.at(-1)?.[0] ?? '{}');

    beforeEach(() => {
        spectator = createComponent({
            props: { fieldId: 'configuration', hint: 'UVE help' } as never
        });
        onChange = vi.fn<(value: string) => void>();
        spectator.component.registerOnChange(onChange);
    });

    it('should show one card per saved route', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        expect(spectator.queryAll(byTestId('uve-route-pattern')).length).toBe(2);
        expect(spectator.queryAll(byTestId('uve-route-default-tag')).length).toBe(1);
    });

    it('should title each route with its pattern and server host', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        const titles = spectator
            .queryAll(byTestId('uve-route-title'))
            .map((title) => title.getAttribute('title'));
        expect(titles).toEqual([
            '/blogs/(.*) → myspa.blogs.com:3000',
            'apps.uve.route.all.pages → myspa.com:3000'
        ]);
        expect(
            spectator.queryAll(byTestId('uve-route-title-from')).map((n) => n.textContent?.trim())
        ).toEqual(['/blogs/(.*)', 'apps.uve.route.all.pages']);
        expect(
            spectator.queryAll(byTestId('uve-route-title-to')).map((n) => n.textContent?.trim())
        ).toEqual(['myspa.blogs.com:3000', 'myspa.com:3000']);
        expect(
            spectator.queryAll(byTestId('uve-route-number')).map((n) => n.textContent.trim())
        ).toEqual(['#1', '#2']);
    });

    it('should title an empty route as new', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(byTestId('uve-route-add'));

        const newTitle = spectator.queryAll(byTestId('uve-route-title'))[2];
        expect(newTitle.textContent?.trim()).toBe('apps.uve.route.new');
        expect(newTitle.querySelector('[data-testid="uve-route-title-to"]')).toBeNull();
    });

    it('should start with a catch-all route when nothing is saved', () => {
        spectator.component.writeValue('');
        spectator.detectChanges();

        const pattern = spectator.query<HTMLInputElement>(byTestId('uve-route-pattern'));
        expect(pattern?.value).toBe('.*');
    });

    it('should add a new route at the end', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(byTestId('uve-route-add'));

        expect(lastEmitted().config.map((route) => route.pattern)).toEqual([
            '/blogs/(.*)',
            '.*',
            ''
        ]);
    });

    it('should warn about routes placed after the catch-all route', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();
        expect(spectator.query(byTestId('uve-route-unreachable-tag'))).toBeFalsy();

        spectator.click(byTestId('uve-route-add'));

        expect(spectator.queryAll(byTestId('uve-route-unreachable-tag')).length).toBe(1);
        expect(spectator.query(byTestId('uve-route-2'))?.textContent).toContain(
            'apps.uve.route.unreachable.hint'
        );
    });

    it('should move a route down', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(spectator.queryAll(byTestId('uve-route-move-down'))[0]);

        expect(lastEmitted().config.map((route) => route.pattern)).toEqual(['.*', '/blogs/(.*)']);
    });

    it('should move a route up', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(spectator.queryAll(byTestId('uve-route-move-up'))[1]);

        expect(lastEmitted().config.map((route) => route.pattern)).toEqual(['.*', '/blogs/(.*)']);
    });

    it('should disable move up on the first route and move down on the last', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        const up = spectator.queryAll<HTMLButtonElement>(byTestId('uve-route-move-up'));
        const down = spectator.queryAll<HTMLButtonElement>(byTestId('uve-route-move-down'));
        expect(up[0].disabled).toBe(true);
        expect(down[1].disabled).toBe(true);
    });

    it('should remove a route', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(spectator.queryAll(byTestId('uve-route-remove'))[0]);

        expect(lastEmitted().config.map((route) => route.pattern)).toEqual(['.*']);
        expect(spectator.queryAll(byTestId('uve-route-pattern')).length).toBe(1);
    });

    it('should not allow removing the only route', () => {
        spectator.component.writeValue('');
        spectator.detectChanges();

        expect(spectator.query<HTMLButtonElement>(byTestId('uve-route-remove'))?.disabled).toBe(
            true
        );
    });

    it('should remove a dev URL and drop the empty options', () => {
        spectator.component.writeValue(
            JSON.stringify({
                config: [
                    {
                        pattern: '.*',
                        url: 'https://a.com',
                        options: { allowedDevURLs: ['http://localhost:3000'] }
                    }
                ]
            })
        );
        spectator.detectChanges();

        spectator.click(byTestId('uve-route-dev-url-remove'));

        expect(spectator.query(byTestId('uve-route-dev-url'))).toBeFalsy();
        expect(lastEmitted().config[0]).toEqual({ pattern: '.*', url: 'https://a.com' });
    });

    it('should keep the routes when switching to JSON and back', async () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        const modeButton = (label: string) =>
            spectator
                .queryAll<HTMLElement>('[data-testid="uve-mode-toggle"] p-togglebutton')
                .find((button) => button.textContent?.includes(label));

        spectator.click(modeButton('apps.uve.mode.json'));
        // ngModel writes to the toggle asynchronously; let it settle before the next click.
        await spectator.fixture.whenStable();
        spectator.detectChanges();
        expect(spectator.query(byTestId('uve-json-editor'))).toBeTruthy();

        spectator.click(modeButton('apps.uve.mode.form'));
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        expect(spectator.query(byTestId('uve-json-editor'))).toBeFalsy();
        expect(
            spectator
                .queryAll<HTMLInputElement>(byTestId('uve-route-pattern'))
                .map((input) => input.value)
        ).toEqual(['/blogs/(.*)', '.*']);
        expect(lastEmitted()).toEqual(JSON.parse(SAMPLE));
    });

    it('should make every control read-only when disabled', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.component.setDisabledState(true);
        spectator.detectChanges();

        expect(spectator.query<HTMLFieldSetElement>('fieldset')?.disabled).toBe(true);
        expect(
            spectator
                .queryAll<HTMLButtonElement>(byTestId('uve-route-remove'))
                .every((button) => button.disabled)
        ).toBe(true);
    });

    it('should emit the edited server URL as JSON', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.typeInElement(
            'https://new.com',
            spectator.queryAll(byTestId('uve-route-url'))[1]
        );

        expect(lastEmitted().config[1].url).toBe('https://new.com');
    });

    it('should add a dev URL under options.allowedDevURLs', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(spectator.queryAll(byTestId('uve-route-dev-url-add'))[1]);
        spectator.typeInElement('http://localhost:3000', byTestId('uve-route-dev-url'));

        expect(lastEmitted().config[1].options).toEqual({
            allowedDevURLs: ['http://localhost:3000']
        });
    });

    it('should be invalid while a route is incomplete', () => {
        spectator.component.writeValue(SAMPLE);
        expect(spectator.component.validate()).toBeNull();

        spectator.detectChanges();
        spectator.click(byTestId('uve-route-add'));

        expect(spectator.component.validate()).toEqual({ uveInvalidRoutes: true });
    });

    it('should only show errors after the user leaves a field', () => {
        spectator.component.writeValue('');
        spectator.detectChanges();
        expect(spectator.query(byTestId('uve-route-url-error'))).toBeFalsy();

        spectator.dispatchFakeEvent(byTestId('uve-route-url'), 'blur');
        spectator.detectChanges();

        expect(spectator.query(byTestId('uve-route-url-error'))?.textContent?.trim()).toBe(
            'apps.uve.route.error.required'
        );
    });

    it('should show the RegEx and dev URL errors with their messages', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.typeInElement('([', spectator.queryAll(byTestId('uve-route-pattern'))[0]);
        spectator.click(spectator.queryAll(byTestId('uve-route-dev-url-add'))[0]);
        spectator.typeInElement('localhost', byTestId('uve-route-dev-url'));
        spectator.dispatchFakeEvent(byTestId('uve-route-dev-url'), 'blur');
        spectator.detectChanges();

        expect(spectator.query(byTestId('uve-route-pattern-error'))?.textContent?.trim()).toBe(
            'apps.uve.route.error.pattern'
        );
        expect(spectator.query(byTestId('uve-route-dev-url-error'))?.textContent?.trim()).toBe(
            'apps.uve.route.error.url'
        );
    });

    it('should show the hint only on the JSON tab', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();
        expect(spectator.query(byTestId('uve-json-hint'))).toBeFalsy();

        spectator.component.writeValue('{"something": "else"}');
        spectator.detectChanges();

        expect(spectator.query(byTestId('uve-json-hint'))).toBeTruthy();
    });

    it('should open the JSON tab when the saved value is not route-shaped', () => {
        spectator.component.writeValue('{"something": "else"}');
        spectator.detectChanges();

        expect(spectator.query(byTestId('uve-json-editor'))).toBeTruthy();
        expect(spectator.query(byTestId('uve-form-blocked'))).toBeTruthy();
    });
});

describe('DotAppsUveConfigFieldComponent in a reactive form', () => {
    let spectator: SpectatorHost<DotAppsUveConfigFieldComponent, { control: FormControl<string> }>;

    const createHost = createHostFactory({
        component: DotAppsUveConfigFieldComponent,
        imports: [ReactiveFormsModule],
        overrideComponents: [
            [
                DotAppsUveConfigFieldComponent,
                {
                    remove: {
                        imports: [DotAppsConfigurationDetailJsonFieldComponent, MarkdownComponent]
                    },
                    add: {
                        imports: [
                            MockComponent(DotAppsConfigurationDetailJsonFieldComponent),
                            MockComponent(MarkdownComponent)
                        ]
                    }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        spectator = createHost(
            '<dot-apps-uve-config-field fieldId="configuration" [formControl]="control" />',
            { hostProps: { control: new FormControl(SAMPLE, { nonNullable: true }) } }
        );
    });

    it('should push UI edits to the form control value', () => {
        spectator.typeInElement(
            'https://new.com',
            spectator.queryAll(byTestId('uve-route-url'))[1]
        );

        expect(JSON.parse(spectator.hostComponent.control.value).config[1].url).toBe(
            'https://new.com'
        );
    });

    it('should mark the form control invalid while a route is broken, and valid again once fixed', () => {
        expect(spectator.hostComponent.control.valid).toBe(true);

        const url = spectator.queryAll(byTestId('uve-route-url'))[1];
        spectator.typeInElement('not-a-url', url);
        expect(spectator.hostComponent.control.errors).toEqual({ uveInvalidRoutes: true });

        spectator.typeInElement('https://fixed.com', url);
        expect(spectator.hostComponent.control.errors).toBeNull();
    });

    it('should render the value set on the form control', () => {
        spectator.hostComponent.control.setValue(
            JSON.stringify({ config: [{ pattern: '/docs/(.*)', url: 'https://docs.com' }] })
        );
        spectator.detectChanges();

        expect(
            spectator
                .queryAll<HTMLInputElement>(byTestId('uve-route-pattern'))
                .map((input) => input.value)
        ).toEqual(['/docs/(.*)']);
    });
});
