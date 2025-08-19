import { it, describe, expect, beforeEach, afterEach } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

import { MOCK_BASE_TYPES, mockContentTypes } from '../../shared/mocks';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveToolbarComponent', () => {
    let spectator: Spectator<DotContentDriveToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveToolbarComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                isTreeExpanded: jest.fn().mockReturnValue(true),
                setIsTreeExpanded: jest.fn(),
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                filters: jest.fn().mockReturnValue({})
            }),
            mockProvider(DotContentTypeService, {
                getContentTypes: jest.fn().mockReturnValue(of(mockContentTypes)),
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES))
            }),
            provideHttpClient()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render toolbar container', () => {
        spectator.detectChanges();
        const toolbar = spectator.query('.dot-content-drive-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should render the tree toggler', () => {
        spectator.detectChanges();
        const toggler = spectator.query('[data-testid="tree-toggler"]');
        expect(toggler).toBeTruthy();
    });

    it('should render the Add New button', () => {
        spectator.detectChanges();
        const button = spectator.query('[data-testid="add-new-button"]');
        expect(button).toBeTruthy();
    });

    it('should render start and end groups', () => {
        spectator.detectChanges();
        expect(spectator.query('.p-toolbar-group-start')).toBeTruthy();
        expect(spectator.query('.p-toolbar-group-end')).toBeTruthy();
    });

    it('should render the content type field', () => {
        spectator.detectChanges();
        const field = spectator.query('[data-testid="content-type-field"]');
        expect(field).toBeTruthy();
    });

    it('should render the search input', () => {
        spectator.detectChanges();
        const input = spectator.query('[data-testid="search-input"]');
        expect(input).toBeTruthy();
    });

    it('should render the base type selector', () => {
        spectator.detectChanges();
        const selector = spectator.query('[data-testid="base-type-selector"]');
        expect(selector).toBeTruthy();
    });
});
