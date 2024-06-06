import { Spectator, createComponentFactory, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    HOST_FOLDER_FIELD_TEXT_MOCK,
    TREE_SELECT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: Spectator<DotEditContentHostFolderFieldComponent>;
    let service: SpyObject<DotEditContentService>;

    const createComponent = createComponentFactory({
        component: DotEditContentHostFolderFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [
            FormGroupDirective,
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_MOCK))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: {
                    ...HOST_FOLDER_FIELD_TEXT_MOCK
                }
            }
        });
        service = spectator.inject(DotEditContentService);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
        expect(service.getSitesTreePath).toHaveBeenCalled();
    });
});
