import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { Route, UrlSegment } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { dotAiConfigDetailMatchGuard } from './dot-ai-config-detail-match.guard';

// Third `CanMatchFn` param (currentSnapshot) is unused by this guard — an empty stand-in is enough.
type CurrentSnapshot = Parameters<typeof dotAiConfigDetailMatchGuard>[2];

describe('dotAiConfigDetailMatchGuard', () => {
    let mockPropertiesService: DotPropertiesService;

    const mockRoute = {} as Route;
    const mockSegments: UrlSegment[] = [];
    const mockSnapshot = {} as CurrentSnapshot;

    beforeEach(() => {
        mockPropertiesService = {
            getFreshFeatureFlag: jest.fn()
        } as unknown as DotPropertiesService;

        TestBed.configureTestingModule({
            providers: [{ provide: DotPropertiesService, useValue: mockPropertiesService }]
        });
    });

    it('should match and render the dotAI page when the feature flag is enabled', (done) => {
        (mockPropertiesService.getFreshFeatureFlag as jest.Mock).mockReturnValue(of(true));

        TestBed.runInInjectionContext(() => {
            const result = dotAiConfigDetailMatchGuard(mockRoute, mockSegments, mockSnapshot);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canMatch) => {
                    expect(canMatch).toBe(true);
                    done();
                });
            }
        });
    });

    it('should not match, so the router falls through to the legacy dotAI screen, when the feature flag is disabled', (done) => {
        (mockPropertiesService.getFreshFeatureFlag as jest.Mock).mockReturnValue(of(false));

        TestBed.runInInjectionContext(() => {
            const result = dotAiConfigDetailMatchGuard(mockRoute, mockSegments, mockSnapshot);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canMatch) => {
                    expect(canMatch).toBe(false);
                    done();
                });
            }
        });
    });

    it('should ask for FEATURE_FLAG_DOTAI_CONFIG_UI specifically', () => {
        (mockPropertiesService.getFreshFeatureFlag as jest.Mock).mockReturnValue(of(true));

        TestBed.runInInjectionContext(() => {
            dotAiConfigDetailMatchGuard(mockRoute, mockSegments, mockSnapshot);
        });

        expect(mockPropertiesService.getFreshFeatureFlag).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_DOTAI_CONFIG_UI
        );
    });
});
