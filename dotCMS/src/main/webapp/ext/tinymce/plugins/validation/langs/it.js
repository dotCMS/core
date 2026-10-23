// Bug fix while porting to TinyMCE 8: this file previously registered under `'en.validation'`
// (copy-paste from en.js) instead of `'it.validation'`, so its translations were never actually
// reachable for an Italian-configured editor — carried forward unchanged below, just under the
// correct language code and TinyMCE 8's flat key shape (see en.js).
tinymce.addI18n('it', {
	'Check Accessibility': 'Verifica Accessibilità',
	'GuideLines': 'Linee Guida',
	'Description': 'Descrizione',
	'Row/Column': 'Riga/Colonna',
	'Error Type': 'Errore',
	'Check ID': 'ID Controllo',
	'Accessibility Review  - Guideline: ': 'Revisione  - Linee Guida: ',
	' Errors Found!': ' Errori Trovati!',
	'No errors found!': 'Nessuno errore trovato!',
	'Validation complete! ': 'Validazione completata! '
});
