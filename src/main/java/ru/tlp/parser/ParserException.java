package ru.tlp.parser;

public class ParserException extends RuntimeException {
    private final int position;
    private final int length;

    public ParserException(
            String message,
            int position,
            int length
    ) {
        super(message);

        this.position = position;
        this.length = length;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }
}
