package dev.ytstream.stream;

/** 利用者にそのまま表示できる日本語メッセージを持つ例外。 */
public final class StreamException extends Exception {
    private static final long serialVersionUID = 1L;

    public StreamException(String message) {
        super(message);
    }
}
