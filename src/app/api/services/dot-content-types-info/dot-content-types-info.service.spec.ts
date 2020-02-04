import { DotContentTypesInfoService } from './dot-content-types-info.service';
import { DOTTestBed } from '@tests/dot-test-bed';

describe('DotContentTypesInfoService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotContentTypesInfoService]);

        this.iconsService = this.injector.get(DotContentTypesInfoService);
    });

    it('should return a event_note for content', () => {
        expect(this.iconsService.getIcon('content')).toBe('event_note');
    });

    it('should return a description for page', () => {
        expect(this.iconsService.getIcon('htmlpage')).toBe('description');
    });

    it('should return a ImmutableWidgetContentType for page', () => {
        expect(this.iconsService.getClazz('widget')).toBe(
            'com.dotcms.contenttype.model.type.ImmutableWidgetContentType'
        );
    });

    it('should return a ImmutablePersonaContentType for page', () => {
        expect(this.iconsService.getClazz('persona')).toBe(
            'com.dotcms.contenttype.model.type.ImmutablePersonaContentType'
        );
    });

    it('should return a ImmutableDotAssetContentType for DotAsset', () => {
        expect(this.iconsService.getClazz('DotAsset')).toBe(
            'com.dotcms.contenttype.model.type.ImmutableDotAssetContentType'
        );
    });
});
