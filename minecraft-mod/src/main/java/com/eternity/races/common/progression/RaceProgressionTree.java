package com.eternity.races.common.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * Дерево прогрессии одной расы: 3 уровня, ветки A и B на Tier 2.
 */
public class RaceProgressionTree {

    public final int raceId;
    public final String raceName;
    public final List<ProgressionNode> tier1Nodes = new ArrayList<>();
    public final List<ProgressionNode> tier2BranchA = new ArrayList<>();
    public final List<ProgressionNode> tier2BranchB = new ArrayList<>();
    public final List<ProgressionNode> tier3Nodes = new ArrayList<>();

    public final int tier2Threshold;   // минимум потраченных очков для Tier 2
    public final int tier3Threshold;   // минимум потраченных очков для Tier 3

    public final String branchAName;
    public final String branchBName;

    public RaceProgressionTree(int raceId, String raceName,
                                int tier2Threshold, int tier3Threshold,
                                String branchAName, String branchBName) {
        this.raceId = raceId;
        this.raceName = raceName;
        this.tier2Threshold = tier2Threshold;
        this.tier3Threshold = tier3Threshold;
        this.branchAName = branchAName;
        this.branchBName = branchBName;
    }

    /** Возвращает все узлы всех уровней. */
    public List<ProgressionNode> allNodes() {
        List<ProgressionNode> all = new ArrayList<>();
        all.addAll(tier1Nodes);
        all.addAll(tier2BranchA);
        all.addAll(tier2BranchB);
        all.addAll(tier3Nodes);
        return all;
    }
}
