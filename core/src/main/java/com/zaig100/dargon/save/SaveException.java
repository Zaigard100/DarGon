package com.zaig100.dargon.save;

/** Сохранение не прочитать: повреждено, из будущей версии или ссылается на неизвестное. */
public final class SaveException extends Exception {

    private static final long serialVersionUID = 1L;

    public SaveException(String message) {
        super(message);
    }

    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
