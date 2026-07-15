package com.by1337.auc;

import com.by1337.auc.handler.SimpleAuction;

class Boot {

    public static void boot(BAuction plugin, SimpleAuction auction) {
        plugin.config().tags.search.forEach((k, v) -> v.boot(auction.tag2id(), k));
    }

}
