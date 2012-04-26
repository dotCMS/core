package com.dotcms.achecker;


public enum Confidence {
		
	KNOW(0),
	LIKELY(1),
	POTENTIAL(2);
	
	private int confidence;

	private Confidence(int confidence) {
		this.confidence = confidence;
	}
	
	public static Confidence make(int confidence) {
		switch (confidence) {
			case 0: return KNOW;
			case 1: return LIKELY;
			case 2: return POTENTIAL;
		}
		return null;
	}
				
}

