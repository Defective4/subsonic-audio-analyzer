package io.github.defective4.audioanalyzer.format;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

public class MarkdownTableWriter extends PrintWriter {
    private final String[] columns;

    public MarkdownTableWriter(Writer writer, String[] columns) {
        super(writer);
        this.columns = columns;
    }

    public void writeLines(String[][] values) {
        if (values == null || Arrays.stream(values).anyMatch(v -> v.length != columns.length))
            throw new IllegalArgumentException("Values' array length is not the same as the number of counts.");

        int[] columnWidths = new int[columns.length];

        for (int i = 0; i < columnWidths.length; i++) {
            int index = i;
            columnWidths[i] = Arrays.stream(values).mapToInt(arr -> arr[index].length()).max().getAsInt();
            columnWidths[i] = Math.max(columnWidths[i], columns[i].length());
        }

        for (int i = 0; i < columns.length; i++) {
            print("| ");
            print(formatCell(columns[i], columnWidths[i]));
            print(' ');
        }
        println('|');

        for (int i = 0; i < columns.length; i++) {
            print("| ");
            print(formatCell(null, columnWidths[i]));
            print(' ');
        }
        println('|');

        for (String[] row : values) {
            for (int i = 0; i < row.length; i++) {
                String val = row[i];
                print("| ");
                print(formatCell(val, columnWidths[i]));
                print(' ');
            }
            println('|');
        }
    }

    private static String formatCell(String content, int width) {
        StringBuilder builder = new StringBuilder(width);
        if (content == null) {
            for (int i = 0; i < width; i++) builder.append('-');
        } else {
            builder.append(content);
            while (builder.length() < builder.capacity()) builder.append(' ');
        }
        return builder.toString();
    }
}
