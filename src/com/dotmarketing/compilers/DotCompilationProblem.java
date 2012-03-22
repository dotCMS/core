package com.dotmarketing.compilers;


public class DotCompilationProblem {
	
	private String fileName;
	private String message;
	private int lineNumber;
	private boolean isError;
	private boolean isWarning;
	
	public DotCompilationProblem (String fileName, String message, int lineNumber, boolean isError) {
		this.fileName = fileName;
		this.message = message;
		this.lineNumber = lineNumber;
		this.isError = isError;
		this.isWarning = !isError;
	}
	
	public String getFileName() {
		return fileName;
	}
	public String getMessage() {
		return message;
	}
	public int getLineNumber() {
		return lineNumber;
	}
	public boolean isError() {
		return isError;
	}
	public boolean isWarning() {
		return isWarning;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DotCompilationProblem { fileName = ");
		sb.append(fileName);
		sb.append(", message = ");
		sb.append(message);
		sb.append(", lineNumber = ");
		sb.append(lineNumber);
		sb.append(", isError = ");
		sb.append(isError);
		sb.append(", isWarning = ");
		sb.append(isWarning);
		sb.append(" }");
		return sb.toString();
		
	}
	
	
	
	
	
}
