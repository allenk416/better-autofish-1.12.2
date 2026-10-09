import java.lang.reflect.Field;
import java.nio.file.*;
import com.northwestwind.forgeautofish.handler.*;
import com.northwestwind.forgeautofish.keybind.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraftforge.fml.common.gameevent.*;

public class HandlerIntegrationTest {
    static int checks;
    static AutoFishHandler handler;
    static Minecraft mc=Minecraft.instance;
    static void ok(boolean result,String description){checks++;if(!result)throw new AssertionError(description);}
    static void set(Object target,String name,Object value)throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    static Object get(Object target,String name)throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    static void arm(long id,long age)throws Exception {
        Object p=get(handler,"policy");set(p,"readyLastTick",true);set(p,"castAllowedAt",0L);set(p,"reelAllowedAt",0L);
        set(p,"observedHook",id);set(p,"hookSince",System.nanoTime()/1000000L-age);
    }
    static void tick(){handler.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));}
    static void toggle(){KeyBinds.autofish.pending=true;handler.onKeyInput(new InputEvent.KeyInputEvent());}
    static EntityFishHook hook(int id,EntityPlayerSP owner) {
        EntityFishHook h=new EntityFishHook();h.id=id;h.owner=owner;h.field_70170_p=mc.field_71441_e;
        mc.field_71441_e.field_72996_f.add(h);return h;
    }
    public static void main(String[] args)throws Exception {
        Path testRoot=Paths.get("build/test-data");Files.createDirectories(testRoot);
        mc.field_71412_D=Files.createTempDirectory(testRoot,"handler-").toFile();
        mc.field_71439_g=new EntityPlayerSP();mc.field_71441_e=new WorldClient();
        mc.field_71439_g.main.item=new ItemFishingRod();
        KeyBinds.register();
        handler=new AutoFishHandler();tick();
        ok(mc.field_71442_b.calls==0,"Disabled by default");toggle();tick();
        ok(mc.field_71442_b.calls==0,"Startup delay");
        arm(Long.MIN_VALUE,0);tick();ok(mc.field_71442_b.calls==1,"Idle casts via normal controller");
        EntityFishHook live=hook(1,mc.field_71439_g);tick();
        ok(mc.field_71439_g.field_71104_cf==live,"Repairs missing own-hook reference");
        ok(mc.field_71442_b.calls==1,"Repair does not double-click");
        live.field_70181_x=-0.1;tick();
        ok(mc.field_71442_b.calls==2,"Immediate bite reels despite age and cast retry timer");
        ok(get(handler,"retiredHook")==live,"Tracks reeled hook only");
        long remaining=(Long)get(handler,"retireAt")-System.nanoTime()/1000000L;
        ok(remaining>2000,"Ghost cleanup timeout separate from 500ms recast");
        mc.field_71439_g.field_71104_cf=null;arm(Long.MIN_VALUE,0);tick();
        ok(mc.field_71442_b.calls==2,"Pending old hook blocks premature cast even with missing reference");
        EntityFishHook newer=hook(2,mc.field_71439_g);mc.field_71439_g.field_71104_cf=newer;
        set(handler,"retireAt",0L);tick();
        ok(live.field_70128_L,"Removes reeled client ghost");
        ok(mc.field_71439_g.field_71104_cf==newer,"Old hook cleanup preserves newer hook");
        newer.field_70128_L=true;mc.field_71439_g.field_71104_cf=null;
        hook(3,new EntityPlayerSP());arm(Long.MIN_VALUE,0);tick();
        ok(mc.field_71439_g.field_71104_cf==null,"Other player's hook ignored");
        ok(mc.field_71442_b.calls==3,"Other hook does not stop own cast");
        mc.field_71462_r=new net.minecraft.client.gui.GuiScreen();arm(Long.MIN_VALUE,0);tick();
        ok(mc.field_71442_b.calls==4,"GUI allows idle cast");
        net.minecraft.client.gui.GuiScreen gui=mc.field_71462_r;
        EntityFishHook guiHook=hook(4,mc.field_71439_g);guiHook.field_70181_x=-0.1;tick();
        ok(mc.field_71442_b.calls==5,"GUI allows instant bite reel");
        ok(mc.field_71462_r==gui,"Fishing preserves GUI and mouse-free state");
        set(handler,"retireAt",0L);mc.field_71462_r=null;tick();
        int beforeGuard=mc.field_71442_b.calls;
        mc.field_71439_g.main.damage=62;arm(Long.MIN_VALUE,0);tick();
        ok(mc.field_71442_b.calls==beforeGuard,"Durability protects rod");mc.field_71439_g.main.damage=0;
        mc.field_71439_g.main.item=new net.minecraft.item.Item();
        mc.field_71439_g.off.item=new ItemFishingRod();arm(Long.MIN_VALUE,0);tick();
        ok(mc.field_71442_b.calls==beforeGuard+1 && mc.field_71442_b.lastHand==net.minecraft.util.EnumHand.OFF_HAND,"Off-hand supported");
        mc.field_71439_g=null;mc.field_71441_e=null;tick();
        ok(mc.field_71442_b.calls==beforeGuard+1,"Disconnect blocks actions");
        mc.field_71439_g=new EntityPlayerSP();mc.field_71441_e=new WorldClient();
        mc.field_71439_g.main.item=new ItemFishingRod();tick();
        ok(mc.field_71442_b.calls==beforeGuard+1,"Reconnect waits for readiness");
        arm(Long.MIN_VALUE,0);tick();ok(mc.field_71442_b.calls==beforeGuard+2,"Reconnect keeps enabled and resumes");
        // A valid bite sound can reel before movement packets show a downward velocity.
        EntityFishHook soundHook=hook(5,mc.field_71439_g);soundHook.wet=false;soundHook.field_70165_t=10;
        tick();sound(10,0,0);tick();
        ok(mc.field_71442_b.calls==beforeGuard+3,"Sound catches fast bite before motion/in-water state");
        set(handler,"retireAt",0L);tick();
        EntityFishHook own=hook(6,mc.field_71439_g);own.field_70165_t=0;mc.field_71439_g.field_71104_cf=own;
        EntityFishHook other=hook(7,new EntityPlayerSP());other.field_70165_t=1;
        tick();int beforeOther=mc.field_71442_b.calls;sound(1,0,0);tick();
        ok(mc.field_71442_b.calls==beforeOther,"Sound nearest other player's hook ignored");
        // Settings key opens a non-pausing GUI; editing and saving changes live config.
        KeyBinds.settings.pending=true;handler.onKeyInput(new InputEvent.KeyInputEvent());
        ok(mc.field_71462_r instanceof FishingSettingsScreen,"F10 opens settings");
        FishingSettingsScreen settings=(FishingSettingsScreen)mc.field_71462_r;
        ok(!settings.func_73868_f(),"Settings screen does not pause");
        net.minecraft.client.gui.GuiTextField[] fields=(net.minecraft.client.gui.GuiTextField[])get(settings,"fields");
        for(net.minecraft.client.gui.GuiTextField f:fields) ok(f.x>=0 && f.x+f.width<=320 && f.y+f.height<172,"Field fits minimum GUI size");
        fields[0].func_146180_a("100");
        Object policyBeforeSave=get(handler,"policy");
        java.lang.reflect.Method click=settings.getClass().getDeclaredMethod("func_146284_a",net.minecraft.client.gui.GuiButton.class);click.setAccessible(true);
        click.invoke(settings,new net.minecraft.client.gui.GuiButton(0,0,0,0,0,""));
        ok("100".equals(handler.settingsSnapshot().getProperty("recastDelayMs")),"Settings apply without restart");
        ok(get(handler,"policy")==policyBeforeSave && (Boolean)get(policyBeforeSave,"readyLastTick"),"Saving settings preserves the current fishing cycle");
        ok(mc.field_71462_r==settings,"Save keeps settings open for AFK");
        fields[0].func_146180_a("NaN");click.invoke(settings,new net.minecraft.client.gui.GuiButton(0,0,0,0,0,""));
        ok("100".equals(handler.settingsSnapshot().getProperty("recastDelayMs")),"Invalid settings rejected");
        toggle();arm(Long.MIN_VALUE,0);tick();ok(mc.field_71442_b.calls==beforeOther,"Toggle off stops fishing");
        String properties=new String(Files.readAllBytes(mc.field_71412_D.toPath().resolve("config/autofish-recovery.properties")),"ISO-8859-1");
        ok(properties.contains("enabled=false"),"Toggle persisted");
        mc.field_71412_D=Files.createTempDirectory(testRoot,"migration-").toFile();
        Path legacy=mc.field_71412_D.toPath().resolve("config/autofish-recovery.properties");Files.createDirectories(legacy.getParent());
        Files.write(legacy,"enabled=true\nrecastDelayMs=2000\nprotectRod=false\n".getBytes("ISO-8859-1"));
        AutoFishHandler migrated=new AutoFishHandler();
        ok("500".equals(migrated.settingsSnapshot().getProperty("recastDelayMs")),"Old default migrates to 500ms");
        ok("true".equals(migrated.settingsSnapshot().getProperty("enabled")),"Migration keeps enabled");
        ok("false".equals(migrated.settingsSnapshot().getProperty("protectRod")),"Migration keeps protection preference");
        Files.write(legacy,"enabled=true\nrecastDelayMs=800\n".getBytes("ISO-8859-1"));
        ok("800".equals(new AutoFishHandler().settingsSnapshot().getProperty("recastDelayMs")),"Custom old delay preserved");
        Files.write(legacy,"enabled=true\nconfigVersion=2\nrecastDelayMs=2000\n".getBytes("ISO-8859-1"));
        ok("2000".equals(new AutoFishHandler().settingsSnapshot().getProperty("recastDelayMs")),"Intentional new 2s setting preserved");
        System.out.println("PASS: "+checks+" compiled-handler integration checks (Minecraft fixtures)");
    }
    static void sound(final float x,final float y,final float z) {
        net.minecraft.client.audio.ISound s=new net.minecraft.client.audio.ISound() {
            public net.minecraft.util.ResourceLocation func_147650_b(){return new net.minecraft.util.ResourceLocation("minecraft:entity.bobber.splash");}
            public float func_147649_g(){return x;}public float func_147654_h(){return y;}public float func_147651_i(){return z;}
        };
        handler.onPlaySound(new net.minecraftforge.client.event.sound.PlaySoundEvent(s));
    }
}
