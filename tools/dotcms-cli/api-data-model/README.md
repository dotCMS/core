# api-data-model

This module implements the base API defined in the CLI using JAX-RS to
provide a Rest interface for calling the api.   We make the rest classes call a delegate implementation
of the service interface,  we could make the resource class implement the service interface itself
but not doing so can provide some flexibility, e.g. you may not want all the public service methods to be rest calls
or you may need to do some special JAX-RS handling to implement the method. 

Any REST specific handling, e.g. authentication, exception mapping, response object, streaming handling
can be done in here that are not concerns of the underlying java interface.

If the project using this allows for CDI it would be easy to add the API and inject the service implementations.
Otherwise, the owner will need to manage the annotation processing itself and methods will need to be created to allow manual injection of the implementation.