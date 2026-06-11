package io.github.defective4.audioanalyzer.expr;

import java.util.Arrays;

import org.apache.commons.cli.Converter;

public class EnumConverter<T extends Enum<?>> implements Converter<T, ExpressionConversionException> {

    private final Class<T> type;

    public EnumConverter(Class<T> type) {
        this.type = type;
    }

    @Override
    public T apply(String string) throws ExpressionConversionException {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.name().equalsIgnoreCase(string)).findAny()
                .orElseThrow(() -> new ExpressionConversionException("No such element: " + string));
    }

}
