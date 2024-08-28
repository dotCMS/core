# AI Clients

This initiative that is introduced to dotCMS in several commits but the main idea is to provide a way to interact with AI services in a more generic way.

## From OpenAIRequest to AIClient
Basically we use to have a class called `OpenAIRequest` that was used to interact with OpenAI services. This class was a bit coupled with OpenAI services and we decided to create a more generic way to interact with AI services.
So we created the `AIClient` interface which will be implemented by any component that needs to interact with an AI Provider.
Since OpenAI is the first AI provider we are integrating with, we created the `OpenAIClient` class that implements the `AIClient` interface.

Usually, the `AIClient` will have a method called `sendRequest` that will receive a `Request` object and return a `Response` object.
That's it. You should not have to worry about instantiating the `AIClient` implementation, since this is managed by a higher level component called `AIClientProxy`. 

## AIClientProxy
This class will hold an internal structure that maps the AI provider identification to something called `AIProxiedClient`. 

The `AIClientProxy` is a class that will manage the instantiation of the `AIClient` implementation.

### AIProxiedClient
The mere purpose of this class it to hold the `AIClient` implementation, an `AIClientStrategy` implementation and a `AIResponseEvaluator` implementation. 
It's like a intersection point between these three classes and to keep decoupled enough the concepts of a AI client and the strategy used to send requests to the AI provider and finally how to interpret/evaluate the response from the AI provider.
It provides the flexibility of decouple a strategy from the client and from the response evaluator.

### AIClientStrategy
This class is responsible for defining the strategy that the `AIClient` will use to interact with the AI provider.
Currently there are 2 strategies:
- `AIDefaultStrategy`: This strategy will be used when the desired behaviour is to just try to send a request.
- `AIModelFallbackStrategy`: This strategy will be used when the desired behaviour is to try to send a request to a model and if it fails, try to send the request to another model and so on until options are exhausted.

### AIResponseEvaluator
This class is responsible for defining how the response from the AI provider will be evaluated. We will look for specific contents in the response to resolve (if any) the type of error and how to handle it. 

## How to use the whole thing
`AIClientProxy` is a singleton class that is the entrypoint for any other dotCMS component which needs to consume AI output.
It used to be the `OpenAIRequest` class but now it should be the `AIClientProxy` class since it provides multi-provider and multi-strategy support.

Given a `AIRequest` instance (which will be built before calling this entrypoint and that for the sake of this example it will be built inline) and an `OutputStream` subclass, this is how you use it in your code:

```java
final AIReponse response = AIProxyClient.get()
        .sendRequest(
                AIProvider.OPEN_AI.name(),
                OpenAIRequest.builder()
                        .model("gpt-3.5-turbo")
                        .prompt("Once upon a time")
                        .build(),
                output);
```
Based on the provider name, behind curtains, the `AIClientProxy` will look for the `AIProxiedClient` instance that is registered with the provider name and will use the strategy and response evaluator that are registered with the `AIProxiedClient` instance.

You can register instances of `AIProxiedClient` that will be used by the `AIClientProxy` like this:

```java
AIProxyClient.get().addClient(
        AIProvider.OPEN_AI.name(),
        AIProxiedClient.of(
                OpenAIClient.get(),
                AIProxyStrategy.MODEL_FALLBACK,
                OpenAIResponseEvaluator.get()));
```
Here we can see how the three main elements are conjugated in a single `AIProxiedClient` instance.
Here we are registering an `OpenAIClient` instance with the `MODEL_FALLBACK` strategy and the `OpenAIResponseEvaluator` instance and it means that whenvever 