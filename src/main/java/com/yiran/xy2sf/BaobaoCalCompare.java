package com.yiran.xy2sf;

import java.util.EnumSet;

public class BaobaoCalCompare {

    // ==================== 测试调用 ====================

    public static void main(String[] args) {
        // 场景：比较两只 160 级的召唤兽
        // 召唤兽1: 成长率 3.605，初值 (300/0/360/0)，加点：力量加 730 点 (全攻)
        // 召唤兽2: 成长率 1.092，初值 (1200/0/1518/0)
        // 目标：让召唤兽2 的攻击力达到与召唤兽1 一致，剩余属性点全部分配给血量(根骨)
        int level = 160;
//        AllocationScheme pet1Scheme = new AllocationScheme(0, 0, 730, 0);
//        PetCompareResult result = compareAndPredict(
//                3.605, 300, 0, 360, 0,    // 召唤1 属性
//                level, pet1Scheme,        // 等级 & 召唤1 加点
//                1.092, 1200, 0, 1518, 0,  // 召唤2 属性
//                EnumSet.of(StatType.POWER),// 让召唤2 的攻击 (POWER) 对齐召唤1
//                StatType.BONE             // 剩余点数全加根骨 (血)
//        );
        AllocationScheme pet1Scheme = new AllocationScheme(0, 0, 730, 0);

        PetCompareResult result = compareAndPredict(
                1.092, 1200, 0, 1518, 0,    // 召唤1 属性
                level, pet1Scheme,        // 等级 & 召唤1 加点
                1.605, 300, 0, 360, 0,  // 召唤2 属性
                EnumSet.of(StatType.POWER),// 让召唤2 的攻击 (POWER) 对齐召唤1
                StatType.BONE             // 剩余点数全加根骨 (血)
        );

        System.out.println(result);
    }

    /** 属性枚举类 */
    public enum StatType {
        BONE,   // 根骨 (血)
        SPIRIT, // 灵性 (法)
        POWER,  // 力量 (攻)
        SPEED   // 敏捷 (速)
    }

    /** 召唤兽属性面板结果 */
    public static class PetAttributeResult {
        private final int hp;
        private final int mp;
        private final int ap;
        private final int sp;

        public PetAttributeResult(int hp, int mp, int ap, int sp) {
            this.hp = hp;
            this.mp = mp;
            this.ap = ap;
            this.sp = sp;
        }

        public int getHp() { return hp; }
        public int getMp() { return mp; }
        public int getAp() { return ap; }
        public int getSp() { return sp; }

        @Override
        public String toString() {
            return String.format("血量(HP): %-7d | 法力(MP): %-7d | 攻击(AP): %-7d | 速度(SP): %-5d", hp, mp, ap, sp);
        }
    }

    /** 召唤兽加点方案 */
    public static class AllocationScheme {
        public int addBone;
        public int addSpirit;
        public int addPower;
        public int addSpeed;

        public AllocationScheme(int addBone, int addSpirit, int addPower, int addSpeed) {
            this.addBone = addBone;
            this.addSpirit = addSpirit;
            this.addPower = addPower;
            this.addSpeed = addSpeed;
        }

        public int getTotalPoints() {
            return addBone + addSpirit + addPower + addSpeed;
        }

        @Override
        public String toString() {
            return String.format("根骨: %d, 灵性: %d, 力量: %d, 敏捷: %d (总加点: %d)",
                    addBone, addSpirit, addPower, addSpeed, getTotalPoints());
        }
    }

    /** 召唤兽对比预测结果 */
    public static class PetCompareResult {
        public PetAttributeResult pet1Stats;  // 召唤兽1 最终面板
        public PetAttributeResult pet2Stats;  // 召唤兽2 最终面板
        public AllocationScheme pet2Scheme;   // 召唤兽2 计算出的加点方案
        public boolean pointsOverflow;        // 召唤兽2 是否点数不够用

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=================== 召唤兽数值预测对比 ===================\n");
            sb.append("召唤兽 1 面板 -> ").append(pet1Stats).append("\n");
            sb.append("----------------------------------------------------------\n");
            sb.append("召唤兽 2 加点 -> ").append(pet2Scheme);
            if (pointsOverflow) {
                sb.append(" ⚠️ [警告: 达到目标所需点数超出可分配上限!]");
            }
            sb.append("\n");
            sb.append("召唤兽 2 面板 -> ").append(pet2Stats).append("\n");
            sb.append("==========================================================");
            return sb.toString();
        }
    }

    // ==================== 核心算法 ====================

    /**
     * 根据加点与初值正向计算面板
     */
    public static PetAttributeResult predictPetAttributes(
            double growthRate, int level,
            int addBone, int addSpirit, int addPower, int addSpeed,
            int baseHp, int baseMp, int baseAp, int baseSp) {

        int totalBone = addBone + level;
        int totalSpirit = addSpirit + level;
        int totalPower = addPower + level;
        int totalSpeed = addSpeed + level;

        int finalHp = baseHp + (int) Math.floor((baseHp * 0.7 + totalBone) * level * growthRate);
        int finalMp = baseMp + (int) Math.floor((baseMp * 0.7 + totalSpirit) * level * growthRate);
        int finalAp = baseAp + (int) Math.floor((baseAp * 0.14 + totalPower * 0.2) * level * growthRate);
        int finalSp = (int) Math.floor((baseSp + totalSpeed) * growthRate);

        return new PetAttributeResult(finalHp, finalMp, finalAp, finalSp);
    }

    /**
     * 对比两只召唤兽：反向解析召唤兽 2 的加点方案并给出对比结果
     *
     * @param growthRate1          召唤兽1 成长率
     * @param baseHp1              召唤兽1 初值血
     * @param baseMp1              召唤兽1 初值法
     * @param baseAp1              召唤兽1 初值攻
     * @param baseSp1              召唤兽1 初值速
     * @param level                召唤兽等级 (两者相同)
     * @param scheme1              召唤兽1 的手动加点方案
     * @param growthRate2          召唤兽2 成长率
     * @param baseHp2              召唤兽2 初值血
     * @param baseMp2              召唤兽2 初值法
     * @param baseAp2              召唤兽2 初值攻
     * @param baseSp2              召唤兽2 初值速
     * @param matchTargets         召唤兽2 需要对齐召唤兽1 的属性集合 (如: EnumSet.of(StatType.POWER))
     * @param remainingTargetStat  对齐完目标后，剩余的所有属性点加到哪项上
     */
    public static PetCompareResult compareAndPredict(
            double growthRate1, int baseHp1, int baseMp1, int baseAp1, int baseSp1,
            int level, AllocationScheme scheme1,
            double growthRate2, int baseHp2, int baseMp2, int baseAp2, int baseSp2,
            EnumSet<StatType> matchTargets, StatType remainingTargetStat) {

        // 1. 先计算召唤兽1 的最终面板
        PetAttributeResult res1 = predictPetAttributes(
                growthRate1, level,
                scheme1.addBone, scheme1.addSpirit, scheme1.addPower, scheme1.addSpeed,
                baseHp1, baseMp1, baseAp1, baseSp1
        );

        int totalAvailablePoints = scheme1.getTotalPoints();
        int reqBone = 0, reqSpirit = 0, reqPower = 0, reqSpeed = 0;

        // 2. 根据选定的对齐属性，反向求解召唤兽2 所需加点
        if (matchTargets.contains(StatType.POWER)) {
            reqPower = calcRequiredPower(res1.getAp(), baseAp2, level, growthRate2);
        }
        if (matchTargets.contains(StatType.BONE)) {
            reqBone = calcRequiredBone(res1.getHp(), baseHp2, level, growthRate2);
        }
        if (matchTargets.contains(StatType.SPIRIT)) {
            reqSpirit = calcRequiredSpirit(res1.getMp(), baseMp2, level, growthRate2);
        }
        if (matchTargets.contains(StatType.SPEED)) {
            reqSpeed = calcRequiredSpeed(res1.getSp(), baseSp2, growthRate2, level);
        }

        // 3. 计算已消费的点数与剩余点数
        int usedPoints = reqBone + reqSpirit + reqPower + reqSpeed;
        int remainingPoints = totalAvailablePoints - usedPoints;
        boolean overflow = false;

        if (remainingPoints < 0) {
            overflow = true;
            remainingPoints = 0; // 超出时清零剩余点分配
        }

        // 4. 将剩余点数分配到指定的单一属性上
        switch (remainingTargetStat) {
            case BONE:   reqBone += remainingPoints; break;
            case SPIRIT: reqSpirit += remainingPoints; break;
            case POWER:  reqPower += remainingPoints; break;
            case SPEED:  reqSpeed += remainingPoints; break;
        }

        AllocationScheme scheme2 = new AllocationScheme(reqBone, reqSpirit, reqPower, reqSpeed);

        // 5. 根据导出的加点方案计算召唤兽2 的面板
        PetAttributeResult res2 = predictPetAttributes(
                growthRate2, level,
                scheme2.addBone, scheme2.addSpirit, scheme2.addPower, scheme2.addSpeed,
                baseHp2, baseMp2, baseAp2, baseSp2
        );

        PetCompareResult compareResult = new PetCompareResult();
        compareResult.pet1Stats = res1;
        compareResult.pet2Stats = res2;
        compareResult.pet2Scheme = scheme2;
        compareResult.pointsOverflow = overflow;

        return compareResult;
    }

    // ==================== 反向推算辅助函数 ====================

    private static int calcRequiredPower(int targetAp, int baseAp, int level, double growth) {
        if (targetAp <= baseAp) return 0;
        double inner = ((targetAp - baseAp) / (level * growth) - baseAp * 0.14) / 0.2;
        int req = (int) Math.ceil(inner - level);
        req = Math.max(0, req);
        while (calcAp(baseAp, req, level, growth) < targetAp) req++;
        return req;
    }

    private static int calcRequiredBone(int targetHp, int baseHp, int level, double growth) {
        if (targetHp <= baseHp) return 0;
        double inner = (targetHp - baseHp) / (level * growth) - baseHp * 0.7;
        int req = (int) Math.ceil(inner - level);
        req = Math.max(0, req);
        while (calcHp(baseHp, req, level, growth) < targetHp) req++;
        return req;
    }

    private static int calcRequiredSpirit(int targetMp, int baseMp, int level, double growth) {
        if (targetMp <= baseMp) return 0;
        double inner = (targetMp - baseMp) / (level * growth) - baseMp * 0.7;
        int req = (int) Math.ceil(inner - level);
        req = Math.max(0, req);
        while (calcMp(baseMp, req, level, growth) < targetMp) req++;
        return req;
    }

    private static int calcRequiredSpeed(int targetSp, int baseSp, double growth, int level) {
        int req = (int) Math.ceil(targetSp / growth - baseSp - level);
        req = Math.max(0, req);
        while ((int) Math.floor((baseSp + req + level) * growth) < targetSp) req++;
        return req;
    }

    private static int calcAp(int baseAp, int addPower, int level, double growth) {
        return baseAp + (int) Math.floor((baseAp * 0.14 + (addPower + level) * 0.2) * level * growth);
    }
    private static int calcHp(int baseHp, int addBone, int level, double growth) {
        return baseHp + (int) Math.floor((baseHp * 0.7 + (addBone + level)) * level * growth);
    }
    private static int calcMp(int baseMp, int addSpirit, int level, double growth) {
        return baseMp + (int) Math.floor((baseMp * 0.7 + (addSpirit + level)) * level * growth);
    }


}