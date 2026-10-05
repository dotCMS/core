import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { MarkdownComponent } from 'ngx-markdown';

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
            .map((title) => title.textContent.trim());
        expect(titles).toEqual([
            '/blogs/(.*) → myspa.blogs.com:3000',
            'apps.uve.route.all.pages → myspa.com:3000'
        ]);
        expect(
            spectator.queryAll(byTestId('uve-route-number')).map((n) => n.textContent.trim())
        ).toEqual(['#1', '#2']);
    });

    it('should title an empty route as new', () => {
        spectator.component.writeValue(SAMPLE);
        spectator.detectChanges();

        spectator.click(byTestId('uve-route-add'));

        expect(spectator.queryAll(byTestId('uve-route-title'))[2].textContent.trim()).toBe(
            'apps.uve.route.new'
        );
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
        expect(spectator.query('.p-error')).toBeFalsy();

        spectator.dispatchFakeEvent(byTestId('uve-route-url'), 'blur');
        spectator.detectChanges();

        expect(spectator.query('.p-error')).toBeTruthy();
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
