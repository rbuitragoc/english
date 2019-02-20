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
    private static final String LARGE = "numbers greater or equal than one quintillion are currently not supported: %s";
    private static final String RETRY = "Do you want to test another? y/n";

    private static final int MAXIMUM_SUPPORTED_POWER_OF_TEN = 15;
    private static final String TWO = "2";

    private Properties mappingProperties;
    private Properties powersOfTenMappingProperties;

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
        powersOfTenMappingProperties = new Properties();
        try {
            powersOfTenMappingProperties.load(findFileAsResource(propertyFiles[3]));
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

    private String capitalize(String phrase) {
        return phrase.strip().isEmpty() ? phrase : phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
    }

    private String decompose(String inputString) {
        long number = Long.parseLong(inputString);
        if (number < 100) {
            Long remainder = number % 10;
            Long tens = number - remainder;
            if (remainder > 0) {
                return String.format("%s %s", asEnglishNumeral(tens.toString()), asEnglishNumeral(remainder.toString()));
            }
        } else {
            return decomposePowersOfTen(number);
        }
        return "TBD";
    }

    private String decomposePowersOfTen(Long number) {
        Long powerOfTen = 100L;
        Object powKey = null;
        Integer[] supportedPowers = {2, 3, 6, 9, 12, 15};
        for (int i = 0; i < supportedPowers.length; i = i + 1) {
            Integer pow = supportedPowers[i];
            double currentPowerOfTen = Math.pow(10, pow);
            double nextSupportedPowerOfTen =
                    Math.pow(10, pow == MAXIMUM_SUPPORTED_POWER_OF_TEN ? MAXIMUM_SUPPORTED_POWER_OF_TEN : supportedPowers[i + 1]);
            if (number >= currentPowerOfTen && number < nextSupportedPowerOfTen) {
                powKey = pow.toString();
                powerOfTen = (long) currentPowerOfTen;
                break;
            }
        }
        if (powerOfTen == null) {
            throw new IllegalArgumentException(String.format(LARGE, number.toString()));
        }
        Long remainder = number % powerOfTen;
        Long powerOfTenUnits = (number - remainder) / powerOfTen;
        String englishPowerNumeral = asEnglishNumeral(powerOfTenUnits.toString());
        Object powerScaleDenomination = powersOfTenMappingProperties.get(powKey);
        if (remainder > 0) {
            String remainderNumeral = asEnglishNumeral(remainder.toString());
            String conditionalAnd = TWO.equals(powKey.toString()) ? " and" : "";
            return String.format("%s %s%s %s", englishPowerNumeral, powerScaleDenomination, conditionalAnd, remainderNumeral);
        } else {
            return String.format("%s %s", englishPowerNumeral, powerScaleDenomination);
        }


    }
}
