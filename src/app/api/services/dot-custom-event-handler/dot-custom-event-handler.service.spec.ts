
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotMenuService } from '@services/dot-menu.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotPushPublishDialogService } from '@services/dot-push-publish-dialog/dot-push-publish-dialog.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

describe('DotCustomEventHandlerService', () => {
    let service: DotCustomEventHandlerService;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotRouterService: DotRouterService;
    let dotUiColorsService: DotUiColorsService;
    let dotContentletEditorService: DotContentletEditorService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let injector;

    beforeEach(() => {
        injector = DOTTestBed.configureTestingModule({
            providers: [
                DotCustomEventHandlerService,
                DotLoadingIndicatorService,
                DotMenuService,
                DotPushPublishDialogService
            ],
            imports: [RouterTestingModule]
        });

        service = injector.get(DotCustomEventHandlerService);
        dotLoadingIndicatorService = injector.get(DotLoadingIndicatorService);
        dotRouterService = injector.get(DotRouterService);
        dotUiColorsService = injector.get(DotUiColorsService);
        dotContentletEditorService = injector.get(DotContentletEditorService);
        dotPushPublishDialogService = injector.get(DotPushPublishDialogService);
    });

    it('should show loading indicator and go to edit page when event is emited by iframe', () => {
        spyOn(dotLoadingIndicatorService, 'show');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-page',
                    data: {
                        url: 'some/url',
                        languageId: '2',
                        hostId: '123'
                    }
                }
            })
        );

        expect(dotLoadingIndicatorService.show).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            url: 'some/url',
            language_id: '2',
            host_id: '123'
        });
    });

    it('should create a contentlet', () => {
        spyOn(dotContentletEditorService, 'create');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'create-contentlet',
                    data: {
                        url: 'hello.world.com'
                    }
                }
            })
        );

        expect(dotContentletEditorService.create).toHaveBeenCalledWith({
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should edit a contentlet', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-contentlet',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledWith('123');
    });

    it('should edit a a workflow task', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-task',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditTask).toHaveBeenCalledWith('123');
    });

    it('should set colors in the ui', () => {
        spyOn(dotUiColorsService, 'setColors');
        const fakeHtmlEl = { hello: 'html' };
        spyOn(document, 'querySelector').and.returnValue(fakeHtmlEl);

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'company-info-updated',
                    payload: {
                        colors: {
                            primary: '#fff',
                            secondary: '#000',
                            background: '#ccc'
                        }
                    }
                }
            })
        );
        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl, {
            primary: '#fff',
            secondary: '#000',
            background: '#ccc'
        });
    });

    it('should notify to open push publish dialog', () => {
        const dataMock = {
            assetIdentifier: '123',
            dateFilter: true,
            removeOnly: true,
            isBundle: false
        };

        spyOn(dotPushPublishDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'push-publish',
                    data: dataMock
                }
            })
        );

        expect(dotPushPublishDialogService.open).toHaveBeenCalledWith(dataMock);
    });
});
