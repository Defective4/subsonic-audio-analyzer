package io.github.defective4.audioanalyzer.expr;

import static io.github.defective4.audioanalyzer.expr.NumericExpressionType.*;

import org.apache.commons.cli.Converter;

public class IntegerExpressionConverter implements Converter<NumericExpression, ExpressionConversionException> {

    @Override
    public NumericExpression apply(String string) throws ExpressionConversionException {
        String exprStr = string;
        if (exprStr.trim().length() < 1) throw new ExpressionConversionException("The expression string it too short");
        char compChar = exprStr.charAt(0);
        NumericExpressionType type = EQUAL_TO;
        if (compChar == '<' || compChar == '>' || compChar == '=') {
            type = switch (compChar) {
                case '<' -> LESS_THAN;
                case '>' -> MORE_THAN;
                case '=' -> EQUAL_TO;
                default -> EQUAL_TO;
            };
            exprStr = exprStr.substring(1);
        }
        if (exprStr.trim().length() < 1) throw new ExpressionConversionException("The expression string it too short");
        int val;
        try {
            val = Integer.parseInt(exprStr);
        } catch (NumberFormatException e) {
            throw new ExpressionConversionException("The expression %s sis invalid".formatted(string));
        }
        return new NumericExpression(type, val);
    }

}
