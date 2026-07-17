package com.by1337.auc.user;

public class UserMails {
    public static final String LOT_SOLD = "native:lot_sold|";
    public static final String ECO_DEPOSIT_CENTS = "native:eco_deposit_cents|";

    public static String makeLotSold(long logUid) {
        return LOT_SOLD + logUid;
    }

    public static String makeDepositCents(long cents) {
        return ECO_DEPOSIT_CENTS + cents;
    }

    public static boolean isLotSold(String s) {
        return s.startsWith(LOT_SOLD);
    }

    public static boolean isDepositCents(String s) {
        return s.startsWith(ECO_DEPOSIT_CENTS);
    }

    public static long getLong(String s) {
        var arr = s.split("\\|", 2);
        if (arr.length != 2) throw new IllegalArgumentException("Bad mail " + s);
        try {
            return Long.parseLong(arr[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad mail " + s);
        }
    }
    public static int getInt(String s) {
        var arr = s.split("\\|", 2);
        if (arr.length != 2) throw new IllegalArgumentException("Bad mail " + s);
        try {
            return Integer.parseInt(arr[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad mail " + s);
        }
    }
}
