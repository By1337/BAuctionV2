package dev.by1337.auc.command;

import dev.by1337.auc.config.Config;
import dev.by1337.auc.search.SearchManager;
import dev.by1337.auc.search.Trie;
import dev.by1337.auc.search.filter.MaterialSearchFilter;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.cmd.CommandReader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class SearchCommandTest {
    private static final String COMMAND = "ah search ";
    private final SearchFilter stone = mock(SearchFilter.class);
    private final SearchFilter sharpness = mock(SearchFilter.class);
    private final SearchFilter creeperEgg = new MaterialSearchFilter(Material.CREEPER_SPAWN_EGG);
    private Trie<SearchFilter> trie;
    private SearchCommand command;
    private Player player;

    @BeforeMethod
    public void setUp() {
        Locale locale = Locale.forLanguageTag("ru-RU");
        trie = new Trie<>();
        trie.insert("камень", stone);
        trie.insert("острота", sharpness);
        trie.insert("алмаз", mock(SearchFilter.class));
        trie.insert("алмазный меч", mock(SearchFilter.class));
        trie.insert("яйцо", mock(SearchFilter.class));
        trie.insert("яйцо призыва крипера", creeperEgg);

        var manager = mock(SearchManager.class);
        when(manager.trie()).thenReturn(trie);
        when(manager.locale()).thenReturn(locale);
        command = new SearchCommand("search", Map.of("ru_RU", manager), mock(Config.class));
        player = mock(Player.class);
        when(player.locale()).thenReturn(locale);
    }

    @DataProvider
    public Object[][] unknownPrefixes() {
        return new Object[][]{
                { "облакокаме" },
                { "остротаоблакокаме" },
                { "облако каме" }
        };
    }

    @Test(dataProvider = "unknownPrefixes")
    public void doesNotCompleteAfterUnknownText(String query) {
        assertEquals(completedQueries(query), List.of());
    }

    @Test
    public void requiresSpaceBeforeTheNextTerm() {
        assertEquals(completedQueries("остротакаме"), List.of());
        assertEquals(completedQueries("яйцо призыва крипераост"), List.of());
    }

    @DataProvider
    public Object[][] validPrefixes() {
        return new Object[][]{
                { "каме", List.of("камень") },
                { "КАМЕ", List.of("камень") },
                { "острота каме", List.of("острота камень") },
                { "острота  каме", List.of("острота  камень") },
                { "алмазный м", List.of("алмазный меч") },
                { "алмазный ", List.of("алмазный меч") },
                { "алмазный меч ост", List.of("алмазный меч острота") },
                { "камень алм", List.of("камень алмаз", "камень алмазный меч") },
                { "яйцо призыва кри", List.of("яйцо призыва крипера") },
                { "яйцо призыва крипера ост", List.of("яйцо призыва крипера острота") },
                { "острота ", List.of("острота яйцо", "острота алмаз", "острота камень", "острота острота",
                        "острота алмазный меч", "острота яйцо призыва крипера") }
        };
    }

    @Test(dataProvider = "validPrefixes")
    public void completesKnownTerms(String query, List<String> expected) {
        assertEquals(completedQueries(query), expected);
    }

    @Test
    public void parsingStillIgnoresUnknownText() {
        assertEquals(trie.parse("остротаоблакокамень"), List.of(sharpness, stone));
    }

    @Test
    public void parsesTheFullItemNameAsOneTerm() {
        assertEquals(trie.parse("яйцо призыва крипера острота"), List.of(creeperEgg, sharpness));
    }

    private List<String> completedQueries(String query) {
        String source = COMMAND + query;
        var reader = new CommandReader(source);
        reader.ridx(COMMAND.length());
        var suggestions = command.suggest(player, reader);
        String unchanged = source.substring(COMMAND.length(), suggestions.start());
        return suggestions.toList().stream()
                .map(completion -> unchanged + completion)
                .toList();
    }
}
