package io.github.defective4.audioanalyzer;

import io.github.defective4.audioanalyzer.ml.TensorflowAnalyzer;

public class Main {
    public static void main(String[] args) {
        try {
            TensorflowAnalyzer analyzer = new TensorflowAnalyzer("http://127.0.0.1:8000/analyze");
            System.out.println(analyzer.requestAnalysis("/tmp/Fish.mp3"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
