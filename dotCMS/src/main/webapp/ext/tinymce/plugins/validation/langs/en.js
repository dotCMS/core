// TinyMCE 8's addI18n takes a flat { englishSourceString: translatedString } map keyed by
// exact `editor.translate()` input, not the old namespaced `addI18n('en.validation', {...})`
// (a lookup shape TinyMCE 8's i18n manager no longer supports).
tinymce.addI18n('en', {
	'Check Accessibility': 'Check Accessibility',
	'GuideLines': 'GuideLines',
	'Description': 'Description',
	'Row/Column': 'Row/Column',
	'Error Type': 'Error Type',
	'Check ID': 'Check ID',
	'Accessibility Review  - Guideline: ': 'Accessibility Review  - Guideline: ',
	' Errors Found!': ' Errors Found!',
	'No errors found!': 'No errors found!',
	'Validation complete! ': 'Validation complete! '
});
