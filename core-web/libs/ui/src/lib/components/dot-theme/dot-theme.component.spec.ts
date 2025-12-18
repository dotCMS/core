import {
    createComponentFactory,
    createHostFactory,
    mockProvider,
    Spectator,
    SpectatorHost,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { Button } from 'primeng/button';
import { DataView } from 'primeng/dataview';

import { DotThemesService } from '@dotcms/data-access';
import { DotTheme, DotPagination } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotThemeComponent } from './dot-theme.component';

const mockThemes: DotTheme[] = [
    {
        identifier: 'theme1',
        inode: 'inode1',
        path: '/application/themes/theme1',
        title: 'Theme 1',
        themeThumbnail: null,
        name: 'Theme 1',
        hostId: 'site1'
    },
    {
        identifier: 'theme2',
        inode: 'inode2',
        path: '/application/themes/theme2',
        title: 'Theme 2',
        themeThumbnail: 'thumbnail2',
        name: 'Theme 2',
        hostId: 'site1'
    },
    {
        identifier: 'SYSTEM_THEME',
        inode: 'system',
        path: '/application/themes/system',
        title: 'System Theme',
        themeThumbnail: '/path/to/thumb.png',
        name: 'System Theme',
        hostId: 'site1'
    }
];

const mockPagination: DotPagination = {
    currentPage: 1,
    perPage: 6,
    totalEntries: 3
};

@Component({
    selector: 'dot-test-host',
    template: '',
    standalone: false
})
class FormHostComponent {
    themeControl = new FormControl<string | null>(null);
}

describe('DotThemeComponent', () => {
    let spectator: Spectator<DotThemeComponent>;
    let themesService: SpyObject<DotThemesService>;
    let globalStoreSignal: ReturnType<typeof signal<string | null>>;

    const createComponent = createComponentFactory({
        component: DotThemeComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotThemesService),
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: GlobalStore,
                useFactory: () => ({
                    currentSiteId: globalStoreSignal
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        globalStoreSignal = signal<string | null>(null);
        spectator = createComponent({ detectChanges: false });
        themesService = spectator.inject(DotThemesService, true);

        themesService.getThemes.mockReturnValue(
            of({
                themes: mockThemes,
                pagination: mockPagination
            })
        );
        themesService.get.mockImplementation((id: string) =>
            of(mockThemes.find((t) => t.identifier === id) || mockThemes[0])
        );
    });

    describe('Component Initialization', () => {
    it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with default state', () => {
            spectator.detectChanges();

            const buttonComponent = spectator.query(Button);
            expect(buttonComponent?.label).toBe('Select a theme');
            expect(buttonComponent?.disabled).toBe(false);

            const popover = spectator.query('p-popover');
            expect(popover).toBeTruthy();

            const dataViewComponent = spectator.query(DataView, { root: true });
            if (dataViewComponent) {
                expect(dataViewComponent.totalRecords).toBe(0);
            }
        });

        it('should initialize hostId from GlobalStore when available', fakeAsync(() => {
            globalStoreSignal.set('site1');
            spectator.detectChanges();
            tick();

            expect(spectator.component.$state.hostId()).toBe('site1');
            expect(themesService.getThemes).toHaveBeenCalledWith(
                expect.objectContaining({
                    hostId: 'site1',
                    page: 1,
                    per_page: 6
                })
            );
        }));
    });

    describe('ControlValueAccessor - writeValue', () => {
        let onChangeSpy: jest.Mock;
        let onTouchedSpy: jest.Mock;
        let onChangeOutputSpy: jest.Mock;

        beforeEach(fakeAsync(() => {
            onChangeSpy = jest.fn();
            onTouchedSpy = jest.fn();
            onChangeOutputSpy = jest.fn();

            spectator.component.registerOnChange(onChangeSpy);
            spectator.component.registerOnTouched(onTouchedSpy);
            spectator.component.onChange.subscribe(onChangeOutputSpy);

            // Setup themes in state
            globalStoreSignal.set('site1');
            spectator.detectChanges();
            tick();
        }));

        it('should update internal value when writeValue is called', () => {
            spectator.component.writeValue('theme1');
            spectator.detectChanges();

            expect(spectator.component.value()).toBe('theme1');
        });

        it('should NOT emit onChange callback when writeValue is called', () => {
            spectator.component.writeValue('theme1');
            spectator.detectChanges();

            expect(onChangeSpy).not.toHaveBeenCalled();
            expect(onTouchedSpy).not.toHaveBeenCalled();
            expect(onChangeOutputSpy).not.toHaveBeenCalled();
        });

        it('should update selectedTheme when writeValue is called with existing theme', fakeAsync(() => {
            spectator.component.writeValue('theme1');
            spectator.detectChanges();
            tick();

            expect(spectator.component.$state.selectedTheme()?.identifier).toBe('theme1');
        }));

        it('should fetch theme when writeValue is called with theme not in loaded list', fakeAsync(() => {
            const newTheme: DotTheme = {
                identifier: 'theme3',
                inode: 'inode3',
                path: '/application/themes/theme3',
                title: 'Theme 3',
                themeThumbnail: null,
                name: 'Theme 3',
                hostId: 'site1'
            };

            themesService.get.mockReturnValue(of(newTheme));

            spectator.component.writeValue('theme3');
            spectator.detectChanges();
            tick();

            expect(themesService.get).toHaveBeenCalledWith('theme3');
            expect(spectator.component.$state.selectedTheme()?.identifier).toBe('theme3');
            expect(onChangeSpy).not.toHaveBeenCalled();
        }));

        it('should clear selectedTheme when writeValue is called with null', () => {
            spectator.component.writeValue('theme1');
            spectator.detectChanges();
            expect(spectator.component.$state.selectedTheme()).toBeTruthy();

            spectator.component.writeValue(null);
            spectator.detectChanges();

            expect(spectator.component.value()).toBeNull();
            expect(spectator.component.$state.selectedTheme()).toBeNull();
            expect(onChangeSpy).not.toHaveBeenCalled();
        });
    });

    describe('User Selection - onThemeSelect', () => {
        let onChangeSpy: jest.Mock;
        let onTouchedSpy: jest.Mock;
        let onChangeOutputSpy: jest.Mock;

        beforeEach(fakeAsync(() => {
            onChangeSpy = jest.fn();
            onTouchedSpy = jest.fn();
            onChangeOutputSpy = jest.fn();

            spectator.component.registerOnChange(onChangeSpy);
            spectator.component.registerOnTouched(onTouchedSpy);
            spectator.component.onChange.subscribe(onChangeOutputSpy);

            // Setup themes in state
            globalStoreSignal.set('site1');
            spectator.detectChanges();
            tick();
        }));

        it('should emit onChange callback exactly once when user selects theme', () => {
            spectator.component.onThemeSelect('theme1');
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledTimes(1);
            expect(onChangeSpy).toHaveBeenCalledWith('theme1');
        });

        it('should emit onChange output exactly once when user selects theme', () => {
            spectator.component.onThemeSelect('theme1');
            spectator.detectChanges();

            expect(onChangeOutputSpy).toHaveBeenCalledTimes(1);
            expect(onChangeOutputSpy).toHaveBeenCalledWith('theme1');
        });

        it('should call onTouched callback when user selects theme', () => {
            spectator.component.onThemeSelect('theme1');
            spectator.detectChanges();

            expect(onTouchedSpy).toHaveBeenCalledTimes(1);
        });

        it('should update value and selectedTheme when user selects theme', () => {
            spectator.component.onThemeSelect('theme1');
            spectator.detectChanges();

            expect(spectator.component.value()).toBe('theme1');
            expect(spectator.component.$state.selectedTheme()?.identifier).toBe('theme1');
        });

        it('should handle null selection', () => {
            spectator.component.onThemeSelect('theme1');
            spectator.detectChanges();
            jest.clearAllMocks();

            spectator.component.onThemeSelect(null);
            spectator.detectChanges();

            expect(spectator.component.value()).toBeNull();
            expect(spectator.component.$state.selectedTheme()).toBeNull();
            expect(onChangeSpy).toHaveBeenCalledTimes(1);
            expect(onChangeSpy).toHaveBeenCalledWith(null);
            expect(onChangeOutputSpy).toHaveBeenCalledTimes(1);
            expect(onChangeOutputSpy).toHaveBeenCalledWith(null);
        });
    });

    describe('Host ID Local Override', () => {
        beforeEach(fakeAsync(() => {
            globalStoreSignal.set('site1');
            spectator.detectChanges();
            tick();
        }));

        it('should initialize hostId from GlobalStore when null', () => {
            expect(spectator.component.$state.hostId()).toBe('site1');
        });

        it('should NOT overwrite hostId when user changes site via onSiteChange', fakeAsync(() => {
            // User changes site
            spectator.component.onSiteChange('site2');
            spectator.detectChanges();
            tick();

            expect(spectator.component.$state.hostId()).toBe('site2');

            // GlobalStore changes - update the signal
            globalStoreSignal.set('site3');
            spectator.detectChanges();
            tick();

            // hostId should remain 'site2' (user selection)
            expect(spectator.component.$state.hostId()).toBe('site2');
        }));

        it('should allow GlobalStore to initialize when hostId is null', fakeAsync(() => {
            // Clear hostId and reset globalStoreSignal
            globalStoreSignal.set(null);
            spectator.component.onSiteChange(null);
            spectator.detectChanges();
            tick();

            expect(spectator.component.$state.hostId()).toBeNull();

            // GlobalStore should be able to initialize again - update the signal
            globalStoreSignal.set('site4');
            spectator.detectChanges();
            tick();

            expect(spectator.component.$state.hostId()).toBe('site4');
        }));
    });

    describe('Theme Thumbnail URL', () => {
        beforeEach(fakeAsync(() => {
            globalStoreSignal.set('site1');
            spectator.detectChanges();
            tick();
        }));

        it('should return empty string for theme without thumbnail', () => {
            const url = spectator.component.getThemeThumbnailUrl(mockThemes[0]);
            expect(url).toBe('');
        });

        it('should return thumbnail URL for regular theme', () => {
            const url = spectator.component.getThemeThumbnailUrl(mockThemes[1]);
            expect(url).toBe('/dA/thumbnail2/720/theme.png');
        });

        it('should return thumbnail as-is for SYSTEM_THEME', () => {
            const url = spectator.component.getThemeThumbnailUrl(mockThemes[2]);
            expect(url).toBe('/path/to/thumb.png');
        });
    });
});

describe('DotThemeComponent - ControlValueAccessor Integration', () => {
    let hostSpectator: SpectatorHost<DotThemeComponent, FormHostComponent>;
    let hostComponent: FormHostComponent;
    let hostThemesService: SpyObject<DotThemesService>;
    let hostGlobalStoreSignal: ReturnType<typeof signal<string | null>>;

    const createHost = createHostFactory({
        component: DotThemeComponent,
        host: FormHostComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotThemesService),
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: GlobalStore,
                useFactory: () => ({
                    currentSiteId: hostGlobalStoreSignal
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(fakeAsync(() => {
        // Initialize signal before creating host
        hostGlobalStoreSignal = signal<string | null>('site1');

        hostSpectator = createHost(`<dot-theme [formControl]="themeControl"></dot-theme>`);
        hostComponent = hostSpectator.hostComponent;
        hostThemesService = hostSpectator.inject(DotThemesService, true);

        hostThemesService.getThemes.mockReturnValue(
            of({
                themes: mockThemes,
                pagination: mockPagination
            })
        );
        hostThemesService.get.mockImplementation((id: string) =>
            of(mockThemes.find((t) => t.identifier === id) || mockThemes[0])
        );

        hostSpectator.detectChanges();
        tick();
        hostSpectator.detectChanges();
    }));

    it('should write value to component from FormControl without emitting', fakeAsync(() => {
        const onChangeSpy = jest.fn();
        hostSpectator.component.onChange.subscribe(onChangeSpy);

        hostComponent.themeControl.setValue('theme1');
        hostSpectator.detectChanges();
        tick();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toEqual('theme1');
        expect(onChangeSpy).not.toHaveBeenCalled();
    }));

    it('should update FormControl when user selects theme', fakeAsync(() => {
        hostSpectator.detectChanges();
        tick();

        hostSpectator.component.onThemeSelect('theme2');
        hostSpectator.detectChanges();
        tick();

        expect(hostComponent.themeControl.value).toBe('theme2');
    }));

    it('should handle disabled state from FormControl', () => {
        hostComponent.themeControl.disable();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.$disabled()).toBe(true);
    });
});
