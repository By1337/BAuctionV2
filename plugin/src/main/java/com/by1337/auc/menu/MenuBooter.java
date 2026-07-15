package com.by1337.auc.menu;

import dev.by1337.bmenu.loader.MenuSubLoader;

public class MenuBooter {
    public static void boot(MenuSubLoader subLoader){
        subLoader.menuCodecRegistry().register("bauc:home", HomeMenu.HomeMenuV2Config.CODEC);
        subLoader.menuCodecRegistry().register("bauc:vault", VaultMenu.VaultMenuConfig.CODEC);
        subLoader.menuCodecRegistry().register("bauc:confirm", ConfirmMenu.ConfirmMenuConfig.CODEC);
    }
}
