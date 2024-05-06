import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { Chip, ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import {
    DotCopyButtonComponent,
    DotLinkComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
import {
    DotFormatDateServiceMock,
    EMPTY_CONTENTLET,
    MockDotMessageService,
    dotcmsContentTypeBasicMock
} from '@dotcms/utils-testing';

import { DotContentAsideInformationComponent } from './dot-content-aside-information.component';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';

const messageServiceMock = new MockDotMessageService({
    Published: 'Published',
    Modified: 'Modified',
    Created: 'Created',
    'Content-Type': 'Content Type',
    References: 'References'
});

const BASIC_CONTENTLET = {
    ...EMPTY_CONTENTLET,
    publishDate: '2021-01-01T00:00:00Z',
    publishUserName: 'admin',
    modDate: '2021-01-01T00:00:00Z',
    modUserName: 'admin',
    ownerName: 'admin',
    createDate: '2021-01-01T00:00:00Z'
};

const CONTENT_TYPE_MOCK = {
    ...dotcmsContentTypeBasicMock,
    variable: 'BlogVariable',
    name: 'Blog'
};

describe('DotContentAsideInformationComponent', () => {
    let spectator: Spectator<DotContentAsideInformationComponent>;
    let dotRelativeDatePipe: DotRelativeDatePipe;
    let router: Router;

    const createComponent = createComponentFactory({
        component: DotContentAsideInformationComponent,
        imports: [
            RouterTestingModule.withRoutes([
                {
                    path: 'content-types-angular/edit/:contentType',
                    component: DotContentAsideInformationComponent
                }
            ]),
            TooltipModule,
            ChipModule,
            ContentletStatusPipe,
            DotMessagePipe,
            DotRelativeDatePipe,
            MockComponent(DotCopyButtonComponent),
            MockComponent(DotLinkComponent)
        ],
        providers: [
            DotRelativeDatePipe,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                contentlet: BASIC_CONTENTLET,
                contentType: CONTENT_TYPE_MOCK,
                loading: false
            }
        });

        router = spectator.inject(Router);
        dotRelativeDatePipe = spectator.inject(DotRelativeDatePipe, true);
    });

    it('should have a copy button', () => {
        spectator.detectChanges();
        const dotCopyButton = spectator.query(DotCopyButtonComponent);
        expect(dotCopyButton).not.toBeNull();
        expect(dotCopyButton.copy).toBe(BASIC_CONTENTLET.inode);
        expect(dotCopyButton.label).toBe(`ID: ${BASIC_CONTENTLET.inode.slice(-10)}`);
    });

    it('should have a chip status', () => {
        spectator.detectChanges();
        const chipComponent = spectator.query(Chip);
        expect(chipComponent).not.toBeNull();
        expect(chipComponent.label).toBe('Revision');
        expect(chipComponent.styleClass).toBe('p-chip-sm p-chip-pink');
    });

    it('should have a link to the content type', fakeAsync(() => {
        spectator.detectChanges();

        const linkElement = spectator.query(byTestId('content-type-link'));
        const span = linkElement.querySelector('.sidebar-card__header');
        const label = linkElement.querySelector('.sidebar-card__label');
        expect(linkElement).not.toBeNull();
        expect(span.textContent.trim()).toBe('Content Type');
        expect(label.textContent.trim()).toBe('Blog');

        linkElement.dispatchEvent(new Event('click'));
        spectator.click(linkElement);
        spectator.detectChanges();
        tick();
        expect(router.url).toBe(`/content-types-angular/edit/BlogVariable`);
    }));

    it('should have a references button', () => {
        spectator.detectChanges();
        const referencesButton = spectator.query(byTestId('references-button'));
        const referencesSpan = referencesButton.querySelector('span');
        expect(referencesButton).not.toBeNull();
        expect(referencesSpan.textContent.trim()).toBe('References');
    });

    describe('history', () => {
        it('should have publish history button', () => {
            spectator.detectChanges();
            const publishButton = spectator.query(byTestId('publish-history'));
            const publishLabel = publishButton.querySelector('.content-history__title');
            const publishAuthor = publishButton.querySelector('.content-history__author');
            const publishDate = publishButton.querySelector('.content-history__date');

            expect(publishButton).not.toBeNull();
            expect(publishLabel.innerHTML.trim()).toBe('Published');
            expect(publishAuthor.textContent.trim()).toBe('admin');
            expect(publishDate.textContent.trim()).toBe(
                dotRelativeDatePipe.transform(BASIC_CONTENTLET.publishDate)
            );
        });

        it('should have Modify history button', () => {
            spectator.detectChanges();
            const modButton = spectator.query(byTestId('mod-history'));
            const modLabel = modButton.querySelector('.content-history__title');
            const modAuthor = modButton.querySelector('.content-history__author');
            const modDate = modButton.querySelector('.content-history__date');

            expect(modButton).not.toBeNull();
            expect(modLabel.innerHTML.trim()).toBe('Modified');
            expect(modAuthor.textContent.trim()).toBe('admin');
            expect(modDate.textContent.trim()).toBe(
                dotRelativeDatePipe.transform(BASIC_CONTENTLET.modDate)
            );
        });

        it('should have Create history button', () => {
            spectator.detectChanges();
            const createButton = spectator.query(byTestId('create-history'));
            const createLabel = createButton.querySelector('.content-history__title');
            const createAuthor = createButton.querySelector('.content-history__author');
            const createDate = createButton.querySelector('.content-history__date');

            expect(createButton).not.toBeNull();
            expect(createLabel.innerHTML.trim()).toBe('Created');
            expect(createAuthor.textContent.trim()).toBe('admin');
            expect(createDate.textContent.trim()).toBe(
                dotRelativeDatePipe.transform(BASIC_CONTENTLET.createDate)
            );
        });

        it('should have history buttons', () => {
            spectator.detectChanges();
            const historyContainer = spectator.query(byTestId('history-container'));
            const publishButton = spectator.query(byTestId('publish-history'));
            const modButton = spectator.query(byTestId('mod-history'));
            const createHistory = spectator.query(byTestId('create-history'));

            expect(historyContainer).not.toBeNull();
            expect(publishButton).not.toBeNull();
            expect(modButton).not.toBeNull();
            expect(createHistory).not.toBeNull();
        });
    });
});
