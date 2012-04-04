if(!dojo._hasResource["dijit.tests.form.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.tests.form.module"] = true;
dojo.provide("dijit.tests.form.module");

try{
	var userArgs = window.location.search.replace(/[\?&](dojoUrl|testUrl|testModule)=[^&]*/g,"").replace(/^&/,"?");

	doh.registerUrl("dijit.tests.form.robot.Button_mouse", dojo.moduleUrl("dijit","tests/form/robot/Button_mouse.html"+userArgs), 99999999);
	doh.registerUrl("dijit.tests.form.robot.Button_a11y", dojo.moduleUrl("dijit","tests/form/robot/Button_a11y.html"+userArgs), 99999999);

	doh.registerUrl("dijit.tests.form.robot.test_validate", dojo.moduleUrl("dijit","tests/form/robot/test_validate.html"+userArgs), 99999999);

	doh.registerUrl("dijit.tests.form.robot.DateTextBox", dojo.moduleUrl("dijit","tests/form/robot/DateTextBox.html"+userArgs), 99999999);

	doh.registerUrl("dijit.tests.form.Form", dojo.moduleUrl("dijit", "tests/form/Form.html"), 99999999);
	doh.registerUrl("dijit.tests.form.robot.Form", dojo.moduleUrl("dijit","tests/form/robot/Form.html"+userArgs), 99999999);

	doh.registerUrl("dijit.tests.form.Select", dojo.moduleUrl("dijit", "tests/form/test_Select.html"));

	doh.registerUrl("dijit.tests.form.robot.ComboBox_mouse", dojo.moduleUrl("dijit","tests/form/robot/_autoComplete_mouse.html"+(userArgs+"&testWidget=dijit.form.ComboBox").replace(/^&/,"?")), 99999999);
	doh.registerUrl("dijit.tests.form.robot.ComboBox_a11y", dojo.moduleUrl("dijit","tests/form/robot/_autoComplete_a11y.html"+(userArgs+"&testWidget=dijit.form.ComboBox").replace(/^&/,"?")), 99999999);

	doh.registerUrl("dijit.tests.form.robot.FilteringSelect_mouse", dojo.moduleUrl("dijit","tests/form/robot/_autoComplete_mouse.html"+(userArgs+"&testWidget=dijit.form.FilteringSelect").replace(/^&/,"?")), 99999999);
	doh.registerUrl("dijit.tests.form.robot.FilteringSelect_a11y", dojo.moduleUrl("dijit","tests/form/robot/_autoComplete_a11y.html"+(userArgs+"&testWidget=dijit.form.FilteringSelect").replace(/^&/,"?")), 99999999);

	doh.registerUrl("dijit.tests.form.robot.Slider_mouse", dojo.moduleUrl("dijit","tests/form/robot/Slider_mouse.html"+userArgs), 99999999);
	doh.registerUrl("dijit.tests.form.robot.Slider_a11y", dojo.moduleUrl("dijit","tests/form/robot/Slider_a11y.html"+userArgs), 99999999);

	doh.registerUrl("dijit.tests.form.robot.Spinner_mouse", dojo.moduleUrl("dijit","tests/form/robot/Spinner_mouse.html"+userArgs), 99999999);
	doh.registerUrl("dijit.tests.form.robot.Spinner_a11y", dojo.moduleUrl("dijit","tests/form/robot/Spinner_a11y.html"+userArgs), 99999999);
}catch(e){
	doh.debug(e);
}

}
