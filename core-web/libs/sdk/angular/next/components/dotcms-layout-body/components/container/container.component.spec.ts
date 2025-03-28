import { expect } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { ContainerComponent } from './container.component';

import { DotCMSSiteParentPermissionable } from '../../../../models';
import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';

describe('ContainerComponent', () => {
    let spectator: Spectator<ContainerComponent>;

    const createComponent = createComponentFactory({
        component: ContainerComponent,
        providers: [
            {
                provide: DotCMSContextService,
                useValue: {
                    isDevMode: jest.fn().mockReturnValue(false)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                container: {
                    identifier: 'test-container',
                    uuid: 'test-uuid',
                    iDate: 1234567890,
                    type: 'test-type',
                    inode: 'test-inode',
                    source: 'test-source',
                    title: 'Test Container',
                    friendlyName: 'Test Container',
                    modDate: 1234567890,
                    modUser: 'test-user',
                    sortOrder: 1,
                    showOnMenu: true,
                    maxContentlets: 10,
                    useDiv: true,
                    preLoop: '',
                    postLoop: '',
                    staticify: false,
                    notes: '',
                    live: true,
                    locked: false,
                    working: true,
                    deleted: false,
                    name: 'Test Container',
                    archived: false,
                    permissionId: 'test-permission',
                    versionId: 'test-version',
                    versionType: 'test-version-type',
                    permissionType: 'test-permission-type',
                    categoryId: 'test-category',
                    idate: 1234567890,
                    new: false,
                    acceptTypes: 'test-accept-types',
                    contentlets: [],
                    parentPermissionable: {
                        Inode: 'test-inode',
                        Identifier: 'test-identifier',
                        permissionByIdentifier: true,
                        type: 'test-type',
                        identifier: 'test-identifier',
                        permissionId: 'test-permission',
                        permissionType: 'test-permission-type',
                        inode: 'test-inode'
                    } as DotCMSSiteParentPermissionable
                }
            }
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render empty container message when no contentlets', () => {
        const emptyMessage = spectator.query('.empty-container');
        expect(emptyMessage?.textContent).toBe('This container is empty.');
    });

    it('should render contentlets when available', () => {
        const mockContentlets = [{ title: 'Test Contentlet 1' }, { title: 'Test Contentlet 2' }];

        spectator.setInput({
            container: {
                ...spectator.component.container,
                contentlets: mockContentlets
            }
        });

        const contentlets = spectator.queryAll('.contentlet-wrapper');
        expect(contentlets.length).toBe(2);
        expect(contentlets[0].textContent).toBe('Test Contentlet 1');
        expect(contentlets[1].textContent).toBe('Test Contentlet 2');
    });

    it('should set data attributes in edit mode', () => {
        const dotCMSContextService = spectator.inject(DotCMSContextService);
        jest.spyOn(dotCMSContextService, 'isDevMode').mockReturnValue(true);

        spectator.detectChanges();

        const container = spectator.query('.container');
        expect(container?.getAttribute('data-dot-accept-types')).toBe('test-accept-types');
        expect(container?.getAttribute('data-dot-identifier')).toBe('test-container');
        expect(container?.getAttribute('data-max-contentlets')).toBe('10');
        expect(container?.getAttribute('data-dot-uuid')).toBe('test-uuid');
    });

    it('should not set data attributes in production mode', () => {
        const container = spectator.query('.container');
        expect(container?.getAttribute('data-dot-accept-types')).toBeNull();
        expect(container?.getAttribute('data-dot-identifier')).toBeNull();
        expect(container?.getAttribute('data-max-contentlets')).toBeNull();
        expect(container?.getAttribute('data-dot-uuid')).toBeNull();
    });
});
