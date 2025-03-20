package com.dotcms.cube;

import static org.junit.Assert.assertEquals;

import com.dotcms.cube.CubeJSQuery.Builder;
import com.dotcms.cube.filters.Filter.Order;
import com.dotcms.cube.filters.LogicalFilter;

import com.dotcms.cube.filters.SimpleFilter;
import org.junit.Test;

public class CubeJSTest {

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment", "Events.variant"
     *   ]
     * }
     * </code>
     */
    @Test
    public void dimensions(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .build();

        final String queryExpected = "{"
                    + "\"dimensions\":["
                        + "\"Events.experiment\","
                        + "\"Events.variant\""
                    + "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with time dimension and date range equals to: Events.count
     * Should: Create the follow query:
     * <code>
     *  {
     *    "timeDimensions":[
     *       {
     *          "dimension":"Events.day",
     *          "granularity":"day",
     *          "dateRange":"This Week"
     *       }
     *    ],
     *    "dimensions":[
     *       "Events.day"
     *    ]
     * }
     * </code>
     */
    @Test
    public void timeDimensions(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.day")
                .timeDimension("Events.day", "day", "This Week")
                .build();

        final String queryExpected = "{\"timeDimensions\":[{\"dimension\":\"Events.day\",\"granularity\":\"day\",\"dateRange\":\"This Week\"}],\"dimensions\":[\"Events.day\"]}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with measures equals to: Events.count
     * Should: Create the follow query:
     * <code>
     *   {
     *   "measures": [
     *     "Events.count"
     *   ]
     * }
     * </code>
     */
    @Test
    public void measures(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .measures("Events.count")
                .build();

        final String queryExpected = "{"
                +   "\"measures\":["
                +       "\"Events.count\""
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * and measures equals to Events.count
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment", "Events.variant"
     *   ],
     *   "measures": [
     *     "Events.count"
     *   ]
     * }
     * </code>
     */
    @Test
    public void dimensionsAndMeasures(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .measures("Events.count")
                .dimensions("Events.experiment", "Events.variant")
                .build();

        final String queryExpected = "{"
                +   "\"measures\":["
                +       "\"Events.count\""
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.experiment\","
                +       "\"Events.variant\""
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment
     * and filters equals to Events.variant EQUALS TO "B"
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   filters: [
     *      {
     *          member: "Events.variant",
     *          operator: "equals",
     *          values: ["B"]
     *      }
     *   ]
     * }
     * </code>
     */
    @Test
    public void dimensionsAndFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment")
                .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                .build();

        final String queryExpected = "{"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":[\"B\"],"
                +           "\"member\":\"Events.variant\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.experiment\""
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with measures equals to: Events.count
     * and filters equals to Events.variant EQUALS TO "B"
     * Should: Create the follow query:
     * <code>
     *   {
     *   "measures": [
     *     "Events.count"
     *   ],
     *   filters: [
     *      {
     *          member: "Events.variant",
     *          operator: "equals",
     *          values: ["B"]
     *      }
     *   ]
     * }
     * </code>
     */
    @Test
    public void measuresAndFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .measures("Events.count")
                .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                .build();

        final String queryExpected = "{"
                +   "\"measures\":["
                +       "\"Events.count\""
                +   "],"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":[\"B\"],"
                +           "\"member\":\"Events.variant\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment
     * and filters equals to Events.variant EQUALS TO "B"
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   {
     *   "measures": [
     *     "Events.count"
     *   ],
     *   filters: [
     *      {
     *          member: "Events.variant",
     *          operator: "equals",
     *          values: ["B"]
     *      }
     *   ]
     * }
     * </code>
     */
    @Test
    public void dimensionAndMeasuresAndFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment")
                .measures("Events.count")
                .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                .build();

        final String queryExpected = "{"
                +   "\"measures\":["
                +       "\"Events.count\""
                +   "],"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":[\"B\"],"
                +           "\"member\":\"Events.variant\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.experiment\""
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with filters but without dimensions and measures
     * Should: Throw a {@link IllegalStateException}
     */
    @Test(expected = IllegalStateException.class)
    public void filterWithoutDimensions(){
        new Builder()
                .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                .build()
                .toString();
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * and two filters:
     * - Events.variant EQUALS TO "B"
     * - Events.experiment EQUALS TO "B"
     *
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   filters: [
     *      {
     *          member: "Events.variant",
     *          operator: "equals",
     *          values: ["B"]
     *      },
     *      {
     *          member: "Events.experiment",
     *          operator: "equals",
     *          values: ["B"]
     *      }
     *   ]
     * }
     * </code>
     */
    @Test
    public void dimensionAndTwoFilters(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                .filter("Events.experiment", SimpleFilter.Operator.EQUALS, "B")
                .build();

        final String queryExpected = "{"
                +   "\"filters\":["
                +       "{"
                +           "\"values\":[\"B\"],"
                +           "\"member\":\"Events.variant\","
                +           "\"operator\":\"equals\""
                +       "},"
                +       "{"
                +           "\"values\":[\"B\"],"
                +           "\"member\":\"Events.experiment\","
                +           "\"operator\":\"equals\""
                +       "}"
                +   "],"
                +   "\"dimensions\":["
                +       "\"Events.experiment\","
                +       "\"Events.variant\""
                +   "]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * and a AND filter with:
     * - Events.variant EQUALS TO "B"
     * - Events.experiment EQUALS TO "B"
     *
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   filters: [
     *      {
     *          "and": [
     *              {
     *                  member: "Events.variant",
     *                  operator: "equals",
     *                  values: ["B"]
     *              },
     *              {
     *                  member: "Events.experiment",
     *                  operator: "equals",
     *                  values: ["B"]
     *              }
     *          ]
     *     }
     *   ]
     * }
     * </code>
     */
    @Test
    public void andFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .filter(
                    LogicalFilter.Builder.and()
                        .add("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                        .add("Events.experiment", SimpleFilter.Operator.EQUALS, "B")
                        .build()
                )
                .build();

        final String queryExpected = "{"
                +   "\"filters\":["
                +       "{"
                +           "\"and\":["
                +               "{"
                +                   "\"values\":[\"B\"],"
                +                   "\"member\":\"Events.variant\","
                +                   "\"operator\":\"equals\""
                +               "},"
                +               "{"
                +                   "\"values\":[\"B\"],"
                +                   "\"member\":\"Events.experiment\","
                +                   "\"operator\":\"equals\""
                +               "}"
                +           "]"
                +       "}"
                + "],"
                + "\"dimensions\":[\"Events.experiment\",\"Events.variant\"]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * and a OR filter with:
     * - Events.variant EQUALS TO "B"
     * - Events.experiment EQUALS TO "B"
     *
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   filters: [
     *      {
     *          "or": [
     *              {
     *                  member: "Events.variant",
     *                  operator: "equals",
     *                  values: ["B"]
     *              },
     *              {
     *                  member: "Events.experiment",
     *                  operator: "equals",
     *                  values: ["B"]
     *              }
     *          ]
     *     }
     *   ]
     * }
     * </code>
     */
    @Test
    public void orFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .filter(
                        LogicalFilter.Builder.or()
                                .add("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                                .add("Events.experiment", SimpleFilter.Operator.EQUALS, "B")
                                .build()
                )
                .build();

        final String queryExpected = "{"
                +   "\"filters\":["
                +       "{"
                +           "\"or\":["
                +               "{"
                +                   "\"values\":[\"B\"],"
                +                   "\"member\":\"Events.variant\","
                +                   "\"operator\":\"equals\""
                +               "},"
                +               "{"
                +                   "\"values\":[\"B\"],"
                +                   "\"member\":\"Events.experiment\","
                +                   "\"operator\":\"equals\""
                +               "}"
                +           "]"
                +       "}"
                + "],"
                + "\"dimensions\":[\"Events.experiment\",\"Events.variant\"]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment and Events.variant
     * and a OR filter with:
     * (Events.variant EQUALS TO "B" AND Events.experiment EQUALS TO "A") OR Events.variant EQUALS TO "B"
     *
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment"
     *   ],
     *   filters: [
     *      {
     *          "or": [
     *              {
     *                  "and": [
     *                      {
     *                          member: "Events.variant",
     *                          operator: "equals",
     *                          values: ["B"]
     *                      },
     *                      {
     *                          member: "Events.experiment",
     *                          operator: "equals",
     *                          values: ["A"]
     *                      }
     *                  ]
     *              },
     *              {
     *                  member: "Events.variant",
     *                  operator: "equals",
     *                  values: ["B"]
     *              }
     *          ]
     *     }
     *   ]
     * }
     * </code>
     */
    @Test
    public void andOrFilter(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .filter(
                        LogicalFilter.Builder.or()
                                .add(LogicalFilter.Builder.and()
                                    .add("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                                    .add("Events.experiment", SimpleFilter.Operator.EQUALS, "A")
                                    .build()
                                )
                                .add("Events.variant", SimpleFilter.Operator.EQUALS, "B")
                                .build()
                )
                .build();

        final String queryExpected = "{"
                +   "\"filters\":["
                +       "{"
                +           "\"or\":["
                +               "{"
                +                   "\"and\":["
                +                       "{"
                +                           "\"values\":[\"B\"],"
                +                           "\"member\":\"Events.variant\","
                +                           "\"operator\":\"equals\""
                +                       "},"
                +                       "{"
                +                           "\"values\":[\"A\"],"
                +                           "\"member\":\"Events.experiment\","
                +                           "\"operator\":\"equals\""
                +                       "}"
                +                   "]"
                +               "},"
                +               "{"
                +                   "\"values\":[\"B\"],"
                +                   "\"member\":\"Events.variant\","
                +                   "\"operator\":\"equals\""
                +               "}"
                +           "]"
                +       "}"
                + "],"
                + "\"dimensions\":[\"Events.experiment\",\"Events.variant\"]"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }

    /**
     * Method to test: {@link Builder#build()}
     * When: Create a CubeJS Query with dimentions equals to: Events.experiment, Events.variant and Events.utcTime
     * and order by Events.utcTime
     * Should: Create the follow query:
     * <code>
     *   {
     *   "dimensions": [
     *     "Events.experiment", "Events.variant", "Events.utcTime"
     *   ],
     *   "order": {
     *      "Events.experiment": "asc",
     *      "Events.utcTime": "desc"
     *   }
     * }
     * </code>
     */
    @Test
    public void order(){
        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant", "Events.utcTime")
                .order("Events.experiment", Order.ASC)
                .order("Events.utcTime", Order.DESC)
                .build();

        final String queryExpected = "{"
                +   "\"dimensions\":["
                +       "\"Events.experiment\","
                +       "\"Events.variant\","
                +       "\"Events.utcTime\""
                +    "],"
                +    "\"order\":{"
                +       "\"Events.experiment\":\"asc\","
                +       "\"Events.utcTime\":\"desc\""
                +    "}"
                + "}";

        assertEquals(queryExpected, cubeJSQuery.toString());
    }
}
