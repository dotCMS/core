import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotAssetImageEditSaveStrategy } from './dotasset-image-edit-save.strategy';

import { FileFieldStore } from '../../store/file-field.store';

const DOTASSET = { identifier: 'ref-id', inode: 'ref-inode', languageId: 1 } as DotCMSContentlet;
const TEMP_FILE = { id: 'temp_1', fileName: 'edited.png' } as DotCMSTempFile;

describe('DotAssetImageEditSaveStrategy', () => {
    let spectator: SpectatorService<DotAssetImageEditSaveStrategy>;
    let fire: DotWorkflowActionsFireService;
    let store: FileFieldStore;

    const createService = createServiceFactory({
        service: DotAssetImageEditSaveStrategy,
        providers: [
            mockProvider(FileFieldStore, {
                uploadedFile: jest.fn().mockReturnValue({ source: 'contentlet', file: DOTASSET }),
                getAssetData: jest.fn(),
                setUIMessage: jest.fn()
            }),
            mockProvider(DotWorkflowActionsFireService, {
                publishContentletByIdentifier: jest.fn().mockReturnValue(of(DOTASSET))
            })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        fire = spectator.inject(DotWorkflowActionsFireService);
        store = spectator.inject(FileFieldStore);
        // mockProvider jest.fns are shared across tests; re-establish defaults each run.
        (store.uploadedFile as jest.Mock).mockReturnValue({ source: 'contentlet', file: DOTASSET });
        (fire.publishContentletByIdentifier as jest.Mock).mockReturnValue(of(DOTASSET));
    });

    it('publishes a new version of the referenced dotAsset in its own language', () => {
        spectator.service.apply(TEMP_FILE);

        expect(fire.publishContentletByIdentifier).toHaveBeenCalledWith(
            { identifier: 'ref-id', asset: 'temp_1' },
            1
        );
    });

    it('refreshes the preview from the new version without changing the field value', () => {
        spectator.service.apply(TEMP_FILE);

        expect(store.getAssetData).toHaveBeenCalledWith('ref-id');
    });

    it('surfaces a server error when the publish fails', () => {
        (fire.publishContentletByIdentifier as jest.Mock).mockReturnValue(
            throwError(() => new Error('boom'))
        );

        spectator.service.apply(TEMP_FILE);

        expect(store.setUIMessage).toHaveBeenCalled();
        expect(store.getAssetData).not.toHaveBeenCalled();
    });

    it('does nothing when there is no referenced asset in preview', () => {
        (store.uploadedFile as jest.Mock).mockReturnValue({
            source: 'temp',
            file: TEMP_FILE
        });

        spectator.service.apply(TEMP_FILE);

        expect(fire.publishContentletByIdentifier).not.toHaveBeenCalled();
    });
});
