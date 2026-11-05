import { createPipeFactory, SpectatorPipe } from '@openng/spectator/vitest';

import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotMessagePipe } from './dot-message.pipe';

describe('DotMessagePipe', () => {
    let spectator: SpectatorPipe<DotMessagePipe>;

    const mockMessages = {
        'apps.search.placeholder': 'Search',
        'apps.best': 'Test {0} {1}'
    };

    const messageServiceMock = new MockDotMessageService(mockMessages);

    const createPipe = createPipeFactory({
        pipe: DotMessagePipe,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    describe('bundle recovery', () => {
        @Component({
            imports: [DotMessagePipe],
            changeDetection: ChangeDetectionStrategy.OnPush,
            template: `
                <span data-testid="label">{{ 'apps.best' | dm: args }}</span>
            `
        })
        class PersistentLabel {
            args = ['Costa Rica', 'Panama'];
        }

        const setup = (initialVersion: string | null = null) => {
            const loadedBundleVersion = signal(initialVersion);
            const loadedBundleLanguage = signal<string | null>(initialVersion ? 'default' : null);
            let translation = initialVersion ? 'Old label' : 'apps.best';
            const get = vi.fn((_key: string, ...args: string[]) =>
                translation.replace('{0}', args[0]).replace('{1}', args[1])
            );
            TestBed.configureTestingModule({
                imports: [PersistentLabel],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: { get, loadedBundleVersion, loadedBundleLanguage }
                    }
                ]
            });
            const fixture = TestBed.createComponent(PersistentLabel);
            fixture.detectChanges();
            return {
                fixture,
                get,
                loadedBundleVersion,
                install: (version: string, message: string, language = 'default') => {
                    translation = message;
                    loadedBundleVersion.set(version);
                    loadedBundleLanguage.set(language);
                    fixture.detectChanges();
                },
                label: () =>
                    fixture.nativeElement.querySelector('[data-testid="label"]').textContent
            };
        };

        it('should recover a persistent OnPush label after initial failure and late retry', () => {
            const { fixture, get, install, label } = setup();
            expect(label()).toBe('apps.best');
            fixture.detectChanges();
            expect(label()).toBe('apps.best');
            expect(get).toHaveBeenCalledTimes(1);

            install('v1', 'Recovered {0} {1}');
            expect(label()).toBe('Recovered Costa Rica Panama');
            expect(get).toHaveBeenCalledTimes(2);
        });

        it('should refresh an installed fallback when a newer bundle arrives', () => {
            const { install, label } = setup('v1');
            expect(label()).toBe('Old label');
            install('v2', 'Updated {0}');
            expect(label()).toBe('Updated Costa Rica');
        });

        it('should refresh a persistent OnPush label when the locale changes at the same version', () => {
            const { install, label, loadedBundleVersion, get } = setup('v1');
            expect(label()).toBe('Old label');
            install('v1', 'Hola {0}', 'es_ES');
            expect(loadedBundleVersion()).toBe('v1');
            expect(label()).toBe('Hola Costa Rica');
            expect(get).toHaveBeenCalledTimes(2);
            install('v1', 'Old label', 'default');
            expect(label()).toBe('Old label');
            expect(get).toHaveBeenCalledTimes(3);
        });

        it('should cache unchanged inputs but detect changed argument values', () => {
            const { get, install } = setup();
            install('v1', 'Hello {0} {1}');
            const pipe = TestBed.runInInjectionContext(() => new DotMessagePipe());
            const args = ['Costa Rica', 'Panama'];
            pipe.transform('apps.best', args);
            get.mockClear();
            pipe.transform('apps.best', args);
            pipe.transform('apps.best', ['Costa Rica', 'Panama']);
            expect(get).not.toHaveBeenCalled();

            args[0] = 'Updated';
            expect(pipe.transform('apps.best', args)).toBe('Hello Updated Panama');
            expect(get).toHaveBeenCalledTimes(1);
            pipe.transform('another.key', args);
            expect(get).toHaveBeenCalledTimes(2);
        });
    });

    it('should return empty string if param is undefined', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: undefined }
        });
        expect(spectator.element.textContent).toBe('');
    });

    it('should return message requested', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: 'apps.search.placeholder' }
        });
        expect(spectator.element.textContent).toBe('Search');
    });

    it('should return message requested with replaced values', () => {
        spectator = createPipe(`{{ value | dm:args }}`, {
            hostProps: { value: 'apps.best', args: ['Costa Rica', 'Panama'] }
        });
        expect(spectator.element.textContent).toBe('Test Costa Rica Panama');
    });

    it('should return key if message not found', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: 'no.label.found' }
        });
        expect(spectator.element.textContent).toBe('no.label.found');
    });
});
