package com.dotmarketing.portlets.chains;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.business.ChainAPI;
import com.dotmarketing.portlets.chains.business.ChainAlreadyExistsException;
import com.dotmarketing.portlets.chains.business.ChainLinkCodeCompilationException;
import com.dotmarketing.portlets.chains.business.ChainsDependOnCodeException;
import com.dotmarketing.portlets.chains.business.DuplicateChainStateParametersException;
import com.dotmarketing.portlets.chains.business.DuplicatedChainLinkException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.portlets.chains.model.ChainLinkCode.Language;

public class ChainServletTest extends ServletTestCase {

	ChainAPI chainAPI;

	public ChainServletTest(String name) {
		super (name);
	}

	private void grabAPI() {
		chainAPI = APILocator.getChainAPI();
	}
	
	//Setup

	public void setUp () {
		grabAPI();
    	//Creating the chain to invoke
    	try {
			createChains();
		} catch (DuplicatedChainLinkException e) {
			e.printStackTrace();
		} catch (DotRuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (ChainLinkCodeCompilationException e) {
			e.printStackTrace();
		} catch (DotCacheException e) {
			e.printStackTrace();
		} catch (DuplicateChainStateParametersException e) {
			e.printStackTrace();
		} catch (ChainAlreadyExistsException e) {
			e.printStackTrace();
		}
	}

	public void tearDown () {
		grabAPI();
    	//Creating the chain to invoke
    	try {
			deleteChains();
		} catch (DuplicatedChainLinkException e) {
			e.printStackTrace();
		} catch (DotRuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (ChainLinkCodeCompilationException e) {
			e.printStackTrace();
		} catch (DotCacheException e) {
			e.printStackTrace();
		} catch (DuplicateChainStateParametersException e) {
			e.printStackTrace();
		} catch (ChainAlreadyExistsException e) {
			e.printStackTrace();
		} catch (ChainsDependOnCodeException e) {
			e.printStackTrace();
		}
	}
	
	//Tests
	
	
	//First simple test checking the invocation of the servlet works fine
	
    public void beginInvokeSimpleChainByKey(WebRequest webRequest)
    {
    	webRequest.addParameter("chainKey", "simple_chain");

    }

    @SuppressWarnings("unchecked")
	public void testInvokeSimpleChainByKey() throws ServletException, IOException, DuplicatedChainLinkException, DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, DuplicateChainStateParametersException, ChainAlreadyExistsException
    {
    	
        ChainServlet servlet = new ChainServlet();
        servlet.service(request, response);
        
		ChainControl control = (ChainControl) request.getAttribute("chains_control");
		List<String> messages = (List<String>) request.getAttribute("chains_messages");
		List<String> errors = (List<String>) request.getAttribute("chains_errors");
		Map<String, List<String>> messagesMap = (Map<String, List<String>>) request.getAttribute("chains_messages_map");
		Map<String, List<String>> errorsMap = (Map<String, List<String>>) request.getAttribute("chains_errors_map");
		
		assertNotNull("control object was not set to the request", control);
		assertNotNull("messages object was not set to the request", messages);
		assertNotNull("errors object was not set to the request", errors);
		assertNotNull("messages map object was not set to the request", messagesMap);
		assertNotNull("errors map object was not set to the request", errorsMap);

		assertNotNull("request object was not set on the control", control.getRequest());
		assertNotNull("request object was not set on the control", control.getSession());
		assertNotNull("request object was not set on the control", control.getChain());
		
    }

	public void endInvokeSimpleChainByKey(WebResponse response)
    {
    }
	
	
	//Internal private method helpers on setup the tests
    private void createChains() throws IOException, DuplicatedChainLinkException, DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, DuplicateChainStateParametersException, ChainAlreadyExistsException {
    	
    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/dotmarketing/portlets/chains/chainslinks/SimpleChainLink.code");
    	byte[] codeBytes = new byte[is.available()];
    	is.read(codeBytes);
    	String code = new String(codeBytes);
    	ChainLink chainLink = chainAPI.addChainLink("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink", code, Language.JAVA);
    	
    	List<ChainState> states = new ArrayList<ChainState> ();

    	//State 1
    	List<ChainStateParameter> parameters = new ArrayList<ChainStateParameter>();
    	ChainStateParameter parameter = new ChainStateParameter();
    	parameter.setNameValue("test", "test-value");
    	parameters.add(parameter);

    	ChainState state = new ChainState(chainLink.getCode().getId());
    	state.setParameters(parameters);
    	states.add(state);
    	
    	//State 2
    	List<ChainStateParameter> parameters2 = new ArrayList<ChainStateParameter>();
    	ChainStateParameter parameter21 = new ChainStateParameter();
    	parameter21.setNameValue("test", "test2-1-value");
    	parameters2.add(parameter21);

    	ChainStateParameter parameter22 = new ChainStateParameter();
    	parameter22.setNameValue("test2", "test2-2-value");
    	parameters2.add(parameter22);

    	ChainState state2 = new ChainState(chainLink.getCode().getId());
    	state2.setParameters(parameters2);
    	states.add(state2);

    	Chain chain = new Chain("Simple Chain", "simple_chain", "sucess", "fail");
    	chainAPI.saveChain(chain, states);
        
    	//Checking the chain and the states were saved correctly
    	chain = chainAPI.loadChainByKey("simple_chain");
    	assertNotNull(chain);
    	states = chain.getStates();
    	//Checking all states got saved
    	assertEquals(2, states.size());
    	//Checking the order of the states
    	assertEquals(1, states.get(0).getOrder());
    	//Checking the order of the states
    	assertEquals(2, states.get(1).getOrder());
    	
    	//Checking the code
    	ChainLink link = states.get(0).getChainLink();
    	assertEquals("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink", link.getCode().getClassName());
    	link = states.get(1).getChainLink();
    	assertEquals("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink", link.getCode().getClassName());
    
    	//Checking the parameters
    	List<ChainStateParameter> params = states.get(0).getParameters();
    	assertEquals(1, params.size());
    	assertEquals("test", params.get(0).getName());
    	assertEquals("test-value", params.get(0).getValue());
    	
    	params = states.get(1).getParameters();
    	assertEquals(2, params.size());
    	assertEquals("test", params.get(0).getName());
    	assertEquals("test2-1-value", params.get(0).getValue());
    	assertEquals("test2", params.get(1).getName());
    	assertEquals("test2-2-value", params.get(1).getValue());		
	}
 
    private void deleteChains() throws IOException, DuplicatedChainLinkException, DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, DuplicateChainStateParametersException, ChainAlreadyExistsException, ChainsDependOnCodeException {
    	
        Chain chain = chainAPI.loadChainByKey("simple_chain");
    	assertNotNull(chain);
    	chainAPI.deleteChain(chain);
        chain = chainAPI.loadChainByKey("simple_chain");
    	assertNull(chain);
    	
        ChainLink chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNotNull(chainLink);
        chainAPI.deleteChainLink(chainLink);
        chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNull(chainLink);
    }
}
