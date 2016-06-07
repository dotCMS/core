# JUnit Tests:

Things you need to know before running the tests:

#####  Modify configuration properties:
This can be done in several ways, the main file for this will be the build.properties inside the test module (./test/build.properties), you could modify this file directly, override it using the ROOT folder of any of your plugins or you could pass directly the properties to ant (if your are running the ant call to execute the tests). 

List of properties you can pass (to ant file) OR modify (build.properties) with their default values:

    ## Data Base
    db.driver=org.postgresql.Driver
    db.base.url=jdbc:postgresql://localhost/
    db.username=postgres
    db.password=root
    db.jar=postgresql-9.0-801.jdbc3.jar
    db.name=dotcms_test
    db.url=${db.base.url}${db.name}
    
    ## Tomcat
    server.port=8081
    server.test.url=http://localhost:${server.port}/servlet/test

##### [Test URLs](#test-urls) :
There are diferent ways to call the tests and depending of the way they are call it the result will change as well.
There are three parameters the test server can expect:
1. class: You can specify which junit class you want to run, if this parameter is not present all the tests are call.
    
    * http://localhost:8081/servlet/test?class=com.dotmarketing.portlets.structure.factories.StructureFactoryTest   -->  Only execute StructureFactoryTest class

    * http://localhost:8081/servlet/test     --> Will execute all the tests
				
2. method: You can specify which test method of a test class you want to run. This needs to be combined with "class" in 1.
    
    * http://localhost:8081/servlet/test?class=com.dotmarketing.portlets.structure.factories.StructureFactoryTest&method=getStructureByInode   -->  Only execute StructureFactoryTest.getStructureByInode method
			
3) resultType: Expecting values are "file" and "plain". If nothing is set "file" is the default behaviour.
file: It will generate an xml report under /tomcat/logs/test/
plain: It will display the result of the tests as plain/text on the browser and console, also it will append the result of the tests in a log file (./tomcat/logs/dotcms-testing.log). This method is very usefull when local testing is made.

    * http://localhost:8081/servlet/test?resultType=file    	--> xml report
    * http://localhost:8081/servlet/test?resultType=plain	--> plain/text result
    * http://localhost:8081/servlet/test			--> xml report
    * http://localhost:8081/servlet/test?class=com.dotmarketing.portlets.structure.factories.StructureFactoryTest&resultType=plain	--> plain/text result
    * http://localhost:8081/servlet/test?class=com.dotmarketing.portlets.structure.factories.StructureFactoryTest			--> xml report

If all the test passed the test servlet will return a 200 responde status code otherwise a 500 response status error code. 

##### [Running the tests](#running-tests):
* Using ant tasks: Execute the "test-dotcms" task of the build.xml in order to run all the tests.

* Making direct calls to the servlet
	1. Execute the "deploy-tests" task of the build.xml
	2. Run the application
	3. Make the calls directly to the test servlet.


##### [Adding more tests](#adding-tests):
The test module is under ./test/ Inside this module you should add your unit tests classes, the default behaviour for the servlet will be to call the main test suite (com.AllTestsSuite) unless you specify a class to run (class parameter). If you spect to have your test class/test suite running in the default way you should add it to the main test suite.

	@RunWith ( Suite.class )
	@Suite.SuiteClasses ( {
		FieldFactoryTest.class,
		StructureFactoryTest.class,
		ContentletFactoryTest.class,
		ContentletAPITest.class
	} )
	public class AllTestsSuite {

	}

You just need to add your class (test class/test suite) inside the @Suite.SuiteClasses anotation.


