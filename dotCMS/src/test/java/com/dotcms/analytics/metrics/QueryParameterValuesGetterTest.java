package com.dotcms.analytics.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.experiments.business.result.Event;
import graphql.AssertException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class QueryParameterValuesGetterTest {
    @Test
    public void getQueryParamsFromUrlWithOneQueryParams(){
        final AbstractParameter parameter = mock(AbstractParameter.class);
        final Event event = mock(Event.class);
        when(event.get("url")).thenReturn(Optional.of("http://localhost:8080/test?testParam=testValue"));

        final QueryParameterValuesGetter queryParameterValuesGetter = new QueryParameterValuesGetter();
        final Collection<QueryParameter> valuesFromEvent = queryParameterValuesGetter
                .getValuesFromEvent(parameter, event);

        Assert.assertEquals(1, valuesFromEvent.size());

        final QueryParameter queryParameter = valuesFromEvent.iterator().next();
        Assert.assertEquals("testParam", queryParameter.getName());
        Assert.assertEquals("testValue", queryParameter.getValue());
    }

    @Test
    public void getQueryParamsFromUrlWithMultiQueryParams(){
        final AbstractParameter parameter = mock(AbstractParameter.class);
        final Event event = mock(Event.class);
        when(event.get("url")).thenReturn(Optional.of("http://localhost:8080/test?testParam1=testValue1&testParam2=testValue2"));

        final QueryParameterValuesGetter queryParameterValuesGetter = new QueryParameterValuesGetter();
        final Collection<QueryParameter> valuesFromEvent = queryParameterValuesGetter
                .getValuesFromEvent(parameter, event);

        Assert.assertEquals(2, valuesFromEvent.size());

        final Iterator<QueryParameter> iterator = valuesFromEvent.iterator();
        final QueryParameter queryParameter_1 = iterator.next();
        Assert.assertEquals("testParam1", queryParameter_1.getName());
        Assert.assertEquals("testValue1", queryParameter_1.getValue());

        final QueryParameter queryParameter_2 = iterator.next();
        Assert.assertEquals("testParam2", queryParameter_2.getName());
        Assert.assertEquals("testValue2", queryParameter_2.getValue());
    }

    @Test
    public void getQueryParamsFromUrlWithNoneQueryParams(){
        final AbstractParameter parameter = mock(AbstractParameter.class);
        final Event event = mock(Event.class);
        when(event.get("url")).thenReturn(Optional.of("http://localhost:8080/test"));

        final QueryParameterValuesGetter queryParameterValuesGetter = new QueryParameterValuesGetter();
        final Collection<QueryParameter> valuesFromEvent = queryParameterValuesGetter
                .getValuesFromEvent(parameter, event);

        Assert.assertTrue(valuesFromEvent.isEmpty());
    }
}
