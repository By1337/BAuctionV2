package com.by1337.auc.command.args;

import com.by1337.auc.util.number.NumberFormatter;
import dev.by1337.cmd.*;
import dev.by1337.core.util.math.FastExpressionParser;
import org.bukkit.entity.Player;

public class NumberArgument<C> extends Argument<C, Number> {

    public NumberArgument(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) return;
        str = str.replace(",", "").replace(".", "");
        try {
            out.put(name, (Number) FastExpressionParser.parse(str));
        } catch (FastExpressionParser.MathFormatException ignored) {
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        if (str.isBlank()) {
            suggestions.suggest("10");
            return;
        }
        str = str.replace(",", "").replace(".", "");
        if (str.endsWith("*")) {

            if (ctx instanceof Player pl) {
                var item = pl.getInventory().getItemInMainHand();
                int x = item.getAmount();
                if (x > 1)
                    suggestions.suggest(str = (str + x));
            }
        }
        try {
            double d = FastExpressionParser.parse(str);
            args.put(name, d);
            suggestions.suggest(NumberFormatter.format(d));
        } catch (FastExpressionParser.MathFormatException ignored) {
        }
    }
}
