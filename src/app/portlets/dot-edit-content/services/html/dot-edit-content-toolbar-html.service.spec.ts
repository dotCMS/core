import { TestBed } from '@angular/core/testing';

import { DotEditContentToolbarHtmlService } from './dot-edit-content-toolbar-html.service';

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

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotEditContentToolbarHtmlService]
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

