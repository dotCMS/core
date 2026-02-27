import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';

import { DotAjaxActionResponseView, DotBundle, DotCurrentUser } from '@dotcms/dotcms-models';

import { AddToBundleService } from './add-to-bundle.service';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';

const MOCK_USER_ID = '1234';

const mockCurrentUser: DotCurrentUser = {
    userId: MOCK_USER_ID,
    givenName: 'Test',
    surname: 'User',
    roleId: 'admin',
    email: 'test@test.com',
    admin: true
};

const mockBundleItems: DotBundle[] = [
    {
        name: 'My bundle',
        id: '1234'
    },
    {
        name: 'My bundle 2',
        id: '1sdf5-23fs-dsf2-sf3oj23p4p42d'
    }
];

const mockBundleResponse = {
    bodyJsonObject: {
        items: mockBundleItems
    }
};

const mockAddToBundleResponse: DotAjaxActionResponseView = {
    errorMessages: [],
    total: 1,
    bundleId: '1234-id-7890-entifier',
    errors: 0,
    _body: {}
};

describe('AddToBundleService', () => {
    let spectator: SpectatorHttp<AddToBundleService>;
    let dotCurrentUserService: SpyObject<DotCurrentUserService>;

    const createHttp = createHttpFactory({
        service: AddToBundleService,
        providers: [mockProvider(DotCurrentUserService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotCurrentUserService = spectator.inject(DotCurrentUserService);
    });

    describe('getBundles', () => {
        beforeEach(() => {
            const { of } = jest.requireActual('rxjs');
            dotCurrentUserService.getCurrentUser.mockReturnValue(of(mockCurrentUser));
        });

        it('should get bundle list for current user', () => {
            spectator.service.getBundles().subscribe((items) => {
                expect(items).toEqual(mockBundleItems);
            });

            const req = spectator.expectOne(
                `/api/bundle/getunsendbundles/userid/${MOCK_USER_ID}`,
                HttpMethod.GET
            );

            req.flush(mockBundleResponse);
        });
    });

    describe('addToBundle', () => {
        it('should do a post request and add to bundle', () => {
            const mockBundleData: DotBundle = {
                id: '1234',
                name: 'my bundle'
            };
            const assetIdentifier = '1234567890';

            spectator.service.addToBundle(assetIdentifier, mockBundleData).subscribe((response) => {
                expect(response).toEqual(mockAddToBundleResponse);
            });

            const req = spectator.expectOne(
                '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle',
                HttpMethod.POST
            );

            expect(req.request.body).toEqual(
                `assetIdentifier=${assetIdentifier}&bundleName=${mockBundleData.name}&bundleSelect=${mockBundleData.id}`
            );
            expect(req.request.headers.get('Content-Type')).toBe(
                'application/x-www-form-urlencoded'
            );

            req.flush(mockAddToBundleResponse);
        });
    });
});
