import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';

import { BinaryImageEditSaveStrategy } from './binary-image-edit-save.strategy';
import { DotAssetImageEditSaveStrategy } from './dotasset-image-edit-save.strategy';
import { ImageEditSaveStrategyResolver } from './image-edit-save-strategy.resolver';

import { INPUT_TYPES } from '../../../../models/dot-edit-content-file.model';
import { FileFieldStore } from '../../store/file-field.store';

describe('ImageEditSaveStrategyResolver', () => {
    let spectator: SpectatorService<ImageEditSaveStrategyResolver>;

    const createService = createServiceFactory({
        service: ImageEditSaveStrategyResolver,
        providers: [
            BinaryImageEditSaveStrategy,
            DotAssetImageEditSaveStrategy,
            mockProvider(FileFieldStore, { applyTempFile: jest.fn() }),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('resolves the Binary strategy for Binary fields', () => {
        expect(spectator.service.resolve(INPUT_TYPES.Binary)).toBeInstanceOf(
            BinaryImageEditSaveStrategy
        );
    });

    it('resolves the dotAsset strategy for Image fields', () => {
        expect(spectator.service.resolve(INPUT_TYPES.Image)).toBeInstanceOf(
            DotAssetImageEditSaveStrategy
        );
    });

    it('resolves the dotAsset strategy for File fields', () => {
        expect(spectator.service.resolve(INPUT_TYPES.File)).toBeInstanceOf(
            DotAssetImageEditSaveStrategy
        );
    });

    it('falls back to the dotAsset strategy when the input type is null', () => {
        expect(spectator.service.resolve(null)).toBeInstanceOf(DotAssetImageEditSaveStrategy);
    });
});
