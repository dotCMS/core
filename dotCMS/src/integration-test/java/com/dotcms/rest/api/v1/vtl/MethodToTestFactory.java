package com.dotcms.rest.api.v1.vtl;

public class MethodToTestFactory {

    public static MethodToTest getMethodToTest(final VTLResourceIntegrationTest.ResourceMethod method) {
        MethodToTest methodToTest = new GetMethod(); // default value

        if(method == VTLResourceIntegrationTest.ResourceMethod.GET) {
            methodToTest = new GetMethod();
        } else if(method == VTLResourceIntegrationTest.ResourceMethod.DYNAMIC_GET) {
            methodToTest = new DynamicGetMethod();
        }

        return methodToTest;
    }
}
