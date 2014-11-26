package org.tinymce.spellchecker;

/**
* SpellcheckMethod.java
* Created on 5/13/13 by Andrey Chorniy
* Copyright 2013, itMD, LLC. All Rights Reserved.
*/
enum SpellcheckMethod {
    checkWords,
    getSuggestions,
    addToDictionary,
    spellcheck //this method is passed in TinyMCE 4.0
}