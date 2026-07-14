package com.by1337.auc.transaction;

import com.by1337.auc.handler.Auction;
import com.by1337.auc.handler.event.ActionResult;
import dev.by1337.sync.common.callback.ResponseFuture;

@FunctionalInterface
public interface Transaction<T> {
    ResponseFuture<T> apply(Auction auction);
}
