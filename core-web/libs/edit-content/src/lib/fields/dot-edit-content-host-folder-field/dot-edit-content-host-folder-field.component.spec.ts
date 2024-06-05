import { Spectator, createComponentFactory, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { TreeNode } from 'primeng/api';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { HOST_FOLDER_FIELD_TEXT_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

const files: TreeNode[] = [
    {
        label: 'demo.dotcms.com',
        data: 'demo.dotcms.com',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'demo.dotcms.com/activities',
                data: 'activities',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'demo.dotcms.com/activities/themes',
                        data: 'themes',
                        icon: 'pi pi-folder-open'
                    }
                ]
            },
            {
                label: 'demo.dotcms.com/home',
                data: 'home',
                icon: 'pi pi-folder-open'
            }
        ]
    }
];

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
                getSitesTreePath: jest.fn().mockReturnValue(of(files))
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
