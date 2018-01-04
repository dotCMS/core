import { TestBed } from '@angular/core/testing';
import { DotEditContentToolbarHtmlService } from './dot-edit-content-toolbar-html.service';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';

describe('DotEditContentToolbarHtmlService', () => {
    let dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService;

    const testDoc = document.implementation.createDocument('http://www.w3.org/1999/xhtml', 'html', null);
    const htmlElement = testDoc.getElementsByTagName('html')[0];
    const dummyContainer = testDoc.createElement('div');
    dummyContainer.innerHTML = `<div data-dot-object="container">
                                        <div data-dot-object="contentlet">
                                            <div class="large-column"></div>
                                        </div>
                                    </div>`;
    htmlElement.appendChild(dummyContainer);

    const messageServiceMock = new MockDotMessageService({
        'editpage.content.contentlet.menu.drag': 'Drag',
        'editpage.content.contentlet.menu.edit': 'Edit',
        'editpage.content.contentlet.menu.remove': 'Remove',
        'editpage.content.container.action.add': 'Add',
        'editpage.content.container.menu.content': 'Content',
        'editpage.content.container.menu.widget': 'Widget',
        'editpage.content.container.menu.form': 'Form'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotEditContentToolbarHtmlService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
        dotEditContentToolbarHtmlService = TestBed.get(DotEditContentToolbarHtmlService);
    });

    it('should create the add and Toolbar for the container', () => {
        dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
        expect(testDoc.getElementsByClassName('dotedit-container__add').length).toEqual(1);
        expect(testDoc.getElementsByClassName('dotedit-container__menu-item').length).toEqual(3);
    });

    it('should create the Drag, Edit and Delete Button for the Content', () => {
        dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
        expect(testDoc.getElementsByClassName('dotedit-contentlet__drag').length).toEqual(1);
        expect(testDoc.getElementsByClassName('dotedit-contentlet__edit').length).toEqual(1);
        expect(testDoc.getElementsByClassName('dotedit-contentlet__remove').length).toEqual(1);
    });
});

