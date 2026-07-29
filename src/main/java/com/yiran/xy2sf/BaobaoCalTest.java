package com.yiran.xy2sf;

public class BaobaoCalTest {

    public static void main(String[] args) {
        // 测试 1
        PetAttributeResult result = predictPetAttributes(
                3.400, // 成长率
                160,   // 等级
                0,     // 根骨加点
                0,     // 灵性加点
                730,   // 力量加点
                0,     // 敏捷加点

                6666,   // 初值血
                6666,     // 初值法
                6696,   // 初值攻
                6666      // 初值速
        );

        System.out.println(result);

        // 测试 2 (完全匹配官网样例)
        PetAttributeResult result2 = predictPetAttributes(
                5.40, // 成长率
                160,   // 等级
                0,     // 根骨加点
                0,     // 灵性加点
                730,   // 力量加点
                0,     // 敏捷加点

                3333,  // 初值血
                3333,     // 初值法
                3393,  // 初值攻
                3333      // 初值速
        );

        System.out.println(result2);

    }

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
            return String.format("召唤兽预测属性 -> 血量(HP): %d | 法力(MP): %d | 攻击(AP): %d | 速度(SP): %d",
                    hp, mp, ap, sp);
        }
    }

    /**
     * 计算召唤兽最终属性预测值（官方100%对齐版）
     */
    public static PetAttributeResult predictPetAttributes(
            double growthRate,
            int level,
            int addBone,
            int addSpirit,
            int addPower,
            int addSpeed,
            int baseHp,
            int baseMp,
            int baseAp,
            int baseSp) {

        // 各项总点数 = 玩家手动加点 + 等级自带点数
        int totalBone = addBone + level;
        int totalSpirit = addSpirit + level;
        int totalPower = addPower + level;
        int totalSpeed = addSpeed + level;

        // 1. HP 计算
        int finalHp = baseHp + (int) Math.floor((baseHp * 0.7 + totalBone) * level * growthRate);

        // 2. MP 计算
        int finalMp = baseMp + (int) Math.floor((baseMp * 0.7 + totalSpirit) * level * growthRate);

        // 3. AP 计算
        int finalAp = baseAp + (int) Math.floor((baseAp * 0.14 + totalPower * 0.2) * level * growthRate);

        // 4. SP 计算
        int finalSp = (int) Math.floor((baseSp + totalSpeed) * growthRate);

        return new PetAttributeResult(finalHp, finalMp, finalAp, finalSp);
    }
}