package com.eternity.races.common.progression;

import com.eternity.races.RacesMod;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Загружает деревья прогрессии из JSON файлов data/racecraft/trees/race_N.json.
 * Реализует ResourceReloadListener для перезагрузки при /reload.
 */
public class ProgressionLoader extends SimplePreparableReloadListener<Map<Integer, RaceProgressionTree>> {

    public static final ProgressionLoader INSTANCE = new ProgressionLoader();

    private final Map<Integer, RaceProgressionTree> trees = new HashMap<>();

    @Override
    protected Map<Integer, RaceProgressionTree> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Integer, RaceProgressionTree> result = new HashMap<>();
        for (int i = 1; i <= 15; i++) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "trees/race_" + i + ".json");
            try (InputStream is = manager.open(loc);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                RaceProgressionTree tree = parseTree(json);
                result.put(tree.raceId, tree);
            } catch (Exception e) {
                RacesMod.class.getModule().getLayer(); // logger placeholder
                System.err.println("[EternityRaces] Ошибка загрузки дерева расы " + i + ": " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Integer, RaceProgressionTree> object, ResourceManager manager, ProfilerFiller profiler) {
        trees.clear();
        trees.putAll(object);
        System.out.println("[EternityRaces] Загружено " + trees.size() + " деревьев прогрессии.");
    }

    public RaceProgressionTree getTree(int raceId) {
        return trees.get(raceId);
    }

    // ─── JSON-парсинг ─────────────────────────────────────────────────────────

    private static RaceProgressionTree parseTree(JsonObject json) {
        int raceId = json.get("race_id").getAsInt();
        String raceName = json.get("race_name").getAsString();

        JsonObject tier2 = json.getAsJsonObject("tier2");
        int tier2Threshold = tier2.get("threshold").getAsInt();
        JsonObject tier3 = json.getAsJsonObject("tier3");
        int tier3Threshold = tier3.get("threshold").getAsInt();

        JsonObject branches = tier2.getAsJsonObject("branches");
        String branchAName = branches.getAsJsonObject("branch_a").get("name").getAsString();
        String branchBName = branches.getAsJsonObject("branch_b").get("name").getAsString();

        RaceProgressionTree tree = new RaceProgressionTree(
                raceId, raceName, tier2Threshold, tier3Threshold, branchAName, branchBName);

        // Tier 1 (индексы 0, 1, 2)
        JsonArray t1 = json.getAsJsonObject("tier1").getAsJsonArray("abilities");
        int cost1 = json.getAsJsonObject("tier1").get("cost").getAsInt();
        for (int i = 0; i < t1.size(); i++) {
            String id = t1.get(i).getAsString();
            tree.tier1Nodes.add(new ProgressionNode(id, i, 1, -1, cost1,
                    "ability." + RacesMod.MOD_ID + "." + id + ".name",
                    "ability." + RacesMod.MOD_ID + "." + id + ".desc",
                    null));
        }

        // Tier 2 Branch A (индексы 3, 4)
        JsonArray t2a = branches.getAsJsonObject("branch_a").getAsJsonArray("abilities");
        int cost2 = branches.getAsJsonObject("branch_a").get("cost_per_ability").getAsInt();
        for (int i = 0; i < t2a.size(); i++) {
            String id = t2a.get(i).getAsString();
            tree.tier2BranchA.add(new ProgressionNode(id, 3 + i, 2, 0, cost2,
                    "ability." + RacesMod.MOD_ID + "." + id + ".name",
                    "ability." + RacesMod.MOD_ID + "." + id + ".desc",
                    branchAName));
        }

        // Tier 2 Branch B (индексы 5, 6)
        JsonArray t2b = branches.getAsJsonObject("branch_b").getAsJsonArray("abilities");
        for (int i = 0; i < t2b.size(); i++) {
            String id = t2b.get(i).getAsString();
            tree.tier2BranchB.add(new ProgressionNode(id, 5 + i, 2, 1, cost2,
                    "ability." + RacesMod.MOD_ID + "." + id + ".name",
                    "ability." + RacesMod.MOD_ID + "." + id + ".desc",
                    branchBName));
        }

        // Tier 3 (индекс 7)
        JsonArray t3 = tier3.getAsJsonArray("abilities");
        int cost3 = tier3.get("cost_per_ability").getAsInt();
        for (int i = 0; i < t3.size(); i++) {
            String id = t3.get(i).getAsString();
            tree.tier3Nodes.add(new ProgressionNode(id, 7, 3, -1, cost3,
                    "ability." + RacesMod.MOD_ID + "." + id + ".name",
                    "ability." + RacesMod.MOD_ID + "." + id + ".desc",
                    null));
        }

        return tree;
    }
}
