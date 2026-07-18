package dev.by1337.auc.common.db;

import dev.by1337.sync.common.work.EventLoopWorker;
import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Queue;

public class DataBatcher<T> {
    private static final Logger log = LoggerFactory.getLogger(DataBatcher.class);
    private final MpmcArrayQueue<T> queue;
    private final Flusher<Queue<T>> flusher;
    private final EventLoopWorker worker;
    private volatile boolean closed;

    public DataBatcher(final int capacity, Flusher<Queue<T>> flusher, EventLoopWorker worker) {
        queue = new MpmcArrayQueue<>(capacity);
        this.flusher = flusher;
        this.worker = worker;
        worker.schedule(this::ioTick, 100);
    }

    private void ioTick() {
        if (closed) return;
        flush(Integer.MAX_VALUE);
        worker.schedule(this::ioTick, 100);
    }

    public void offer(T t) {
        if (closed) throw new IllegalStateException("Batch is closed!");
        if (!queue.offer(t)) {
            log.warn("DataBatcher queue is full, forcing flush");

            flush(queue.capacity() / 4);

            if (!queue.offer(t)) {
                throw new IllegalStateException(
                        "Failed to enqueue value after forced flush"
                );
            }
        }
    }

    private void flush(int limit) {
        try {
            flusher.accept(queue, limit);
        } catch (SQLException e) {
            log.error("Failed to flush", e);
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        try {
            flusher.accept(queue, Integer.MAX_VALUE);
        } catch (SQLException e) {
            log.error("Failed to flush", e);
        }
    }

    @FunctionalInterface
    public interface Flusher<T> {
        void accept(T t, int limit) throws SQLException;
    }
}
