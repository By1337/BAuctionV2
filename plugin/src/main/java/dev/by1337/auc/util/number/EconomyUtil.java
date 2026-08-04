package dev.by1337.auc.util.number;

public class EconomyUtil {
    public static long toCents(double amount) {
        return Math.round(amount * 100D);
    }
    public static double fromCents(long amount) {
        return amount / 100D;
    }
}
