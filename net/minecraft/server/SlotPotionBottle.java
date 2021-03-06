package net.minecraft.server;

class SlotPotionBottle extends Slot {

    private EntityHuman a;

    public SlotPotionBottle(EntityHuman entityhuman, IInventory iinventory, int i, int j, int k) {
        super(iinventory, i, j, k);
        this.a = entityhuman;
    }

    public boolean isAllowed(ItemStack itemstack) {
        return a_(itemstack);
    }

    public int a() {
        return 1;
    }

    public void b(ItemStack itemstack) {
        if (itemstack.id == Item.POTION.id && itemstack.getData() > 0) {
            this.a.a((Statistic) AchievementList.A, 1);
        }

        super.b(itemstack);
    }

    public static boolean a_(ItemStack itemstack) {
        return itemstack != null && (itemstack.id == Item.POTION.id || itemstack.id == Item.GLASS_BOTTLE.id);
    }
}
