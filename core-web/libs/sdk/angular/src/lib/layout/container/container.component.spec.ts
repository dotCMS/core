import { Spectator, byTestId, byText, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { Component, Input } from '@angular/core';

import { ContainerComponent } from './container.component';

import { NoComponent } from '../../components/no-component/no-component.component';
import { DotCMSContainer, DotCMSContentlet } from '../../models';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { PageResponseMock } from '../../utils/testing.utils';

@Component({
    selector: 'dotcms-mock-component',
    standalone: true,
    template: 'Hello world'
})
class DotcmsSDKMockComponent {
    @Input() contentlet!: DotCMSContentlet;
}

describe('ContainerComponent', () => {
    let spectator: Spectator<ContainerComponent>;

    describe('inside editor', () => {
        const createComponent = createComponentFactory({
            component: ContainerComponent,
            detectChanges: false,
            providers: [
                {
                    provide: PageContextService,
                    useValue: {
                        pageContextValue: {
                            pageAsset: {
                                containers: PageResponseMock.containers
                            },
                            components: {
                                Banner: of(DotcmsSDKMockComponent)
                            },
                            isInsideEditor: true
                        }
                    }
                }
            ]
        });

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    container: PageResponseMock.layout.body.rows[0].columns[0]
                        .containers[0] as DotCMSContainer
                }
            });
        });

        it('should render MockContainerComponent', () => {
            spectator.detectChanges();
            expect(spectator.query(DotcmsSDKMockComponent)).toBeTruthy();
        });

        it('should container have data attributes', () => {
            spectator.detectChanges();
            const container = spectator.debugElement.nativeElement;
            expect(container?.getAttribute('data-dot-accept-types')).toBeDefined();
            expect(container?.getAttribute('data-dot-identifier')).toBeDefined();
            expect(container?.getAttribute('data-max-contentlets')).toBeDefined();
            expect(container?.getAttribute('ata-dot-uuid')).toBeDefined();
        });

        it('should contentlets have data attributes', () => {
            spectator.detectChanges();
            const contentlets = spectator.queryAll(byTestId('dot-contentlet'));
            contentlets.forEach((contentlet) => {
                expect(contentlet.getAttribute('data-dot-identifier')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-basetype')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-title')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-inode')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-type')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-container')).toBeDefined();
                expect(contentlet.getAttribute('data-dot-on-number-of-pages')).toBeDefined();
            });
        });

        it('should render NoComponentComponent when no component is found', () => {
            spectator.setInput(
                'container',
                PageResponseMock.layout.body.rows[1].columns[0].containers[0] as DotCMSContainer
            );
            spectator.detectChanges();
            expect(spectator.query(NoComponent)).toBeTruthy();
        });

        it('should render message when container is empty', () => {
            spectator.setInput(
                'container',
                PageResponseMock.layout.body.rows[2].columns[0].containers[0] as DotCMSContainer
            );
            spectator.detectChanges();
            expect(spectator.query(byText('This container is empty.'))).toBeTruthy();
        });
    });

    describe('outside editor', () => {
        const createComponent = createComponentFactory({
            component: ContainerComponent,
            detectChanges: false,
            providers: [
                {
                    provide: PageContextService,
                    useValue: {
                        pageContextValue: {
                            pageAsset: {
                                containers: PageResponseMock.containers
                            },
                            components: {
                                Banner: of(DotcmsSDKMockComponent)
                            },
                            isInsideEditor: false
                        }
                    }
                }
            ]
        });

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    container: PageResponseMock.layout.body.rows[0].columns[0]
                        .containers[0] as DotCMSContainer
                }
            });
        });

        it('should not have data attributes', () => {
            spectator.detectChanges();
            const container = spectator.query(byTestId('dot-container'));
            expect(container?.getAttribute('data-dot-accept-types')).toBeUndefined();

            const contentlets = spectator.queryAll(byTestId('dot-contentlet'));
            contentlets.forEach((contentlet) => {
                expect(contentlet.getAttribute('data-dot-identifier')).toBeUndefined();
            });
        });
    });
});
