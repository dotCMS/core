import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Tag } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentState } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentletStatusBadgeComponent } from './dot-contentlet-status-badge.component';

describe('DotContentletStatusBadgeComponent', () => {
    let spectator: Spectator<DotContentletStatusBadgeComponent>;

    // 'New' and 'Published' have explicit translations to prove every label goes
    // through DotMessageService; the rest fall back to the key (mock and real
    // service behave the same way).
    const messageServiceMock = new MockDotMessageService({
        New: 'New Translated',
        Published: 'Published Translated'
    });

    const createComponent = createComponentFactory({
        component: DotContentletStatusBadgeComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const getTag = () => spectator.query(Tag);

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('status derivation', () => {
        const cases: {
            case: string;
            state: DotContentState;
            value: string;
            severity: string;
        }[] = [
            {
                case: 'published',
                state: {
                    live: true,
                    working: true,
                    hasLiveVersion: true,
                    archived: false,
                    deleted: false
                },
                value: 'Published Translated',
                severity: 'success'
            },
            {
                case: 'archived',
                state: {
                    archived: true,
                    live: false,
                    working: false,
                    hasLiveVersion: false,
                    deleted: false
                },
                value: 'Archived',
                severity: 'danger'
            },
            {
                case: 'deleted',
                state: {
                    deleted: true,
                    live: false,
                    working: false,
                    hasLiveVersion: false,
                    archived: false
                },
                value: 'Archived',
                severity: 'danger'
            },
            {
                case: 'archived wins over live (precedence)',
                state: {
                    archived: true,
                    live: true,
                    working: true,
                    hasLiveVersion: true,
                    deleted: false
                },
                value: 'Archived',
                severity: 'danger'
            },
            {
                case: 'revision',
                state: {
                    live: false,
                    working: false,
                    hasLiveVersion: true,
                    archived: false,
                    deleted: false
                },
                value: 'Revision',
                severity: 'info'
            },
            {
                case: 'draft (all false)',
                state: {
                    live: false,
                    working: false,
                    hasLiveVersion: false,
                    archived: false,
                    deleted: false
                },
                value: 'Draft',
                severity: 'warn'
            },
            {
                case: 'live but not working → draft',
                state: {
                    live: true,
                    working: false,
                    hasLiveVersion: true,
                    archived: false,
                    deleted: false
                },
                value: 'Draft',
                severity: 'warn'
            },
            {
                case: 'live without live version → draft',
                state: {
                    live: true,
                    working: true,
                    hasLiveVersion: false,
                    archived: false,
                    deleted: false
                },
                value: 'Draft',
                severity: 'warn'
            }
        ];

        it.each(cases)(
            'should hand value "$value" and severity "$severity" to the Tag when $case',
            ({ state, value, severity }) => {
                spectator.setInput('state', state);

                const tag = getTag();
                expect(tag.value).toBe(value);
                expect(tag.severity).toBe(severity);
            }
        );
    });

    describe('null state', () => {
        it('should hand the translated "New" label and info severity to the Tag', () => {
            spectator.setInput('state', null);

            const tag = getTag();
            expect(tag.value).toBe('New Translated');
            expect(tag.severity).toBe('info');
        });
    });

    describe('rendering', () => {
        it('should render the status tag', () => {
            spectator.setInput('state', {
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            });

            expect(spectator.query(byTestId('status-tag'))).toBeTruthy();
        });
    });
});
