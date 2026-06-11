package io.github.defective4.audioanalyzer.expr;

public class IntegerExpression extends NumericExpression {

    public IntegerExpression(NumericExpressionType type, int number) {
        super(type, number);
    }

    public int getInt() {
        return getNumber().intValue();
    }
}
