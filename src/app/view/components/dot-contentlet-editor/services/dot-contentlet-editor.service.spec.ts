import { of as observableOf } from 'rxjs';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotContentletEditorService } from './dot-contentlet-editor.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';

describe('DotContentletEditorService', () => {
    const load = () => {};
    const keyDown = () => {};
    let service: DotContentletEditorService;
    let dotMenuService: DotMenuService;
    let dotRouterService: DotRouterService;
    let injector;

    beforeEach(() => {
        injector = DOTTestBed.configureTestingModule({
            providers: [DotContentletEditorService, DotMenuService, DotRouterService],
            imports: [RouterTestingModule]
        });

        service = injector.get(DotContentletEditorService);
        dotMenuService = injector.get(DotMenuService);
        dotRouterService = injector.get(DotRouterService);
        spyOn(dotMenuService, 'getDotMenuId').and.returnValue(observableOf('456'));
    });

    it('should set data to add', () => {
        service.editUrl$.subscribe((url: string) => {
            expect(url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_p_id=content`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet`,
                    `&_content_cmd=edit&inode=999`
                ].join('')
            );
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for add');
        });

        service.add({
            header: 'This is a header for add',
            data: {
                baseTypes: 'content',
                container: '123'
            },
            events: {
                load: load,
                keyDown: keyDown
            }
        });
    });

    it('should set data to edit', () => {
        spyOnProperty(dotRouterService, 'currentPortlet').and.returnValue({
            url: '/c/c_Test/123',
            id: 'c_Test'
        });
        service.editUrl$.subscribe((url: string) => {
            expect(url).toEqual(
                [
                    '/c/portal/layout',
                    '?p_p_id=content',
                    '&p_p_action=1',
                    '&p_p_state=maximized',
                    '&p_p_mode=view',
                    '&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet',
                    '&_content_cmd=edit&inode=999'
                ].join('')
            );
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for edit');
        });

        service.edit({
            header: 'This is a header for edit',
            data: {
                inode: '999'
            }
        });
    });

    it('should set data to edit when current portlet is edit-page', () => {
        spyOnProperty(dotRouterService, 'currentPortlet').and.returnValue({
            url: '/#/edit-page/content?url=%2Fabout-us%2Findex&language_id=1',
            id: 'edit-page'
        });
        service.editUrl$.subscribe((url: string) => {
            expect(url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_p_id=content`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet`,
                    `&_content_cmd=edit&inode=999`
                ].join('')
            );
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for edit');
        });

        service.edit({
            header: 'This is a header for edit',
            data: {
                inode: '999'
            }
        });
    });

    it('should set data to edit when current portlet is site-browser', () => {
        spyOnProperty(dotRouterService, 'currentPortlet').and.returnValue({
            url: '/#/c/site-browser/ad5acc23-a466-4ac6-9c76-e6a3bc1d609e',
            id: 'site-browser'
        });
        service.editUrl$.subscribe((url: string) => {
            expect(url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_p_id=content`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet`,
                    `&_content_cmd=edit&inode=ad5acc23-a466-4ac6-9c76-e6a3bc1d609e`
                ].join('')
            );
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for edit');
        });

        service.edit({
            header: 'This is a header for edit',
            data: {
                inode: 'ad5acc23-a466-4ac6-9c76-e6a3bc1d609e'
            }
        });
    });

    it('should set url to create a contentlet', () => {
        service.createUrl$.subscribe((url: string) => {
            expect(url).toEqual('hello.world.com');
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for create');
        });

        service.create({
            header: 'This is a header for create',
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should clear url and undbind', () => {
        service.addUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.editUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.close$.subscribe((message) => {
            expect(message).toBe(true);
        });

        service.clear();

        expect(service.loadHandler).toEqual(null);
        expect(service.keyDownHandler).toEqual(null);
    });
});
