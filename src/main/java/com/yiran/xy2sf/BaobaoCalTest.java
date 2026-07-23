package com.yiran.xy2sf;

public class BaobaoCalTest {

    //^.*加强魅惑.*(?:\r?\n|\r)?

    // 测试调用
    public static void main(String[] args) {
        // 示例：成长率 1.275，100级，全敏加点 400 点，初值血100/法0/攻0/速180
        PetAttributeResult result = predictPetAttributes(
                3.605, // 成长率
                160,   // 等级
                0,     // 根骨加点
                0,     // 灵性加点
                730,     // 力量加点
                0,   // 敏捷加点

                300,   // 初值血
                0,     // 初值法
                360,     // 初值攻
                0    // 初值速
        );

        System.out.println(result);
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
     * 计算召唤兽最终属性预测值（包含等级自动增加的基础点数）
     *
     * @param growthRate Growth rate (e.g. 1.275)
     * @param level      Level
     * @param addBone    Manual allocation for Bone
     * @param addSpirit  Manual allocation for Spirit
     * @param addPower   Manual allocation for Power
     * @param addSpeed   Manual allocation for Speed
     * @param baseHp     Base HP
     * @param baseMp     Base MP
     * @param baseAp     Base AP
     * @param baseSp     Base SP
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

        // 1. 计算各项的总点数 (玩家手动分配的点数 + 等级自带的点数)
        int totalBone = addBone + level;
        int totalSpirit = addSpirit + level;
        int totalPower = addPower + level;
        int totalSpeed = addSpeed + level;

        // 2. 计算 HP (血量)
        int hpFromStats = (int) Math.floor(level * growthRate * totalBone * 0.7);
        int hpFromBase = (int) Math.floor(baseHp * (1 + level * growthRate * 0.002));
        int finalHp = hpFromStats + hpFromBase;

        // 3. 计算 MP (法力)
        int mpFromStats = (int) Math.floor(level * growthRate * totalSpirit * 0.7);
        int mpFromBase = (int) Math.floor(baseMp * (1 + level * growthRate * 0.002));
        int finalMp = mpFromStats + mpFromBase;

        // 4. 计算 AP (攻击)
        int apFromStats = (int) Math.floor(level * growthRate * totalPower * 0.14);
        int apFromBase = (int) Math.floor(baseAp * (1 + level * growthRate * 0.002));
        int finalAp = apFromStats + apFromBase;

        // 5. 计算 SP (速度)
        int finalSp = (int) Math.floor((baseSp + totalSpeed) * growthRate);

        return new PetAttributeResult(finalHp, finalMp, finalAp, finalSp);
    }

}
