package net.minecraft.server;

public class ControllerJump {

    private EntityLiving a;
    private boolean b = false;

    public ControllerJump(EntityLiving entityliving) {
        this.a = entityliving;
    }

    public void a() {
        this.b = true;
    }

    public void b() {
        if (this.b) {
            this.a.e(true);
            this.b = false;
        }
    }
}
