package com.dotcms.analytics.metrics;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.analytics.metrics.ParameterValuesTransformer.Values;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test of {@link QueryParameterValuesTransformer}
 */
public class QueryParameterValuesTransformerTest {

    /**
     * Method to test: {@link QueryParameterValuesTransformer}
     * When: Has two Query Params: testName=TestValue and testName2=testValue2 and the Condition values is
     * equals to {"name": "TestName", "value": "testValue"}
     * Should: return not Real values because the Query Parameters are case Sensitivee
     */
    @Test
    public void transformQueryParamsValuesCaseSensitive(){
        final QueryParameterValuesTransformer queryParameterValuesTransformer = new QueryParameterValuesTransformer();

        final Collection<QueryParameter> valuesFromEvent = list(
                new QueryParameter("testName", "anyValue"),
                new QueryParameter("testName2", "TestValue2")
        );
        final AbstractCondition condition = mock(AbstractCondition.class);
        when(condition.value()).thenReturn(new QueryParameter("TestName", "testValue"));

        final Values transforms = queryParameterValuesTransformer.transform(
                valuesFromEvent, condition);

        Assert.assertEquals(0, transforms.getRealValues().size());

        Assert.assertEquals("testValue", transforms.getConditionValue());
    }


    /**
     * Method to test: {@link QueryParameterValuesTransformer}
     * When: Has two Query Params: testName=anyValue and testName2=testValue2 and the Condition values is
     * equals to {"name": "testName", "value": "testValue"}
     * Should: return the value of the Query Parameter and the Condition
     */
    @Test
    public void transformQueryParamsValues(){
        final QueryParameterValuesTransformer queryParameterValuesTransformer = new QueryParameterValuesTransformer();

        final Collection<QueryParameter> valuesFromEvent = list(
                new QueryParameter("testName", "anyValue"),
                new QueryParameter("testName2", "TestValue2")
        );
        final AbstractCondition condition = mock(AbstractCondition.class);
        when(condition.value()).thenReturn(new QueryParameter("testName", "testValue"));

        final Values transforms = queryParameterValuesTransformer.transform(
                valuesFromEvent, condition);

        Assert.assertEquals(1, transforms.getRealValues().size());

        Assert.assertEquals("anyValue", transforms.getRealValues().iterator().next());
        Assert.assertEquals("testValue", transforms.getConditionValue());
    }
}
