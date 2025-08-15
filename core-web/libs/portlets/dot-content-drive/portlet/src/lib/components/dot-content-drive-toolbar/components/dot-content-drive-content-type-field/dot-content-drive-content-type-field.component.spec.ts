import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveContentTypeFieldComponent } from './dot-content-drive-content-type-field.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveContentTypeFieldComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveContentTypeFieldComponent,
        providers: [
            {
                provide: DotContentDriveStore,
                useValue: {
                    filters: jest.fn().mockReturnValue({ baseType: [] }),
                    getFilterValue: jest.fn().mockReturnValue([]),
                    patchFilters: jest.fn(),
                    removeFilter: jest.fn()
                }
            },
            {
                provide: DotContentTypeService,
                useValue: {
                    getContentTypes: jest.fn().mockReturnValue(of([]))
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
