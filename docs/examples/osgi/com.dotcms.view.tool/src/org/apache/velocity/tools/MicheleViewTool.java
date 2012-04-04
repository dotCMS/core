package org.apache.velocity.tools;

import java.util.Random;

import org.apache.velocity.tools.view.tools.ViewTool;

public class MicheleViewTool implements ViewTool {

	@Override
	public void init(Object initData) {
	}

	public String getHelloMessage() {
		return "Ciao";
	}

	public String getHelloMessage(String name) {
		return "Hello " + name;
	}

}
