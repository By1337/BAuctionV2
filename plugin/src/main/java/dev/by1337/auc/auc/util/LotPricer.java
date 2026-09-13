package dev.by1337.auc.auc.util;

import dev.by1337.auc.util.number.EconomyUtil;

public class LotPricer {
    public final long centsPriceForOne;
    public final double priceForOne;
    public final double price;
    private final int count;

    public LotPricer(long centsPriceForOne, int count) {
        this.count = count;
        price = EconomyUtil.fromCents(centsPriceForOne * count);
        this.centsPriceForOne = centsPriceForOne;
        priceForOne = EconomyUtil.fromCents(centsPriceForOne);
    }

    public long centsFor(int count) {
        return centsPriceForOne * count;
    }

    public double priceFor(int count) {
        if (count == this.count) return price;
        return priceForOne * count;
    }

    public long centsForOne() {
        return centsPriceForOne;
    }

    public double priceForOne() {
        return priceForOne;
    }
}
