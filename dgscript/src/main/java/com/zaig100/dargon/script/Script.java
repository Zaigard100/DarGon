package com.zaig100.dargon.script;

/**
 * Скомпилированный скрипт. Выполняется через {@link ScriptEngine#run}; можно запускать повторно.
 */
public final class Script {

    private final String file;
    final Stmt.Block program;

    Script(String file, Stmt.Block program) {
        this.file = file;
        this.program = program;
    }

    /** Имя файла, указанное при компиляции. */
    public String file() {
        return file;
    }
}
