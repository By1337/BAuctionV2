package dev.by1337.auc.handler.eco;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.eco.VaultHook;
import dev.by1337.auc.handler.event.UserMailEvent;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.UserMails;
import dev.by1337.auc.user.pdc.PdcMap;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class EcoGiverTest {
    @Test
    public void bVaultStillHonorsPreviouslyQueuedPaymentsWithoutCheckingOnlineStatus() throws Exception {
        UUID seller = UUID.randomUUID();
        var economy = mock(VaultHook.class);
        when(economy.isBVault()).thenReturn(true);

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            new EcoGiver().handle(null, new UserMailEvent(new AucUser(seller, new PdcMap()), UserMails.makeDepositCents(369)));

            verify(economy).depositCents(eq(seller), eq(369L), any());
            plugin.verify(BAuction::playerList, never());
        }
    }
}
