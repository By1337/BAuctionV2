package com.by1337.auc.menu;

import com.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.cmd.Command;

public class MenuBooter {

    public static void boot(MenuSubLoader subLoader, AucLifecycle lifecycle) {
        Command<ExecuteContext> commands = lifecycle.bootMenuCommand(Commands.create());
        ConfirmMenu.bootCommands(commands.copy());
        ContainerViewMenu.bootCommands(commands.copy());
        LotsMenu.bootCommands(commands.copy());
        HomeMenu.bootCommands(commands.copy());
        SelectCountMenu.bootCommands(commands.copy());
        VaultMenu.bootCommands(commands.copy());

        subLoader.menuCodecRegistry().register("bauc:home", HomeMenu.HomeMenuV2Config.CODEC);
        subLoader.menuCodecRegistry().register("bauc:vault", VaultMenu.VaultMenuConfig.CODEC);
        subLoader.menuCodecRegistry().register("bauc:confirm", ConfirmMenu.ConfirmMenuConfig.CODEC);
        subLoader.menuCodecRegistry().register("bauc:select_count", SelectCountMenu.SelectCountConfig.CODEC);
        subLoader.menuCodecRegistry().register("bauc:container_view", ContainerViewMenu.ContainerViewConfig.CODEC);
        lifecycle.menuRegister(subLoader);
    }
}
