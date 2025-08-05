import { expect, it } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { SEO_RULES_COLORS, SEO_RULES_ICONS } from '@dotcms/dotcms-models';

import { DotSeoMetaTagsUtilService } from './dot-seo-meta-tags-util.service';

import { DotMessageService } from '../dot-messages/dot-messages.service';

describe('DotSeoMetaTagsUtilService', () => {
    let spectator: SpectatorService<DotSeoMetaTagsUtilService>;
    const createService = createServiceFactory(DotSeoMetaTagsUtilService);

    beforeEach(() => {
        spectator = createService({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get: (key: string) => key
                    }
                }
            ]
        });
    });

    it('should add HTML tags to a message', () => {
        const message = 'og:title This is a test message with SEO_TAGS';
        const result = spectator.service.addHTMLTag(message);

        expect(result).toContain('<code>og:title</code>');
    });

    it('should return an error item', () => {
        const message = 'Error message';
        const result = spectator.service.getErrorItem(message);

        expect(result.message).toEqual(spectator.service.addHTMLTag(message));
        expect(result.color).toBe(SEO_RULES_COLORS.ERROR);
        expect(result.itemIcon).toBe(SEO_RULES_ICONS.TIMES);
    });

    it('should return a warning item', () => {
        const message = 'Warning message';
        const result = spectator.service.getWarningItem(message);

        expect(result.message).toEqual(spectator.service.addHTMLTag(message));
        expect(result.color).toBe(SEO_RULES_COLORS.WARNING);
        expect(result.itemIcon).toBe(SEO_RULES_ICONS.EXCLAMATION_CIRCLE);
    });

    it('should return a done item', () => {
        const message = 'Done message';
        const result = spectator.service.getDoneItem(message);

        expect(result.message).toEqual(spectator.service.addHTMLTag(message));
        expect(result.color).toBe(SEO_RULES_COLORS.DONE);
        expect(result.itemIcon).toBe(SEO_RULES_ICONS.CHECK);
    });

    it('should validate that all values are falsy or empty', () => {
        const falsyValues: (string | NodeListOf<Element>)[] = [null, undefined, ''];
        const result = spectator.service.areAllFalsyOrEmpty(falsyValues);

        expect(result).toBe(true);

        const nonEmptyValues: (string | NodeListOf<Element>)[] = [
            null,
            undefined,
            'test',
            document.querySelectorAll('div')
        ];
        const nonEmptyResult = spectator.service.areAllFalsyOrEmpty(nonEmptyValues);

        expect(nonEmptyResult).toBe(false);
    });
});
