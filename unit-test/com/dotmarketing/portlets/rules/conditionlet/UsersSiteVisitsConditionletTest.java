package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.com.sun.xml.bind.v2.runtime.IllegalAnnotationException;
import com.dotcms.repackage.org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import com.dotcms.repackage.ucar.nc2.stream.NcStreamProto.Data;
import com.dotcms.unittest.TestUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.GREATER_THAN_OR_EQUAL;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.LESS_THAN_OR_EQUAL;
import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionlet.SITE_VISITS_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsersSiteVisitsConditionletTest {
	
	
	@BeforeMethod
	public void setUp() throws Exception{
		MockitoAnnotations.initMocks(this);
	}
	
	@DataProvider(name="cases")
	public Object[][] compareCases() throws Exception{
		try{
			List<TestCase> data = List.newArrayList();
			
			data.add(new TestCase("If number of visits is Equal, evaluate to false")
					.withComparison(EQUAL)
					.withSiteVisits("10")
					.shouldBeFalse()
					);
			
			data.add(new TestCase("If number of visits is less, evaluate to true")
					.withComparison(LESS_THAN)
					.withSiteVisits("8")
					.shouldBeTrue()
					);
			
			data.add(new TestCase("If number of visits is less than or equal, evaluate to true")
					.withComparison(LESS_THAN_OR_EQUAL)
					.withSiteVisits("5")
					.shouldBeTrue()
					);
			
			data.add(new TestCase("If number of visits is great, evaluate to false")
					.withComparison(GREATER_THAN)
					.withSiteVisits("1")
					.shouldBeFalse()
					);
			
			data.add(new TestCase("If number of visits is great than or equal, evaluate to false")
					.withComparison(GREATER_THAN_OR_EQUAL)
					.withSiteVisits("1")
					.shouldBeFalse()
					);
		}
	}
	
	private class TestCase {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Map<String, ParameterModel> params = Maps.newLinkedHashMap();
        private final String testDescription;
        private boolean expect;


        public TestCase(String testDescription) {
            this.testDescription = testDescription;
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
            conditionlet = new UsersSiteVisitsConditionlet();
        }

        TestCase shouldBeTrue(){
            this.expect = true;
            return this;
        }

        TestCase shouldBeFalse() {
            this.expect = false;
            return this;
        }

        TestCase withComparison(Comparison c){
            params.put(COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, c != null ? c.getId() : null));
            return this;
        }

       TestCase withSiteVisits(String visits){
    	   params.put(SITE_VISITS_KEY, new ParameterModel(SITE_VISITS_KEY, visits));
       }

        @Override
        public String toString() {
            return testDescription;
        }
    }
}
