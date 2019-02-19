package com.rb.english;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class Main {

    private static final String HALT = "Can't load property file located at \"%s\". Halting (%s).";
    private static final String PROMPT = "Please enter a number and I'll write it as en English Numeral for you:";
    private static final String CONFIRMATION = "%s (Read as %d): \"%s\"";
    private static final String WRONG = "%s Can't parse this number! (%s)";
    private static final String RETRY = "Do you want to test another? y/n";

    private Properties mappingProperties;

    public static void main(String[] args) throws Exception {
        Main runner = new Main();
        runner.loadProperties();
        boolean retry;
        do {
            retry = runner.captureAndTranslate();
        } while (retry);
    }

    private boolean captureAndTranslate() throws IOException {
        System.out.println(PROMPT);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
        String inputString = reader.readLine();

        Long inputNumber;
        try {
            inputNumber = Long.parseLong(inputString.strip());
            String normalizedString = inputNumber.toString(); // normalizing
            String englishNumeral = capitalize(asEnglishNumeral(normalizedString));

            System.out.println(String.format(CONFIRMATION, "^".repeat(inputString.length()), inputNumber, englishNumeral));
        } catch (NumberFormatException e) {
            System.err.println(String.format(WRONG, "^".repeat(inputString.length()), e.getMessage()));
        }
        System.out.println(RETRY);

        return reader.readLine().matches("[y|Y]");
    }

    private void loadProperties() {
        mappingProperties = new Properties();
        Stream.of("digits.properties", "teens.properties", "tens.properties").forEach(p -> {
            try {
                mappingProperties.load(findFileAsResource(p));
            } catch (IOException e) {
                System.err.println(String.format(HALT, p, e.getLocalizedMessage()));
            }
        });
    }

    private FileReader findFileAsResource(String path) throws FileNotFoundException {
        return new FileReader(
                new File(Optional.ofNullable(getClass().getResource(path))
                        .orElseThrow(() -> new FileNotFoundException(path)).getPath()));
    }

    private String asEnglishNumeral(String inputString) {
        return mappingProperties.containsKey(inputString) ?
                String.format("%s", mappingProperties.get(inputString)) : decompose(inputString);
    }

    private String capitalize(String phrase) {
        return phrase.strip().isEmpty() ? phrase : phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
    }

    private String decompose(String inputString) {
        Long number = Long.parseLong(inputString);
        Long remainder = number % 10;
        Long tens = number - remainder;
        if (remainder > 0) {
            return String.format("%s %s", asEnglishNumeral(tens.toString()), asEnglishNumeral(remainder.toString()));
        }
        return "TBD";
    }

}
