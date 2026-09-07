import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotContentTypeService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerToolbarComponent } from './dot-asset-picker-toolbar.component';

import { DotKeyboardShortcutService } from '../../../../services/dot-keyboard-shortcut/dot-keyboard-shortcut.service';
import { DotContentTypeFilterComponent } from '../../../dot-content-type-filter/dot-content-type-filter.component';
import { buildAssetPickerConfig } from '../../asset-picker-config';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';
import { DotAssetPickerConfig } from '../../store/models';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

/** Only the slice of the store the toolbar reads. Signals, not `jest.fn()`s, so `computed` reacts. */
const createMockStore = (config: DotAssetPickerConfig) => ({
    config: signal(config),
    filters: signal({}),
    selectedNode: signal(undefined),
    setSearch: jest.fn(),
    patchFilters: jest.fn(),
    removeFilter: jest.fn()
});

describe('DotAssetPickerToolbarComponent', () => {
    let spectator: Spectator<DotAssetPickerToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerToolbarComponent,
        providers: [
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of([])),
                getContentTypesWithPagination: jest
                    .fn()
                    .mockReturnValue(of({ contentTypes: [], pagination: {} }))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of([]))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ],
        detectChanges: false
    });

    const setup = (config: DotAssetPickerConfig) => {
        TestBed.overrideComponent(DotAssetPickerToolbarComponent, {
            add: {
                providers: [{ provide: DotAssetPickerStore, useValue: createMockStore(config) }]
            }
        });

        spectator = createComponent();
        spectator.detectChanges();
    };

    /** What the selector is allowed to OFFER, as the child filter sees it. */
    const offeredBaseTypes = () =>
        spectator.query(DotContentTypeFilterComponent)?.$allowedBaseTypes();

    describe('content-type selector restriction', () => {
        // AC (#36836): the selector offers dotAsset + File Asset in BOTH modes. What differs is
        // only the pre-selection.
        it('should offer only the asset-bearing base types for an Image field', () => {
            setup(buildAssetPickerConfig({ mode: 'image', site: SITE }));

            expect(offeredBaseTypes()).toEqual([
                DotCMSBaseTypesContentTypes.DOTASSET,
                DotCMSBaseTypesContentTypes.FILEASSET
            ]);
        });

        it('should offer only the asset-bearing base types for a File field', () => {
            // Regression: this used to be derived from `config.baseTypes` — empty for File — so the
            // selector fell back to "no restriction" and listed Widget and Content.
            setup(buildAssetPickerConfig({ mode: 'file', site: SITE }));

            expect(offeredBaseTypes()).toEqual([
                DotCMSBaseTypesContentTypes.DOTASSET,
                DotCMSBaseTypesContentTypes.FILEASSET
            ]);
        });

        it('should not pre-select anything for a File field while still restricting the options', () => {
            setup(buildAssetPickerConfig({ mode: 'file', site: SITE }));

            expect(spectator.query(DotContentTypeFilterComponent)?.$baseTypes()).toEqual([]);
            expect(offeredBaseTypes()).toHaveLength(2);
        });

        it('should leave the selector unrestricted when the config allows everything', () => {
            setup({ site: SITE });

            expect(offeredBaseTypes()).toBeNull();
        });
    });

    // This is the case the shortcut registry exists for. The picker opens over a portlet that has
    // already claimed the same combination, so both search boxes are alive at once and something has
    // to arbitrate. Claims are per-combination and last-in-wins, so the dialog wins while it is up.
    describe('search shortcut', () => {
        const pressModK = (): KeyboardEvent => {
            const event = new KeyboardEvent('keydown', {
                key: 'k',
                metaKey: true,
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);

            return event;
        };

        const searchField = () => spectator.query('[data-testid="asset-picker-search-input"]');

        it('should focus its own search field', () => {
            setup({ site: SITE });

            pressModK();

            expect(document.activeElement).toBe(searchField());
        });

        it('should suppress the browser default', () => {
            setup({ site: SITE });

            const event = pressModK();

            expect(event.defaultPrevented).toBe(true);
        });

        // The claim it shadowed has to come back, or the portlet behind would lose its shortcut for
        // the rest of the session after the dialog is used once.
        it('should hand the shortcut back to the surface underneath when it closes', () => {
            setup({ site: SITE });

            const portletSearch = jest.fn();
            const shortcuts = spectator.inject(DotKeyboardShortcutService);
            shortcuts.register({
                combination: 'mod+k',
                label: 'portlet search',
                handler: portletSearch
            });

            // The picker registered first, so re-registering above puts the "portlet" on top. Destroy
            // the picker and the standing claim must still be the one it was shadowing.
            spectator.fixture.destroy();
            pressModK();

            expect(portletSearch).toHaveBeenCalledTimes(1);
        });
    });
});
