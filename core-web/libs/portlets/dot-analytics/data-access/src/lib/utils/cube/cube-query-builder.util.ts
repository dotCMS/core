import {
    CubeJSFilter,
    CubeJSQuery,
    CubeJSTimeDimension,
    CubePrefix,
    DimensionField,
    FilterOperator,
    Granularity,
    SortDirection,
    TimeRangeCubeJS
} from '../../types';

/**
 * CubeJS Query Builder for analytics requests.
 *
 * Supports two cube types:
 * - `request`: For pageview analytics (default)
 * - `EventSummary`: For conversion analytics
 *
 * @example
 * ```typescript
 * // Pageview query
 * createCubeQuery()
 *     .measures(['totalRequest'])
 *     .pageviews()
 *     .siteId('site-123')
 *     .build();
 *
 * // Conversion query
 * createCubeQuery()
 *     .measures(['totalEvents'])
 *     .conversions()
 *     .build();
 * ```
 */
export class CubeQueryBuilder {
    private query: CubeJSQuery = {};
    private currentPrefix: CubePrefix = 'request';

    constructor() {
        this.query = {};
    }

    /**
     * Set the cube to query from.
     * All subsequent operations will use this cube's prefix.
     *
     * @param prefix - The cube to use ('request' for pageviews, 'EventSummary' for conversions)
     */
    fromCube(prefix: CubePrefix): CubeQueryBuilder {
        this.currentPrefix = prefix;

        return this;
    }

    /**
     * Add measures to the query.
     * Automatically prefixes with current cube (request or EventSummary).
     */
    measures(measures: string[]): CubeQueryBuilder {
        const prefixedMeasures = measures.map((m) => this.prefixField(m));
        this.query.measures = prefixedMeasures;

        return this;
    }

    /**
     * Add dimensions to the query.
     * Automatically prefixes with current cube.
     *
     * @param dimensions - Array of dimension field names (e.g., ['path', 'pageTitle'])
     */
    dimensions(dimensions: DimensionField[]): CubeQueryBuilder {
        const prefixedDimensions = dimensions.map((d) => this.prefixField(d));
        this.query.dimensions = prefixedDimensions;

        return this;
    }

    /**
     * Add pageview filter (eventType = 'pageview').
     * Use with fromCube('request') for pageview analytics.
     */
    pageviews(): CubeQueryBuilder {
        this.addFilter({
            member: this.prefixField('eventType'),
            operator: 'equals',
            values: ['pageview']
        });

        return this;
    }

    /**
     * Add conversion filter (eventType = 'conversion').
     * Use with fromCube('EventSummary') for conversion analytics.
     */
    conversions(): CubeQueryBuilder {
        this.addFilter({
            member: this.prefixField('eventType'),
            operator: 'equals',
            values: ['conversion']
        });

        return this;
    }

    /**
     * Add site ID filter using current cube prefix.
     */
    siteId(siteId: string | string[]): CubeQueryBuilder {
        const values = Array.isArray(siteId) ? siteId : [siteId];
        const siteIdFilter: CubeJSFilter = {
            member: this.prefixField('siteId'),
            operator: 'equals',
            values
        };

        this.addFilter(siteIdFilter);

        return this;
    }

    /**
     * Add a custom filter.
     * Automatically prefixes member with current cube.
     */
    filter(member: string, operator: FilterOperator, values: string[]): CubeQueryBuilder {
        const customFilter: CubeJSFilter = {
            member: this.prefixField(member),
            operator,
            values
        };

        this.addFilter(customFilter);

        return this;
    }

    /**
     * Add multiple filters at once.
     * Automatically prefixes members with current cube.
     */
    filters(filters: CubeJSFilter[]): CubeQueryBuilder {
        const prefixedFilters = filters.map((filter) => ({
            ...filter,
            member: this.prefixField(filter.member)
        }));

        prefixedFilters.forEach((f) => this.addFilter(f));

        return this;
    }

    /**
     * Add time range with optional granularity.
     * Automatically prefixes dimension with current cube.
     */
    timeRange(
        dimension: string,
        timeRangeInput: TimeRangeCubeJS,
        granularity?: Granularity
    ): CubeQueryBuilder {
        const prefixedDimension = this.prefixField(dimension);

        let dateRangeValue: string | [string, string];

        if (Array.isArray(timeRangeInput)) {
            dateRangeValue = timeRangeInput as [string, string];
        } else {
            dateRangeValue = timeRangeInput as string;
        }

        const timeDimension: CubeJSTimeDimension = {
            dimension: prefixedDimension,
            dateRange: dateRangeValue
        };

        if (granularity) {
            timeDimension.granularity = granularity;
        }

        this.query.timeDimensions = [timeDimension];

        return this;
    }

    /**
     * Add ordering by field.
     * Automatically prefixes field with current cube.
     */
    orderBy(field: string, direction: SortDirection = 'desc'): CubeQueryBuilder {
        const prefixedField = this.prefixField(field);
        this.query.order = { [prefixedField]: direction };

        return this;
    }

    /**
     * Set result limit.
     */
    limit(limit: number): CubeQueryBuilder {
        this.query.limit = limit;

        return this;
    }

    /**
     * Build the final query object.
     */
    build(): CubeJSQuery {
        return { ...this.query };
    }

    /**
     * Prefix a field with the current cube name if not already prefixed.
     */
    private prefixField(field: string): string {
        if (field.includes('.')) {
            return field;
        }

        return `${this.currentPrefix}.${field}`;
    }

    /**
     * Add a filter to the query.
     */
    private addFilter(filter: CubeJSFilter): void {
        this.query.filters = this.query.filters ? [...this.query.filters, filter] : [filter];
    }
}

/**
 * Create a new CubeJS query builder instance.
 * Default cube is 'request' for pageview queries.
 */
export function createCubeQuery(): CubeQueryBuilder {
    return new CubeQueryBuilder();
}
