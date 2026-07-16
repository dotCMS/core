import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import {
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotDownloadBundleFormComponent,
    DotDownloadBundleFormValue
} from './dot-download-bundle-form.component';

const FILTERS: DotPushPublishFilter[] = [
    { key: 'ForcePush.yml', title: 'Force Push', defaultFilter: false },
    { key: 'Default.yml', title: 'Default filter', defaultFilter: true },
    { key: 'OnlySelected.yml', title: 'Only Selected', defaultFilter: false }
];

describe('DotDownloadBundleFormComponent', () => {
    let spectator: Spectator<DotDownloadBundleFormComponent>;
    let filtersService: jest.Mocked<DotPushPublishFiltersService>;

    const createComponent = createComponentFactory({
        component: DotDownloadBundleFormComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DotPushPublishFiltersService, {
                get: jest.fn().mockReturnValue(of(FILTERS))
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { bundleId: 'bundle-1' } });
        filtersService = spectator.inject(
            DotPushPublishFiltersService
        ) as jest.Mocked<DotPushPublishFiltersService>;
        spectator.detectChanges();
    });

    it('loads filters on init and sorts them alphabetically', () => {
        expect(filtersService.get).toHaveBeenCalled();
        expect(spectator.component.filterOptions.map((f) => f.label)).toEqual([
            'Default filter',
            'Force Push',
            'Only Selected'
        ]);
    });

    it('initializes with Publish selected and the default filter', () => {
        expect(spectator.component.form.get('downloadOptionSelected')?.value).toBe('publish');
        expect(spectator.component.form.get('filterKey')?.value).toBe('Default.yml');
    });

    it('emits BE-shaped value (operation "0" for publish) on init', () => {
        let lastValue: DotDownloadBundleFormValue | null = null;
        spectator.output('value').subscribe((v) => (lastValue = v as DotDownloadBundleFormValue));

        spectator.component.form.patchValue({ filterKey: 'ForcePush.yml' });

        expect(lastValue).toEqual({
            bundleId: 'bundle-1',
            operation: '0',
            filterKey: 'ForcePush.yml'
        });
    });

    it('disables and clears the filter dropdown when Unpublish is selected', () => {
        spectator.component.form.patchValue({ downloadOptionSelected: 'unpublish' });
        expect(spectator.component.form.get('filterKey')?.disabled).toBe(true);
        expect(spectator.component.form.get('filterKey')?.value).toBe('');
    });

    it('emits operation "1" with empty filterKey when Unpublish is selected', () => {
        let lastValue: DotDownloadBundleFormValue | null = null;
        spectator.output('value').subscribe((v) => (lastValue = v as DotDownloadBundleFormValue));

        spectator.component.form.patchValue({ downloadOptionSelected: 'unpublish' });

        expect(lastValue).toMatchObject({ operation: '1', filterKey: '' });
    });

    it('re-enables the filter dropdown when switching back to Publish', () => {
        spectator.component.form.patchValue({ downloadOptionSelected: 'unpublish' });
        spectator.component.form.patchValue({ downloadOptionSelected: 'publish' });
        expect(spectator.component.form.get('filterKey')?.disabled).toBe(false);
        expect(spectator.component.form.get('filterKey')?.value).toBe('Default.yml');
    });

    it('emits validity changes', () => {
        let lastValid: boolean | null = null;
        spectator.output('valid').subscribe((v) => (lastValid = v as boolean));

        // Force the form invalid by clearing the required control.
        spectator.component.form.get('downloadOptionSelected')?.setValue('');
        expect(lastValid).toBe(false);

        spectator.component.form.get('downloadOptionSelected')?.setValue('publish');
        expect(lastValid).toBe(true);
    });
});
