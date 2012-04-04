if(!dojo._hasResource["dijit.tests._base.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.tests._base.module"] = true;
dojo.provide("dijit.tests._base.module");

try{
	var userArgs = window.location.search.replace(/[\?&](dojoUrl|testUrl|testModule)=[^&]*/g,"").replace(/^&/,"?"),
		test_robot = true;

	doh.registerUrl("dijit.tests._base.manager", dojo.moduleUrl("dijit", "tests/_base/manager.html"));
	doh.registerUrl("dijit.tests._base.tabindex", dojo.moduleUrl("dijit", "tests/_base/tabindex.html"));
	doh.registerUrl("dijit.tests._base.sniffQuirks", dojo.moduleUrl("dijit", "tests/_base/sniffQuirks.html"));
	doh.registerUrl("dijit.tests._base.sniffStandards", dojo.moduleUrl("dijit", "tests/_base/sniffStandards.html"));
	doh.registerUrl("dijit.tests._base.viewport", dojo.moduleUrl("dijit", "tests/_base/viewport.html"));
	doh.registerUrl("dijit.tests._base.viewportQuirks", dojo.moduleUrl("dijit", "tests/_base/viewportQuirks.html"));
	doh.registerUrl("dijit.tests._base.scroll", dojo.moduleUrl("dijit", "tests/_base/test_scroll.html"), 99999999);
	doh.registerUrl("dijit.tests._base.wai", dojo.moduleUrl("dijit", "tests/_base/wai.html"));
	doh.registerUrl("dijit.tests._base.place", dojo.moduleUrl("dijit", "tests/_base/place.html"));
	if(test_robot){
		doh.registerUrl("dijit.tests._base.robot.FocusManager", dojo.moduleUrl("dijit","tests/_base/robot/FocusManager.html"), 99999999);
		doh.registerUrl("dijit.tests._base.robot.focus_mouse", dojo.moduleUrl("dijit","tests/_base/robot/focus_mouse.html"), 99999999);
	}

}catch(e){
	doh.debug(e);
}

}
