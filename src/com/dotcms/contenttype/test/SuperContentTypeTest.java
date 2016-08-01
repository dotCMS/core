package com.dotcms.contenttype.test;
import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.ServletContext;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.dotmarketing.db.test.DataSourceForTesting;
import com.dotmarketing.util.Config;
import com.google.common.io.Files;



	@RunWith(Suite.class)
	@SuiteClasses({ 
		ContentTypeBuilderTest.class, 
		ContentTypeFactoryImplTest.class, 
		FieldFactoryImplTest.class, 
		FieldBuilderTest.class 
	})
	

	public class SuperContentTypeTest   {
		public interface Application {
			  public String myFunction(String abc);
		
		}
		
		
		@BeforeClass
		public static void SetUpTests() throws FileNotFoundException, Exception {
			
			new DataSourceForTesting().setup();
			
			
			
			ServletContext context = Mockito.mock(ServletContext.class);
			final String topPath= Files.createTempDir().getCanonicalPath();
			Mockito.when(context.getRealPath(Matchers.anyString())).thenAnswer(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) throws Throwable {
					String path = (String) invocation.getArguments()[0];
					path = topPath + path.replaceAll("/", File.separator);
	
					return path;
				}
			});

			Config.CONTEXT = context;

		}
	}
