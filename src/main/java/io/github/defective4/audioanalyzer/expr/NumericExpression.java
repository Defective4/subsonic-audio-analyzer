package io.github.defective4.audioanalyzer.expr;

public class NumericExpression {
    private final Number number;
    private final NumericExpressionType type;

    protected NumericExpression(NumericExpressionType type, Number number) {
        this.type = type;
        this.number = number;
    }

    public Number getNumber() {
        return number;
    }

    public NumericExpressionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "NumericExpression [number=" + number + ", type=" + type + "]";
    }

}
