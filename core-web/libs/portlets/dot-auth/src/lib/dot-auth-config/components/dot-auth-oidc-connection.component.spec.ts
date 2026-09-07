import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthOidcConnectionComponent } from './dot-auth-oidc-connection.component';

import { DEFAULT_CONFIG } from '../store/dot-auth-config.mappers';

describe('DotAuthOidcConnectionComponent', () => {
    let spectator: Spectator<DotAuthOidcConnectionComponent>;

    const createComponent = createComponentFactory({
        component: DotAuthOidcConnectionComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                oidc: structuredClone(DEFAULT_CONFIG.oidc)
            }
        });
    });

    describe('advanced section groups fields', () => {
        beforeEach(() => {
            spectator.component.showAdvanced.set(true);
            spectator.detectChanges();
        });

        it('renders the groups URL and response path inputs', () => {
            expect(spectator.query(byTestId('groups-url'))).toBeTruthy();
            expect(spectator.query(byTestId('groups-response-path'))).toBeTruthy();
        });

        it('emits oidc.groupsUrl on groups URL input', () => {
            const emitted: unknown[] = [];
            spectator.output('fieldChange').subscribe((change) => emitted.push(change));

            spectator.typeInElement(
                "https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups?query=member_key_id=='{email}'",
                spectator.query(byTestId('groups-url')) as HTMLInputElement
            );

            expect(emitted.pop()).toEqual({
                path: 'oidc.groupsUrl',
                value: "https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups?query=member_key_id=='{email}'"
            });
        });

        it('emits oidc.groupsResponsePath on response path input', () => {
            const emitted: unknown[] = [];
            spectator.output('fieldChange').subscribe((change) => emitted.push(change));

            spectator.typeInElement(
                'memberships[].groupKey.id',
                spectator.query(byTestId('groups-response-path')) as HTMLInputElement
            );

            expect(emitted.pop()).toEqual({
                path: 'oidc.groupsResponsePath',
                value: 'memberships[].groupKey.id'
            });
        });
    });

    it('hides the groups fields while the advanced section is collapsed', () => {
        expect(spectator.query(byTestId('groups-url'))).toBeNull();
        expect(spectator.query(byTestId('groups-response-path'))).toBeNull();
    });
});
