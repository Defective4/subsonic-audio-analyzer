package io.github.defective4.audioanalyzer.expr;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.cli.Converter;

public class URLConverter implements Converter<URL, MalformedURLException> {

    @Override
    public URL apply(String string) throws MalformedURLException {
        if (!string.endsWith("/")) string = string + "/";
        return URI.create(string).toURL();
    }

}
