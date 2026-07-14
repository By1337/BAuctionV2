package com.by1337.auc.util.number;

import java.text.DecimalFormat;

public class NumberFormatter {
   private static final DecimalFormat df = new DecimalFormat("#,###");
   private static final DecimalFormat simple = new DecimalFormat("#");

   public static String format(double d){
       return d <= 1000 ? simple.format(d) : df.format(d);
   }
}
