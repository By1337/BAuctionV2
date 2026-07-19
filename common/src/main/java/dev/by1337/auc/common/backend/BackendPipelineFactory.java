package dev.by1337.auc.common.backend;

import dev.by1337.auc.common.backend.item.ItemServiceBackend;
import dev.by1337.auc.common.backend.log.LogRepositoryBackend;
import dev.by1337.auc.common.backend.lot.LotsRepositoryBackend;
import dev.by1337.sync.common.channel.pipeline.Pipeline;

public class BackendPipelineFactory {

    public static void make(Pipeline pipeline){
        pipeline
                .addLast("item_stack_repository", new ItemServiceBackend())
                .addLast("lots_repository", new LotsRepositoryBackend())
                .addLast("name_repository", new PlayerNameBackend())
                .addLast("log_repository", new LogRepositoryBackend())
                ;
    }
}
