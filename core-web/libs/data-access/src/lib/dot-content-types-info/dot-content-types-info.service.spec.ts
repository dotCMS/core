import { DotContentTypesInfoService } from './dot-content-types-info.service';

describe('DotContentTypesInfoService', () => {
    let service: DotContentTypesInfoService;
    beforeEach(() => {
        service = new DotContentTypesInfoService();
    });

    it('should return a event_note for content', () => {
        expect(service.getIcon('content')).toBe('event_note');
    });

    it('should return a description for page', () => {
        expect(service.getIcon('htmlpage')).toBe('description');
    });

    it('should return a ImmutableWidgetContentType for page', () => {
        expect(service.getClazz('widget')).toBe(
            'com.dotcms.contenttype.model.type.ImmutableWidgetContentType'
        );
    });

    it('should return a ImmutablePersonaContentType for page', () => {
        expect(service.getClazz('persona')).toBe(
            'com.dotcms.contenttype.model.type.ImmutablePersonaContentType'
        );
    });

    it('should return a ImmutableDotAssetContentType for DotAsset', () => {
        expect(service.getClazz('DotAsset')).toBe(
            'com.dotcms.contenttype.model.type.ImmutableDotAssetContentType'
        );
    });
});
