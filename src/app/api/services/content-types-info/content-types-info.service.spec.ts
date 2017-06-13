import { ContentTypesInfoService } from './content-types-info.service';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('ContentTypesInfoService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            ContentTypesInfoService
        ]);

        this.iconsService = this.injector.get(ContentTypesInfoService);
    });

    it('should return a fa-newspaper-o for content', () => {
        expect(this.iconsService.getIcon('content')).toBe('fa-newspaper-o');
    });

    it('should return a fa-file-text-o for page', () => {
        expect(this.iconsService.getIcon('page')).toBe('fa-file-text-o');
    });

    it('should return a ImmutableWidgetContentType for page', () => {
        expect(this.iconsService.getClazz('widget')).toBe('com.dotcms.contenttype.model.type.ImmutableWidgetContentType');
    });

    it('should return a ImmutablePersonaContentType for page', () => {
        expect(this.iconsService.getClazz('persona')).toBe('com.dotcms.contenttype.model.type.ImmutablePersonaContentType');
    });
});