package com.costin.travelify.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final double RADIUS_EARTH = 6371; // Radius of the Earth in kilometers

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIUS_EARTH * c;
    }

    public static String getMonthNameAndYearForLocalDateTime(LocalDateTime timestamp) {
        String year = timestamp.format(DateTimeFormatter.ofPattern("yyyy"));
        String monthName = timestamp.format(DateTimeFormatter.ofPattern("MMMM"));

        return STR."\{monthName} \{year}";
    }

    public static Date convertFromUnixTimestampToDate(long unixTimestamp) {
        long timestampInMillis = unixTimestamp * 1000;
        return new Date(timestampInMillis);
    }

    public static Date convertFromUnixTimestampToDateUsingTimezoneIdV2(long unixTimestamp, String timezoneId) {
        long timestampInSeconds = unixTimestamp;
        Instant instant = Instant.ofEpochSecond(timestampInSeconds);

        // Convert the Unix timestamp to a Date object with the provided timezone
        return Date.from(instant.atZone(ZoneId.of(timezoneId)).toInstant());
    }

    public static Date addHoursToDate(Date date, int hoursToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hoursToAdd);
        return calendar.getTime();
    }

    public static Date addMinutesToDate(Date date, int minutesToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutesToAdd);
        return calendar.getTime();
    }

    public static String getHourFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d", hour, minute);
    }

    public static long getTomorrowsLinuxTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime tomorrowDateTime = currentDateTime.plusDays(1);
        ZoneId systemZone = ZoneId.systemDefault();

        return tomorrowDateTime.atZone(systemZone).toEpochSecond();
    }

    public static double convertFromKelvinToCelsiusDegrees(double kelvinDegrees) {
        return kelvinDegrees - 273.15;
    }

    public static LocalDate convertFromDateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDate getLocalDateFromMonthName(String monthName) {
        if(monthName == null || monthName.equals("")) {
            return null;
        }

        Month month = Month.valueOf(monthName.toUpperCase());
        return LocalDate.now().withMonth(month.getValue()).with(TemporalAdjusters.firstDayOfMonth());
    }

    public static List<LocalDate> createListOfDatesBetweenFirstDayAndLastDay(String firstDay, String lastDay)
            throws ParseException {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate firstDayDate = convertFromDateToLocalDate(convertFromDateStringToDate(firstDay));
        LocalDate lastDayDate = convertFromDateToLocalDate(convertFromDateStringToDate(lastDay));
        LocalDate currentDayDate = firstDayDate;
        while (!currentDayDate.isAfter(lastDayDate)) {
            dates.add(currentDayDate);
            currentDayDate = currentDayDate.plusDays(1);
        }
        return dates;
    }

    public static List<String> createListOfDaysNames(int numberOfDays) {
        List<String> daysNames = new ArrayList<>();
        IntStream.range(0, numberOfDays).forEach(i -> {
            String dayName = STR."Day \{i + 1}";
            daysNames.add(dayName);
        });
        return daysNames;
    }

    public static Date convertFromDateStringToDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.parse(dateString);
    }

    public static long convertFromDateStringToUnixTimestamp(String dateString) {
        try {
            Date date = convertFromDateStringToDate(dateString);
            return date.getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String joinListOfStrings(List<String> stringList, CharSequence delimiter) {
        if(stringList == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(delimiter);
        stringList.forEach(joiner::add);
        return joiner.toString();
    }

    public static String joinListOfIds(Set<Long> idsList, CharSequence delimiter) {
        if(idsList == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(delimiter);
        idsList.forEach(id -> joiner.add(String.valueOf(id)));
        return joiner.toString();
    }

    public static Set<Long> getSetOfIdsFromString(String idsString, String delimiter) {
        return Arrays.stream(idsString.split(delimiter))
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    public static int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(str1.charAt(i - 1), str2.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    public static List<String> tokenizeString(String str, String delimiter) {
        List<String> tokens = new ArrayList<>();

        if(str == null) {
            return tokens;
        }

        if(str.isEmpty()) {
            return tokens;
        }

        StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }

        return tokens;
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static double normalize(double value, double min, double max,
                                   double normalizedMin, double normalizedMax) {
        return ((value - min) / (max - min)) * (normalizedMax - normalizedMin) + normalizedMin;
    }

    public static String getDifferenceBetweenTimings(LocalDateTime firstTiming, LocalDateTime secondTiming) {
        Duration duration = Duration.between(firstTiming, secondTiming);
        long hoursDiff = duration.toHours();
        long minutesDiff = duration.toMinutes();
        long daysDiff = duration.toDays();

        if(minutesDiff < 60) {
            if(minutesDiff == 1) {
                return STR."\{minutesDiff} min ago";
            } else {
                return STR."\{minutesDiff} mins ago";
            }
        }

        if(hoursDiff < 24) {
            if(hoursDiff == 1) {
                return STR."\{hoursDiff} hour ago";
            } else {
                return STR."\{hoursDiff} hours ago";
            }
        }

        if(daysDiff == 1) {
            return STR."\{daysDiff} day ago";
        }
        return STR."\{daysDiff} days ago";
    }

    public static Date getDateObjectForHour(int hour, int minute) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime todayAtTenThirty = today.with(LocalTime.of(hour, minute));
        return Date.from(todayAtTenThirty.atZone(ZoneId.systemDefault()).toInstant());
    }

}
