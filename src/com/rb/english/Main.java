package com.rb.english;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class Main {

    private static final String HALT = "Can't load property file located at \"%s\". Halting (%s).";
    private static final String PROMPT = "Please enter a number and I'll write it as en English Numeral for you:";
    private static final String CONFIRMATION = "%s (Read as %d): \"%s\"";
    private static final String WRONG = "%s Can't write English Numeral for this number: (%s)";
    private static final String LARGE = "numbers greater or equal than one %s are currently not supported: %s";
    private static final String RETRY = "Do you want to try another number? y/n";

    // Limits of operational capacity
    private static final Integer MAXIMUM_SUPPORTED_POWER_OF_TEN = 15;
    private static final Integer FIRST_UNSUPPORTED_POWER_OF_TEN = 18;
    // Lowest powers
    private static final Integer TENS = 1;
    private static final String TENS_KEY = TENS.toString();
    private static final Integer HUNDREDS = 2;
    private static final String HUNDREDS_KEY = HUNDREDS.toString();

    private Properties mappingProperties;
    private Properties powersOfTenProperties;

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
        } catch (IllegalArgumentException e) {
            System.err.println(String.format(WRONG, "^".repeat(inputString.length()), e.getMessage()));
        }
        System.out.println(RETRY);

        return reader.readLine().matches("[y|Y]");
    }

    private String capitalize(String phrase) {
        return phrase.strip().isEmpty() ? phrase : phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
    }

    private void loadProperties() {
        mappingProperties = new Properties();
        String[] propertyFiles = {
            "digits.properties",
            "teens.properties",
            "tens.properties",
            "powersoften.properties"
        };
        Stream.of(propertyFiles).filter(n -> !n.contains("pow")).forEach(p -> {
            try {
                mappingProperties.load(findFileAsResource(p));
            } catch (IOException e) {
                System.err.println(String.format(HALT, p, e.getLocalizedMessage()));
            }
        });
        powersOfTenProperties = new Properties();
        try {
            powersOfTenProperties.load(findFileAsResource(propertyFiles[3]));
        } catch (IOException e) {
            System.err.println(String.format(HALT, propertyFiles[3], e.getLocalizedMessage()));
        }
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

    private String decompose(String inputString) {
        long number = Long.parseLong(inputString);

        Long powerOfTen = null;
        String powKey = null;
        for (Integer pow = TENS; pow < FIRST_UNSUPPORTED_POWER_OF_TEN; pow = nextPow(pow)) {
            double currentPowerOfTen = Math.pow(10, pow);
            double nextPowerOfTen = Math.pow(10, nextPowerOfTen(pow));
            if (number >= currentPowerOfTen && number < nextPowerOfTen) {
                powKey = pow.toString();
                powerOfTen = (long) currentPowerOfTen;
                break;
            }
        }
        if (powerOfTen == null) {
            Object firstUnsupportedDenominator = powersOfTenProperties.get(FIRST_UNSUPPORTED_POWER_OF_TEN.toString());
            throw new IllegalArgumentException(String.format(LARGE, firstUnsupportedDenominator, number));
        }
        Long remainder = number % powerOfTen;
        Long powerOfTenUnits = TENS_KEY.equals(powKey) ? number - remainder : (number - remainder) / powerOfTen;
        String englishPowerNumeral = asEnglishNumeral(powerOfTenUnits.toString());
        Object powerScaleDenomination = TENS_KEY.equals(powKey) ? "" : " " + powersOfTenProperties.get(powKey);
        if (remainder > 0) {
            String conditionalAnd = HUNDREDS_KEY.equals(powKey) ? " and" : "";
            String remainderNumeral = asEnglishNumeral(remainder.toString());
            return String.format("%s%s%s %s", englishPowerNumeral, powerScaleDenomination, conditionalAnd, remainderNumeral);
        } else {
            return String.format("%s%s", englishPowerNumeral, powerScaleDenomination);
        }
    }

    private int nextPow(Integer pow) {
        return TENS.equals(pow) || HUNDREDS.equals(pow) ? pow + 1 : pow + 3;
    }

    private double nextPowerOfTen(Integer pow) {
        return MAXIMUM_SUPPORTED_POWER_OF_TEN.equals(pow) ? FIRST_UNSUPPORTED_POWER_OF_TEN : nextPow(pow);
    }

}