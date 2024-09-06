package com.dotcms.cli.command.language;

import picocli.CommandLine.Parameters;

public class LanguagePullMixin {

    @Parameters(index = "0", arity = "0..1", paramLabel = "idOrIso", description = "Language Id or ISO Code.")
    String languageIdOrIso;

}
