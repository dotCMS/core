
package com.dotmarketing.exception;

public class DeprecatedCodeException extends DotRuntimeException {
	
	private static final long serialVersionUID = 1L;

	
	public DeprecatedCodeException(String x){
		super(x);

	}
    public DeprecatedCodeException(){
      super("Deprecated code removed from dotCMS");

  }
}
