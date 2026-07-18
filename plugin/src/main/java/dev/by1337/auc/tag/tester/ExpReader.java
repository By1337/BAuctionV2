package dev.by1337.auc.tag.tester;

import dev.by1337.core.util.math.FastExpressionParser;

public final class ExpReader {
    private int ridx;
    private final String exp;
    private final int size;

    public ExpReader(String exp) {
        this.exp = exp;
        size = exp.length();
    }


    public char next() {
        if (ridx < size) {
            return exp.charAt(ridx++);
        }
        ridx++;
        return '\0';
    }

    public boolean hasNext() {
        return ridx < size;
    }

    public char last() {
        if (size == 0) return '\0';
        return exp.charAt(Math.max(0, Math.min(Math.max(0, ridx), size - 1)));
    }

    public void back() {
        ridx--;
    }

    public int ridx() {
        return ridx;
    }

    public void ridx(int ridx) {
        this.ridx = ridx;
    }

    private String context() {

        return "\n" + exp + "\n" +
                " ".repeat(Math.max(0, ridx)) +
                "^ ridx=" + ridx + ", length=" + exp.length() + "\n";
    }

    public FastExpressionParser.MathFormatException badNumber(NumberFormatException e) throws FastExpressionParser.MathFormatException {
        return new FastExpressionParser.MathFormatException("Bad number: " + e.getMessage() + "\n" + context());
    }

    public FastExpressionParser.MathFormatException expected(String s) throws FastExpressionParser.MathFormatException {
        return new FastExpressionParser.MathFormatException("Expected " + s + " but got '" + last() + "' at " + ridx + "\n" + context());
    }

    public FastExpressionParser.MathFormatException expected(char c) throws FastExpressionParser.MathFormatException {
        return new FastExpressionParser.MathFormatException("Expected '" + c + "' but got '" + last() + "' at " + ridx + "\n" + context());
    }

}
