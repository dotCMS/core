import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { DotPageStore } from './dot-pages.store';
import { DotCurrentUserService } from '@dotcms/app/api/services/dot-current-user/dot-current-user.service';
import { DotCurrentUser } from '@dotcms/app/shared/models/dot-current-user/dot-current-user';
import { CurrentUserDataMock } from '../../dot-starter/dot-starter-resolver.service.spec';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { pagesInitialTestData } from '../dot-pages.component.spec';
import { MockDotHttpErrorManagerService } from '../../dot-edit-page/components/dot-favorite-page/store/dot-favorite-page.store.spec';

@Injectable()
class MockDotCurrentUserService {
    public getCurrentUser(): Observable<DotCurrentUser> {
        return of(CurrentUserDataMock);
    }
}

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 15;
    totalRecords = 20;

    public get(): Observable<ESContent> {
        return of({
            contentTook: 1,
            jsonObjectView: { contentlets: pagesInitialTestData },
            queryTook: 1,
            resultsSize: pagesInitialTestData.length
        });
    }
}

describe('DotPageStore', () => {
    let dotPageStore: DotPageStore;
    let dotESContentService: DotESContentService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotPageStore,
                { provide: DotHttpErrorManagerService, useClass: MockDotHttpErrorManagerService },
                { provide: DotCurrentUserService, useClass: MockDotCurrentUserService },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        });
        dotPageStore = TestBed.inject(DotPageStore);
        dotESContentService = TestBed.inject(DotESContentService);

        dotPageStore.setInitialStateData();
    });

    it('should load Favorite Pages initial data', () => {
        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(pagesInitialTestData);
            expect(data.favoritePages.total).toEqual(pagesInitialTestData.length);
            expect(data.loggedUserId).toEqual(CurrentUserDataMock.userId);
        });
    });

    it('should limit Favorite Pages', () => {
        spyOn(dotPageStore, 'setFavoritePages').and.callThrough();
        dotPageStore.limitFavoritePages();
        expect(dotPageStore.setFavoritePages).toHaveBeenCalledWith(
            pagesInitialTestData.slice(0, 5)
        );
    });

    // Updaters
    it('should update Favorite Pages', () => {
        dotPageStore.setFavoritePages(pagesInitialTestData);
        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(pagesInitialTestData);
        });
    });

    it('should set all Favorite Pages value in store', () => {
        spyOn(dotPageStore, 'setFavoritePages').and.callThrough();
        spyOn(dotESContentService, 'get').and.returnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: pagesInitialTestData as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 20
            })
        );
        dotPageStore.loadAllFavoritePages();

        expect(dotESContentService.get).toHaveBeenCalledTimes(1);

        const expectedInputArray = [...pagesInitialTestData, ...pagesInitialTestData];
        expect(dotPageStore.setFavoritePages).toHaveBeenCalledWith(expectedInputArray);
    });
});
