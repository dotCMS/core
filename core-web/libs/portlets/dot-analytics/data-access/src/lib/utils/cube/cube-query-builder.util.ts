import {
    CubeJSQuery,
    CubeJSTimeDimension,
    DimensionField,
    Granularity,
    MeasureField,
    OrderField,
    SortDirection,
    TimeRangeInput
} from '../../types';

/**
 * Simple CubeJS Query Builder for the 6 specific analytics requests
 */
export class CubeQueryBuilder {
    private query: CubeJSQuery = {};

    constructor() {
        this.query = {};
    }

    /**
     * Add measures to the query
     */
    measures(measures: MeasureField[]): CubeQueryBuilder {
        const prefixedMeasures = measures.map((m) =>
            m.startsWith('request.') ? m : `request.${m}`
        );
        this.query.measures = prefixedMeasures;

        return this;
    }

    /**
     * Add dimensions to the query
     */
    dimensions(dimensions: DimensionField[]): CubeQueryBuilder {
        const prefixedDimensions = dimensions.map((d) =>
            d.startsWith('request.') ? d : `request.${d}`
        );
        this.query.dimensions = prefixedDimensions;

        return this;
    }

    /**
     * Add pageview filter (common for all 6 requests)
     */
    pageviews(): CubeQueryBuilder {
        this.query.filters = [
            {
                member: 'request.eventType',
                operator: 'equals',
                values: ['pageview']
            }
        ];

        return this;
    }

    /**
     * Add time range - automatically detects TimeRange string vs DateRange array
     */
    timeRange(
        dimension: DimensionField,
        timeRangeInput: TimeRangeInput,
        granularity?: Granularity
    ): CubeQueryBuilder {
        const prefixedDimension = dimension.startsWith('request.')
            ? dimension
            : `request.${dimension}`;

        let dateRangeValue: string | [string, string];

        // Detect if it's a DateRange array or TimeRange string
        if (Array.isArray(timeRangeInput)) {
            // It's a DateRange array - use as tuple for CubeJS
            dateRangeValue = timeRangeInput as [string, string];
        } else {
            // It's a TimeRange string - use as-is
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
     * Add ordering
     */
    orderBy(field: OrderField, direction: SortDirection = 'desc'): CubeQueryBuilder {
        const prefixedField = field.startsWith('request.') ? field : `request.${field}`;
        this.query.order = { [prefixedField]: direction };

        return this;
    }

    /**
     * Add limit
     */
    limit(limit: number): CubeQueryBuilder {
        this.query.limit = limit;

        return this;
    }

    /**
     * Build the final query
     */
    build(): CubeJSQuery {
        return { ...this.query };
    }
}

/**
 * Create a new query builder instance
 */
export function createCubeQuery(): CubeQueryBuilder {
    return new CubeQueryBuilder();
}
