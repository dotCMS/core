define("dijit/form/VerticalRuleLabels", ["dojo", "dijit", "dijit/form/HorizontalRuleLabels"], function(dojo, dijit) {

dojo.declare("dijit.form.VerticalRuleLabels", dijit.form.HorizontalRuleLabels,
{
	// summary:
	//		Labels for the `dijit.form.VerticalSlider`

	templateString: '<div class="dijitRuleContainer dijitRuleContainerV dijitRuleLabelsContainer dijitRuleLabelsContainerV"></div>',

	_positionPrefix: '<div class="dijitRuleLabelContainer dijitRuleLabelContainerV" style="top:',
	_labelPrefix: '"><span class="dijitRuleLabel dijitRuleLabelV">',

	_calcPosition: function(pos){
		// Overrides HorizontalRuleLabel._calcPosition()
		return 100-pos;
	},

	// needed to prevent labels from being reversed in RTL mode
	_isHorizontal: false
});


return dijit.form.VerticalRuleLabels;
});
