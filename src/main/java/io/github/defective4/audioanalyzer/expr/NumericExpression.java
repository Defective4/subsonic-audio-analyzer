package io.github.defective4.audioanalyzer.expr;

public class NumericExpression extends Number {
    private final Number number;
    private final NumericExpressionType type;

    public NumericExpression(NumericExpressionType type, Number number) {
        this.type = type;
        this.number = number;
    }

    @Override
    public double doubleValue() {
        return number.doubleValue();
    }

    @Override
    public float floatValue() {
        return number.floatValue();
    }

    public Number getNumber() {
        return number;
    }

    public NumericExpressionType getType() {
        return type;
    }

    @Override
    public int intValue() {
        return number.intValue();
    }

    @Override
    public long longValue() {
        return number.longValue();
    }

    @Override
    public String toString() {
        return "NumericExpression [number=" + number + ", type=" + type + "]";
    }

}
