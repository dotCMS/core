/* eslint-disable @typescript-eslint/no-explicit-any */

// const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
//     'toString'
// ]);

// route.queryParams = {};

xdescribe('DotExperimentExperimentResolver', () => {
    it('should return a DotRenderedPageState', () => {
        pending('convert to jest');
    });
    //
    //
    // let dotHttpErrorManagerService: DotHttpErrorManagerService;
    // let dotPageStateService: DotPageStateService;
    // let dotPageStateServiceRequestPageSpy: jasmine.Spy;
    // let dotRouterService: DotRouterService;
    //
    // let injector: TestBed;
    // let dotEditPageResolver: DotEditPageResolver;
    //
    // beforeEach(() => {
    //     TestBed.configureTestingModule({
    //         imports: [HttpClientTestingModule],
    //         providers: [
    //             { provide: CoreWebService, useClass: CoreWebServiceMock },
    //             DotHttpErrorManagerService,
    //             DotPageStateService,
    //             DotEditPageResolver,
    //             DotPageRenderService,
    //             DotContentletLockerService,
    //             DotAlertConfirmService,
    //             ConfirmationService,
    //             DotFormatDateService,
    //             DotESContentService,
    //             { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
    //             { provide: DotRouterService, useClass: MockDotRouterService },
    //             {
    //                 provide: ActivatedRouteSnapshot,
    //                 useValue: route
    //             },
    //             {
    //                 provide: LoginService,
    //                 useClass: LoginServiceMock
    //             }
    //         ]
    //     });
    //     injector = getTestBed();
    //     dotEditPageResolver = injector.get(DotEditPageResolver);
    //     dotHttpErrorManagerService = injector.get(DotHttpErrorManagerService);
    //     dotPageStateService = injector.get(DotPageStateService);
    //     dotPageStateServiceRequestPageSpy = jest.spyOn(dotPageStateService, 'requestPage');
    //     dotRouterService = injector.get(DotRouterService);
    //
    //     jest.spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
    // });
    //
    // beforeEach(() => {
    //     route.queryParams.url = 'edit-page/content';
    //     route.queryParams.language_id = '2';
    //     route.children = [
    //         {
    //             url: [
    //                 {
    //                     path: 'content'
    //                 }
    //             ]
    //         }
    //     ];
    // });
    //
    // it('should return a DotRenderedPageState', () => {
    //     const mock = new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));
    //     dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
    //
    //     dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState) => {
    //         expect(state).toEqual(mock);
    //     });
    //
    //     expect<any>(dotPageStateService.requestPage).toHaveBeenCalledWith({
    //         url: 'edit-page/content',
    //         viewAs: {
    //             language: '2'
    //         }
    //     });
    // });
    //
    // it('should redirect to site-browser when request fail', () => {
    //     const fake403Response = mockResponseView(403);
    //
    //     dotPageStateServiceRequestPageSpy.mockReturnValue(throwError(fake403Response));
    //
    //     dotEditPageResolver.resolve(route).subscribe();
    //     expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
    // });
    //
    // it('should return DotPageRenderState from local state', () => {
    //     const mock = new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));
    //     dotPageStateService.setInternalNavigationState(mock);
    //
    //     dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState) => {
    //         expect(state).toEqual(mock);
    //     });
    //
    //     expect(dotPageStateServiceRequestPageSpy).not.toHaveBeenCalled();
    // });
    //
    // xdescribe('handle layout', () => {
    //     beforeEach(() => {
    //         route.children = [
    //             {
    //                 url: [
    //                     {
    //                         path: 'layout'
    //                     }
    //                 ]
    //             }
    //         ];
    //     });
    //
    //     it('should return a DotRenderedPageState', () => {
    //         const mock = new DotPageRenderState(
    //             mockUser(),
    //             new DotPageRender(mockDotRenderedPage())
    //         );
    //         dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
    //
    //         dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState) => {
    //             expect(state).toEqual(mock);
    //         });
    //         expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
    //     });
    //
    //     it('should handle error and redirect to site-browser when cant edit layout', () => {
    //         const mock = new DotPageRenderState(
    //             mockUser(),
    //             new DotPageRender({
    //                 ...mockDotRenderedPage(),
    //                 page: {
    //                     ...mockDotRenderedPage().page,
    //                     canEdit: false
    //                 }
    //             })
    //         );
    //         dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
    //
    //         dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState) => {
    //             expect(state).toBeNull();
    //         });
    //         expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
    //         expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
    //             new HttpErrorResponse(
    //                 new HttpResponse({
    //                     body: null,
    //                     status: HttpCode.FORBIDDEN,
    //                     headers: null,
    //                     url: ''
    //                 })
    //             )
    //         );
    //     });
    //
    //     it('should handle error and redirect to site-browser when layout is not drawed', () => {
    //         const mock = new DotPageRenderState(
    //             mockUser(),
    //             new DotPageRender({
    //                 ...mockDotRenderedPage(),
    //                 layout: null
    //             })
    //         );
    //         dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
    //
    //         dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState) => {
    //             expect(state).toBeNull();
    //         });
    //         expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
    //         expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
    //     });
    // });
});
