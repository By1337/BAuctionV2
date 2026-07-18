package dev.by1337.auc.util;

public class TimeUtil {

    public static String getFormat(long time) {
        return getFormat(time, false);
    }

    private static String getFormat(long time, boolean prefix) {
        long currentTimeMillis = System.currentTimeMillis();
        long timeDifferenceMillis = currentTimeMillis - time;
        timeDifferenceMillis = timeDifferenceMillis < 0 ? -timeDifferenceMillis : timeDifferenceMillis;

        long seconds = timeDifferenceMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = months / 12;

        if (timeDifferenceMillis > 999) {
            minutes %= 60;
            seconds %= 60;
            hours %= 24;
            days %= 30;
            months %= 12;
            String formattedTime = formatTime(years, months, days, hours, minutes, seconds);
            if (!prefix) return formattedTime;
            if (time < currentTimeMillis) {
                return formattedTime + " назад";
            } else {
                return "через " + formattedTime;
            }

        } else {
            return "<lang:bauctionv2.time.format.seconds:0>";
        }
    }

    private static String formatTime(long years, long months, long days, long hours, long minutes, long seconds) {
        if (years != 0) {
            if (months != 0) return String.format("<lang:bauctionv2.time.format.years.months:%d:%d>", years, months);
            return String.format("<lang:bauctionv2.time.format.years:%d>", years);
        } else if (months != 0) {
            if (days != 0) return String.format("<lang:bauctionv2.time.format.months.days:%d:%d>", months, days);
            return String.format("<lang:bauctionv2.time.format.months:%d>", months);
        } else if (days != 0) {
            if (hours != 0) return String.format("<lang:bauctionv2.time.format.days.hours:%d:%d>", days, hours);
            return String.format("<lang:bauctionv2.time.format.days:%d>", days);
        } else if (hours != 0) {
            if (minutes != 0) return String.format("<lang:bauctionv2.time.format.hours.minutes:%d:%d>", hours, minutes);
            return String.format("<lang:bauctionv2.time.format.hours:%d>", hours);
        } else if (minutes != 0) {
            if (seconds != 0) return String.format("<lang:bauctionv2.time.format.minutes.seconds:%d:%d>", minutes, seconds);
            return String.format("<lang:bauctionv2.time.format.minutes:%d>", minutes);
        }
        return String.format("<lang:bauctionv2.time.format.seconds:%d>", seconds);
    }


    /*private static String formatTime_OLD(long years, long months, long days, long hours, long minutes, long seconds) {
        if (years != 0) {
            if (months != 0) return String.format("%s г. %s мес.", years, months);
            return String.format("%s г.", years);
        } else if (months != 0) {
            if (days != 0) return String.format("%s мес. %s дн.", months, days);
            return String.format("%s мес.", months);
        } else if (days != 0) {
            if (hours != 0) return String.format("%s дн. %s ч.", days, hours);
            return String.format("%s дн.", days);
        } else if (hours != 0) {
            if (minutes != 0) return String.format("%s ч. %s мин.", hours, minutes);
            return String.format("%s ч.", hours);
        } else if (minutes != 0) {
            if (seconds != 0) return String.format("%s мин. %s сек.", minutes, seconds);
            return String.format("%s мин.", minutes);
        }
        return seconds + " сек.";
    }*/


}
