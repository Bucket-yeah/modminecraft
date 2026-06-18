package com.eternity.races.common.progression;

import java.util.List;

/**
 * Один узел в дереве прогрессии.
 */
public class ProgressionNode {

    public final String abilityId;
    public final int abilityIndex;   // 0-7 в массиве способностей
    public final int tier;           // 1, 2, или 3
    public final int branch;         // -1 = нет ветки, 0 = ветка A, 1 = ветка B
    public final int cost;           // стоимость в очках рас
    public final String nameKey;     // ключ локализации для названия
    public final String descKey;     // ключ локализации для описания
    public final String branchName;  // название ветки (null для Tier 1 и 3)

    public ProgressionNode(String abilityId, int abilityIndex, int tier, int branch,
                           int cost, String nameKey, String descKey, String branchName) {
        this.abilityId = abilityId;
        this.abilityIndex = abilityIndex;
        this.tier = tier;
        this.branch = branch;
        this.cost = cost;
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.branchName = branchName;
    }
}
