package com.dotmarketing.portlets.chains.business;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.compilers.DotCompilationProblems;
import com.dotmarketing.compilers.DotJdtCompiler;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.ChainLink;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainLinkCode;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.portlets.chains.model.ChainLinkCode.Language;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainAPIImpl implements ChainAPI {

	/**
	 * This is a private class loader used internally by this class to dynamically instanciate
	 * and manage chain links 
	 * 
	 * @author davidtorresv
	 *
	 */
	private class ChainLinkClassLoader extends ClassLoader {
		
		ClassLoader cl;
		public ChainLinkClassLoader(ClassLoader contextClassloader) {
			cl = contextClassloader;
		}
		
	    public byte[] findClassBytes(String className, String currentRoot){

	        try{
	            String pathName = currentRoot +
	                File.separatorChar + className.replace('.', File.separatorChar) + ".class";
	            FileInputStream inFile = new
	                FileInputStream(pathName);
	            byte[] classBytes = new
	                byte[inFile.available()];
	            inFile.read(classBytes);
	            return classBytes;
	        }
	        catch (java.io.IOException ioEx){
	            return null;
	        }
	    }

	    @Override
		protected Class<?> findClass(String name)
        	throws ClassNotFoundException {
			
			File codeDir = new File(buildFilePath);
			if(!codeDir.exists()) {
				if(!codeDir.mkdirs())
					throw new ClassNotFoundException(name, new Exception("Couldn't create the compilation folder for links path: " + buildFilePath));
			}
			byte[] classBytes = findClassBytes(name, buildFilePath);
	        if (classBytes==null){
	        	//If the class doesn't exist on the links compilation folder then
	        	// the class loader will use the context classloader given on the constructor
	            return cl.loadClass(name);
	        }
	        else{
	            return defineClass(name, classBytes, 0, classBytes.length);
	        }
		}
				

	}

	private ClassLoader linksClassLoader;
	private String buildFilePath = System.getProperty("java.io.tmpdir") + File.separator + "_dotcms_" + 
		File.separator + "links" + File.separator + "build";
	private ChainFactory chainFactory = FactoryLocator.getChainFactory();

	public ChainAPIImpl () {
		linksClassLoader = new ChainLinkClassLoader(Thread.currentThread().getContextClassLoader());
		chainFactory = FactoryLocator.getChainFactory();
	}
	
	public ChainLink instanciateChainLink(long linkCodeId) throws DotRuntimeException, DotDataException, DotCacheException {
	
		//Looking for the chain link code using the factory
		ChainLinkCode linkCode = chainFactory.loadChainLinkCode(linkCodeId);
		if(linkCode == null)
			throw new DotRuntimeException("Link code id: " + linkCodeId + ", not found.");

		try {
			return instanciateChainLink(linkCode);
		} catch (ChainLinkCodeCompilationException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	private ChainLink instanciateChainLink(ChainLinkCode linkCode) throws ChainLinkCodeCompilationException, DotRuntimeException, DotDataException, DotCacheException {
	
		if(!Language.JAVA.equals(linkCode.getLanguage()))
			throw new ChainLinkCodeCompilationException("Only Java language is now supported", null);
		
		String className = linkCode.getClassName();
		
		//Initializing path variables
        String javaFilePath = buildFilePath +
        	File.separatorChar + className.replace('.', File.separatorChar) + ".java";
        File javaFile = new File(javaFilePath);

        String classFilePath = buildFilePath +
    		File.separatorChar + className.replace('.', File.separatorChar) + ".class";
        File classFile = new File(classFilePath);
       
        //If the compiled class file doesn't exist or the link code in the database is newer
        // then we need to recompile
        if(!classFile.exists() || new Date(classFile.lastModified()).compareTo(linkCode.getLastModifiedDate()) < 0) {
        	
        	if(javaFile.exists()) {
        		//If the class file already exists means that code already compiled is old we need to drop the
        		//actual classloader to force the class reloading
        		if (!javaFile.delete()) {
					throw new DotRuntimeException("Couldn't remove the old file to compile the new code on the link id: " + linkCode.getId());       			
        		}
            	//Need to drop the classloader to force a class reload
        		linksClassLoader = new ChainLinkClassLoader(Thread.currentThread().getContextClassLoader());
        	}
        	
        	//Creating the java file to save the java code in the file
    		try {
                File parentFolder = new File(new File(javaFilePath).getParent());
                if(!parentFolder.exists())
             	   parentFolder.mkdirs();
				if (!javaFile.createNewFile()) {
					throw new DotRuntimeException("Couldn't create the file to compile the code link id: " + linkCode.getId());       			
				}
			} catch (IOException e) {
				throw new DotRuntimeException("Couldn't create the file to compile the code link id: " + linkCode.getId());       			
			}
			
			//Saving the java code in the file
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(javaFilePath);
				pw.write(linkCode.getCode());
				pw.flush();
			} catch (IOException e) {
				throw new DotRuntimeException("Couldn't create the file to compile the code link id: " + linkCode.getId());       			
			} finally {
				if (pw != null) pw.close();
			}	
			
			//Compiling the java code 
			try {
				DotCompilationProblems problems = DotJdtCompiler.compileClass(javaFilePath, linkCode.getClassName(), buildFilePath);
				//If java code compilation problems are found then a DotRuntime Exception is thrown with the compilation errors in it
				if(problems.hasCompilationErrors()) {
					throw new ChainLinkCodeCompilationException("A compilation error has ocurred when compiling your link code", problems);
				}
			} catch (FileNotFoundException e) {
				throw new DotRuntimeException("Error ocurred trying to compile the link code." , e);
			}
			
        }
		
        ChainLink instance = null;
        try {
 			instance = (ChainLink) linksClassLoader.loadClass(linkCode.getClassName()).newInstance();
 			instance.setCode(linkCode);
		} catch (ClassNotFoundException e) {
			throw new DotRuntimeException("Unable to load the class for link code id: " + linkCode.getId() + ".", e);
		} catch (InstantiationException e) {
			throw new DotRuntimeException("Unable to load the class for link code id: " + linkCode.getId() + ".", e);
		} catch (IllegalAccessException e) {
			throw new DotRuntimeException("Unable to load the class for link code id: " + linkCode.getId() + ".", e);
		} catch (IllegalArgumentException e) {
			throw new DotRuntimeException("Unable to load the class for link code id: " + linkCode.getId() + ".", e);
		}
		
		return instance;
	}

	public List<ChainLink> findAllChainLinks() throws DotDataException, DotRuntimeException, DotCacheException {
		
		ArrayList<ChainLink> list = new ArrayList<ChainLink> ();
		List<ChainLinkCode> codes = chainFactory.findAllChainLinkCodes();
		
		for(ChainLinkCode code : codes) {
			ChainLink link =  instanciateChainLink(code.getId());
			list.add(link);
		}
		
		return list;
	}

	public List<Chain> findAllChains() throws DotDataException {
		return chainFactory.findAllChains();
	}

	public ChainLink findChainLinkByClassName(String fullyQualifiedClassName) throws DotDataException, DotRuntimeException, ChainLinkCodeCompilationException, DotCacheException {

		ChainLinkCode code = chainFactory.findChainLinkCodeByClassName(fullyQualifiedClassName);
		if(code == null)
			return null;
		return instanciateChainLink(code);

	}

	public List<ChainLink> findChainLinksByKeyword(String keyword) throws DotRuntimeException, DotDataException, DotCacheException {
		ArrayList<ChainLink> list = new ArrayList<ChainLink> ();
		List<ChainLinkCode> codes = chainFactory.findAllChainLinkCodes();
		
		for(ChainLinkCode code : codes) {
			ChainLink link =  instanciateChainLink(code.getId());
			if(link.getDescription().contains(keyword) || link.getTitle().contains(keyword) ||
					link.getCode().getClassName().contains(keyword))
				list.add(link);
		}
		
		return list;
		
	}

	public List<ChainState> getChainStates(Chain chain) throws DotDataException, DotCacheException {
		return chainFactory.loadChainStates(chain);
	}

	public Chain loadChain(long chainId) throws DotDataException, DotCacheException {
		return chainFactory.loadChain(chainId);
	}

	
	public List<ChainStateParameter> loadChainStateParameters(ChainState chainState) throws DotDataException {
		return chainFactory.findChainStateParameters(chainState);
	}

	protected void deleteChainState (ChainState s) throws DotDataException, DotCacheException {
		List<ChainStateParameter> params = chainFactory.findChainStateParameters(s);
		for(ChainStateParameter param : params) {
			chainFactory.deleteChainStateParameter(param);
		}
		chainFactory.deleteChainState(s);
	}
	
	public void saveChain(Chain chain, List<ChainState> states) throws DotDataException, DotCacheException, DuplicateChainStateParametersException, ChainAlreadyExistsException {

		Chain oldChain = loadChainByKey(chain.getKey());
		if(oldChain != null && oldChain.getId() != chain.getId())
			throw new ChainAlreadyExistsException("A chain with this same key = " + chain.getKey() + " already exists on the sytem."); 
		
		//Checking the parameters before save
		List<ChainStateParameter> duplicatedStateParameters = new ArrayList<ChainStateParameter>();
		for(ChainState state : states) {
			Set<String> paramsCheck = new HashSet<String>();
			List<ChainStateParameter> params = state.getParameters();
			for(ChainStateParameter p : params) {
				if(paramsCheck.contains(p.getName()))
					duplicatedStateParameters.add(p);
				paramsCheck.add(p.getName());
			}
			
		}
		if(duplicatedStateParameters.size() > 0)
			throw new DuplicateChainStateParametersException("For your chain: " + chain.getName() + " your are submitting duplicated link parameters",
					duplicatedStateParameters);
		
		
		boolean isNewChain = chain.getId() == 0;
		
		chainFactory.saveChain(chain);
		int order = 1;
		
		//If it's not a new chain state we need to clean up old parameters that might be now deleted
		if(!isNewChain) {
			List<ChainState> oldStates = chainFactory.findChainStates(chain);
			for(ChainState s : oldStates) {
				if(!states.contains(s)) {
					deleteChainState(s);
				}
			}
		}

		
		for(ChainState state : states) {
			
			//Saving chain state
			boolean isNewState = state.getId() == 0;
			state.setOrder(order);
			state.setChainId(chain.getId());
			chainFactory.saveChainState(state);

			//Saving the state parameters 
			List<ChainStateParameter> params = state.getParameters();
			
			//If it's not a new chain state we need to clean up old parameters that might be now deleted
			if(!isNewState) {
				List<ChainStateParameter> oldParams = state.getParameters();
				for(ChainStateParameter p : oldParams) {
					if(!params.contains(p))
						chainFactory.deleteChainStateParameter(p);
				}
			}
			for(ChainStateParameter p : params) {
				p.setChainStateId(state.getId());
				chainFactory.saveChainStateParameter(p);	
			}

			order++;
		}
		
		
		
	}

	public ChainLink addChainLink(String className, String code, Language lang) throws DuplicatedChainLinkException, DotDataException, DotRuntimeException, ChainLinkCodeCompilationException, DotCacheException {
		return saveChainLink(0, className, code, lang);
	}
	
	public ChainLink saveChainLink(long chainLinkId, String className, String code, Language lang) throws DuplicatedChainLinkException, ChainLinkCodeCompilationException, DotDataException, DotRuntimeException, DotCacheException {

		boolean isNew = false;
		if(chainLinkId == 0)
			isNew = true;
		
		ChainLink oldChainLink = findChainLinkByClassName(className);
		if(oldChainLink != null && isNew)
			throw new DuplicatedChainLinkException ("Another chain link already exist in the system with the same class name.");
		else if (!isNew && oldChainLink.getCode().getId() != chainLinkId)
			throw new DuplicatedChainLinkException ("Another chain link already exist in the system with the same class name.");
		
		ChainLinkCode lcode;
		if(!isNew) {
			lcode = chainFactory.loadChainLinkCode(chainLinkId);
			lcode.setClassName(className);
			lcode.setCode(code);
			lcode.setLanguage(lang);
		} else {
			lcode = new ChainLinkCode (className, code, lang);
		}

		
		ChainLink ret = instanciateChainLink(lcode);
		
		chainFactory.saveChainLinkCode(lcode);
		
		return ret;
	}

	public Chain loadChainByKey(String chainKey) throws DotDataException, DotCacheException {
		return chainFactory.loadChainByKey(chainKey);

	}

	public void deleteChain(Chain chain) throws DotDataException, DotCacheException {
		List<ChainState> states = chainFactory.findChainStates(chain);
		for(ChainState s : states) {
			deleteChainState(s);
		}
		chainFactory.deleteChain(chain);
	}

	public void deleteChainLink(ChainLink chainLink) throws ChainsDependOnCodeException, DotDataException, DotCacheException {
		List<Chain> chains = chainFactory.findDependentChains(chainLink.getCode());
		if(chains.size() > 0)
			throw new ChainsDependOnCodeException(chains);
		chainFactory.deleteChainLinkCode(chainLink.getCode());
	}


}
