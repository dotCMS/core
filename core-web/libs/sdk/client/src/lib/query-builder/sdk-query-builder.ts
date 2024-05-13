import { buildField, santizeQuery } from './utils';

export class QueryBuilder {
    protected _query = '';

    field(field: string) {
        return buildField(this._query, field);
    }

    excludeField(field: string) {
        return buildField(this._query, field, true);
    }

    build() {
        return santizeQuery(this._query);
    }
}
