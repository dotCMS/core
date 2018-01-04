import { fakeAsync, tick } from '@angular/core/testing';
import { DotEditContentHtmlService } from './dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './html/dot-edit-content-toolbar-html.service';
import { DotContainerContentletService } from './dot-container-contentlet.service';
import { DotDragDropAPIHtmlService } from './html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './html/dot-dom-html-util.service';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { LoggerService, StringUtils } from 'dotcms-js/dotcms-js';
import { Config } from 'dotcms-js/core/config.service';
import { Logger } from 'angular2-logger/core';
import { DOTTestBed } from '../../../test/dot-test-bed';

class MockDotEditContentHtmlService {
    bindContainersEvents(): void {

    }

    bindContenletsEvents(): void {

    }
}

describe('DotEditContentHtmlService', () => {
    const testDoc = document.implementation.createDocument('http://www.w3.org/1999/xhtml', 'html', null);
    const htmlElement = testDoc.getElementsByTagName('html')[0];
    const dummyContainer = testDoc.createElement('div');
    dummyContainer.innerHTML = `<div data-dot-object="container">
                                        <div data-dot-object="contentlet">
                                            <div class="large-column"></div>
                                        </div>
                                    </div>`;
    htmlElement.appendChild(dummyContainer);

    const messageServiceMock = new MockMessageService({
        'editpage.content.contentlet.menu.drag': 'Drag',
        'editpage.content.contentlet.menu.edit': 'Edit',
        'editpage.content.contentlet.menu.remove': 'Remove',
        'editpage.content.container.action.add': 'Add',
        'editpage.content.container.menu.content': 'Content',
        'editpage.content.container.menu.widget': 'Widget',
        'editpage.content.container.menu.form': 'Form'
    });

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            { provide: DotEditContentHtmlService, useClass: MockDotEditContentHtmlService },
            DotContainerContentletService,
            DotEditContentToolbarHtmlService,
            DotDragDropAPIHtmlService,
            DotDOMHtmlUtilService,
            LoggerService,
            Config,
            Logger,
            StringUtils,
            { provide: MessageService, useValue: messageServiceMock }
        ]);
        this.dotEditContentHtmlService = this.injector.get(DotEditContentHtmlService);
        this.dotEditContentToolbarHtmlService = this.injector.get(DotEditContentToolbarHtmlService);
    });

    it('should bind containers events when dotEditContentToolbarHtmlService resolved', fakeAsync((): void => {
        const spyEditContentHtmlService = spyOn(this.dotEditContentHtmlService, 'bindContainersEvents');

        this.dotEditContentToolbarHtmlService.addContainerToolbar(testDoc).then(() => {
            this.dotEditContentHtmlService.bindContainersEvents();
        });

        tick();
        expect(spyEditContentHtmlService).toHaveBeenCalledTimes(1);
    }));

    it('should bind contentlets events when dotEditContentToolbarHtmlService resolved', fakeAsync((): void => {
        const spyEditContentHtmlService = spyOn(this.dotEditContentHtmlService, 'bindContenletsEvents');

        this.dotEditContentToolbarHtmlService.addContainerToolbar(testDoc).then(() => {
            this.dotEditContentHtmlService.bindContenletsEvents();
        });

        tick();
        expect(spyEditContentHtmlService).toHaveBeenCalledTimes(1);
    }));
});
