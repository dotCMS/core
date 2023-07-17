package com.dotcms.analytics.metrics;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

public class QueryParameterValuesTransformerTest {

    /**
     * Method to test: {@link QueryParameterValuesTransformer}
     * When: Has two Query Params: testName=TestValue and testName2=testValue2 and the Condition values is
     * equals to {"name": "TestName", "value": "testValue"}
     * Should: return a String with the value 'TestValue'
     */
    @Test
    public void transformQueryParamsValues(){
        final QueryParameterValuesTransformer queryParameterValuesTransformer = new QueryParameterValuesTransformer();

        final Collection<QueryParameter> valuesFromEvent = list(
                new QueryParameter("testName", "anyValue"),
                new QueryParameter("testName2", "TestValue2")
        );
        final AbstractCondition condition = mock(AbstractCondition.class);
        when(condition.value()).thenReturn("{\"name\": \"TestName\", \"value\": \"testValue\"}");

        final Collection<String> transforms = queryParameterValuesTransformer.transform(
                valuesFromEvent, condition);

        Assert.assertEquals(1, transforms.size());

        Assert.assertEquals("anyValue", transforms.iterator().next());
    }

}
