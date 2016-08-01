package com.dotmarketing.util;

import java.io.IOException;
import java.io.Serializable;

public class MyObject implements Serializable {

		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		public String name;

	    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
	        in.defaultReadObject();
	        
	        this.name = this.name+"!";
	        

	    }
	
}
