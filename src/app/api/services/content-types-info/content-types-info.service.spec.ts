import { ContentTypesInfoService } from './content-types-info.service';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('ContentTypesInfoService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([ContentTypesInfoService]);

        this.iconsService = this.injector.get(ContentTypesInfoService);
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
});
