package com.dotmarketing.portlets.chains.business;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.ChainLink;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.portlets.chains.model.ChainLinkCode.Language;

public class ChainAPITest extends ServletTestCase {

	ChainAPI chainAPI;
	
	public ChainAPITest(String name) {
		super (name);
	}

	private void grabAPI() {
		chainAPI = APILocator.getChainAPI();
	}

	public void setUp () {
		grabAPI();
	}
	
    public void testSaveChain() throws DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, IOException, DuplicateChainStateParametersException, ChainAlreadyExistsException {
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
    
    public void testSaveInvalidParameterChain () throws DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, ChainsDependOnCodeException, IOException, ChainAlreadyExistsException {

    	ChainLink chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNotNull(chainLink);
    	
    	//trying to saved duplicated parameters
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

    	//duplicate parameter
    	ChainStateParameter parameter22 = new ChainStateParameter();
    	parameter22.setNameValue("test", "test2-2-value");
    	parameters2.add(parameter22);

    	ChainState state2 = new ChainState(chainLink.getCode().getId());
    	state2.setParameters(parameters2);
    	states.add(state2);

    	Chain chain = new Chain("Simple Chain 2", "simple_chain2", "sucess", "fail");
    	try {
			chainAPI.saveChain(chain, states);
			assertTrue("Let me save duplicated parameters!!", false);
		} catch (DuplicateChainStateParametersException e) {
			
		}
        
    }
    
    public void testSaveAChainAlreadyExists () throws DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, ChainsDependOnCodeException, IOException, DuplicateChainStateParametersException {

    	ChainLink chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNotNull(chainLink);
    	
    	//trying to saved duplicated parameters
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

    	//duplicate parameter
    	ChainStateParameter parameter22 = new ChainStateParameter();
    	parameter22.setNameValue("test", "test2-2-value");
    	parameters2.add(parameter22);

    	ChainState state2 = new ChainState(chainLink.getCode().getId());
    	state2.setParameters(parameters2);
    	states.add(state2);

    	Chain chain = new Chain("Simple Chain", "simple_chain", "sucess", "fail");
    	try {
			chainAPI.saveChain(chain, states);
			assertTrue("Let me save duplicated chain!!", false);
		} catch (ChainAlreadyExistsException e) {

		}
        
    }
    
    public void testSaveALinkCodeAlreadyExists () throws DotRuntimeException, ChainLinkCodeCompilationException, DotCacheException, ChainsDependOnCodeException, IOException, DuplicateChainStateParametersException, DotDataException {

    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/dotmarketing/portlets/chains/chainslinks/SimpleChainLink.code");
    	byte[] codeBytes = new byte[is.available()];
    	is.read(codeBytes);
    	String code = new String(codeBytes);
    	try {
			chainAPI.addChainLink("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink", code, Language.JAVA);
			assertTrue("Let me save duplicated chain link!!", false);
		} catch (DuplicatedChainLinkException e) {
			
		} 

        
    }
    
    
    public void testSaveALinkCodeDoesntCompile () throws DuplicatedChainLinkException, DotRuntimeException, DotDataException, DotCacheException, IOException  {

    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/dotmarketing/portlets/chains/chainslinks/SimpleChainLink2.code");
    	byte[] codeBytes = new byte[is.available()];
    	is.read(codeBytes);
    	String code = new String(codeBytes);
    	try {
			chainAPI.addChainLink("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLinkInvalid", code, Language.JAVA);
			assertTrue("It let me save not compilable code!!", false);
		} catch (ChainLinkCodeCompilationException e) {
			//Cool it didn't save
		} 
		is.close();
		
    	is = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/dotmarketing/portlets/chains/chainslinks/SimpleChainLinkDoesntCompile.code");
    	codeBytes = new byte[is.available()];
    	is.read(codeBytes);
    	code = new String(codeBytes);
    	try {
			chainAPI.addChainLink("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLinkDoesnCompile", code, Language.JAVA);
			assertTrue("It let me save not compilable code!!", false);
		} catch (ChainLinkCodeCompilationException e) {
			//Perfect it didn't save it
		} 
		is.close();
        
    }
    public void testDeleteChains () throws DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, ChainsDependOnCodeException {

        Chain chain = chainAPI.loadChainByKey("simple_chain");
    	assertNotNull(chain);
    	chainAPI.deleteChain(chain);
        chain = chainAPI.loadChainByKey("simple_chain");
    	assertNull(chain);

    }
    
    public void testDeleteChainCode () throws DotRuntimeException, DotDataException, ChainLinkCodeCompilationException, DotCacheException, ChainsDependOnCodeException {

        ChainLink chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNotNull(chainLink);
        chainAPI.deleteChainLink(chainLink);
        chainLink = chainAPI.findChainLinkByClassName("com.dotmarketing.portlets.chains.chainslinks.SimpleChainLink");
    	assertNull(chainLink);
    	
    }
    

}
