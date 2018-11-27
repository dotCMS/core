import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { DotRelationshipService } from './dot-relationship.service';
import { Response, ConnectionBackend, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { tick, fakeAsync } from '@angular/core/testing';

const cardinalities = [
    {
        label: 'Many to many',
        id: 0,
        name: 'MANY_TO_MANY'
    },
    {
        label: 'One to one',
        id: 1,
        name: 'ONE_TO_ONE'
    }
];

describe('RelationshipService', () => {
    let relationshipService: DotRelationshipService;

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotRelationshipService]);

        relationshipService = this.injector.get(DotRelationshipService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should load cardinalities', fakeAsync(() => {
        relationshipService.loadCardinalities().subscribe((res) => this.response = res);

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: cardinalities
                    }
                })
            )
        );

        tick();

        expect(this.response).toEqual(cardinalities);
    }));
});
