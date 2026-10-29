// Based on AutoFish 1.0.4 for Minecraft 1.12.2 by NorthWestWind.
// Modified by allenk416, 2026-10-09; see NOTICE.md for the modification notice.
// See NOTICE.md and LICENSE for provenance and license information.
package com.northwestwind.forgeautofish.handler;

import com.northwestwind.forgeautofish.keybind.KeyBinds;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

/** Client-thread replacement for the original 1.0.4 handler. Uses runtime SRG names. */
@SideOnly(Side.CLIENT)
public class AutoFishHandler {
    private boolean enabled;
    private boolean protectRod=true;
    private boolean soundDetection=true;
    private boolean fixCastPitch, fixCastYaw;
    private float castPitch=60, castYaw;
    private long recastDelay=500;
    private long ghostTimeout=3000;
    private double motionThreshold=0.05, soundRadius=1.0;
    private FishingPolicy policy;
    private Object sessionWorld, sessionPlayer;
    private EntityFishHook retiredHook;
    private long retireAt;
    private final File configFile;
    private final Properties config=new Properties();
    private final ConcurrentLinkedQueue<BiteSignal> biteSignals=new ConcurrentLinkedQueue<BiteSignal>();

    private static final class BiteSignal {
        final Object world; final long time; final double x,y,z;
        BiteSignal(Object world,long time,double x,double y,double z) {
            this.world=world; this.time=time; this.x=x; this.y=y; this.z=z;
        }
    }

    public AutoFishHandler() {
        configFile=new File(Minecraft.func_71410_x().field_71412_D,"config/autofish-recovery.properties");
        if(configFile.isFile()) {
            try(InputStream in=new FileInputStream(configFile)) { config.load(in); }
            catch(IOException e) { System.err.println("[Better AutoFish] Cannot read config: "+e); }
        }
        enabled=Boolean.parseBoolean(config.getProperty("enabled","false"));
        // Upgrade only the old default, preserving the user's other settings and enabled state.
        if(!"2".equals(config.getProperty("configVersion")) && "2000".equals(config.getProperty("recastDelayMs")))
            config.setProperty("recastDelayMs","500");
        readSettings();
        saveConfig();
        System.out.println("[Better AutoFish] Loaded Better AutoFish; enabled="+enabled);
    }

    private void readSettings() {
        protectRod=Boolean.parseBoolean(config.getProperty("protectRod","true"));
        soundDetection=Boolean.parseBoolean(config.getProperty("soundDetection","true"));
        fixCastPitch=Boolean.parseBoolean(config.getProperty("fixCastPitch","false"));
        fixCastYaw=Boolean.parseBoolean(config.getProperty("fixCastYaw","false"));
        config.setProperty("fixCastPitch",Boolean.toString(fixCastPitch));
        config.setProperty("fixCastYaw",Boolean.toString(fixCastYaw));
        castPitch=(float)decimal("castPitch",60,-90,90);
        castYaw=(float)decimal("castYaw",0,-180,180);
        recastDelay=number("recastDelayMs",500,100,10000);
        ghostTimeout=number("ghostTimeoutMs",3000,1000,10000);
        motionThreshold=decimal("motionThreshold",0.05,0.01,1.0);
        soundRadius=decimal("soundRadius",1.0,0.1,5.0);
        long startup=number("startupDelayMs",5000,1000,60000);
        long retry=number("castRetryMs",5000,2000,60000);
        long maxWait=number("hookTimeoutMs",10000,6000,600000);
        long dryWait=number("dryHookTimeoutMs",20000,10000,60000);
        if(policy==null) policy=new FishingPolicy(startup,retry,recastDelay,maxWait,dryWait);
        else policy.configure(startup,retry,recastDelay,maxWait,dryWait);
    }

    private double decimal(String name,double fallback,double min,double max) {
        double value=fallback;
        try { value=Double.parseDouble(config.getProperty(name,Double.toString(fallback))); }
        catch(NumberFormatException ignored) { }
        if(Double.isNaN(value) || Double.isInfinite(value)) value=fallback;
        value=Math.max(min,Math.min(max,value)); config.setProperty(name,Double.toString(value));
        return value;
    }

    public Properties settingsSnapshot() {
        Properties copy=new Properties(); copy.putAll(config);
        copy.setProperty("enabled",Boolean.toString(enabled)); return copy;
    }

    public boolean applySettings(Properties edited) {
        boolean wasEnabled=enabled;
        config.putAll(edited); enabled=Boolean.parseBoolean(config.getProperty("enabled","false"));
        readSettings();
        if(wasEnabled!=enabled) { policy.reset(); biteSignals.clear(); }
        if(!enabled) { retiredHook=null; biteSignals.clear(); }
        return saveConfig();
    }

    private long number(String name,long fallback,long min,long max) {
        long value=fallback;
        try { value=Long.parseLong(config.getProperty(name,Long.toString(fallback))); }
        catch(NumberFormatException ignored) { }
        value=Math.max(min,Math.min(max,value)); config.setProperty(name,Long.toString(value));
        return value;
    }

    private boolean saveConfig() {
        config.setProperty("enabled",Boolean.toString(enabled));
        config.setProperty("protectRod",Boolean.toString(protectRod));
        config.setProperty("soundDetection",Boolean.toString(soundDetection));
        config.setProperty("configVersion","2");
        try {
            File parent=configFile.getParentFile();
            if(!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create "+parent);
            try(OutputStream out=new FileOutputStream(configFile)) {
                config.store(out,"Better AutoFish for Minecraft 1.12.2; F7 toggle, F10 settings. GUI/background fishing enabled.");
            }
            return true;
        } catch(IOException e) {
            System.err.println("[Better AutoFish] Cannot save config: "+e);
            return false;
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc=Minecraft.func_71410_x();
        if(KeyBinds.autofish!=null && KeyBinds.autofish.func_151468_f()) {
            enabled=!enabled; policy.reset(); retiredHook=null; biteSignals.clear(); saveConfig();
            if(mc.field_71439_g!=null) mc.field_71439_g.func_146105_b(
                    new TextComponentTranslation(enabled ? "message.forgeautofish.enabled" : "message.forgeautofish.disabled"),true);
        }
        if(KeyBinds.settings!=null && KeyBinds.settings.func_151468_f())
            mc.func_147108_a(new FishingSettingsScreen(this,mc.field_71462_r));
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound=event.getSound();
        if(sound==null) return;
        String name=sound.func_147650_b().toString();
        if(!"minecraft:entity.bobber.splash".equals(name) && !"entity.bobber.splash".equals(name)) return;
        // Sound callbacks only enqueue data; all entity reads/actions stay on the client tick.
        if(biteSignals.size()>=32) biteSignals.poll();
        biteSignals.offer(new BiteSignal(Minecraft.func_71410_x().field_71441_e,System.nanoTime()/1000000L,
                sound.func_147649_g(),sound.func_147654_h(),sound.func_147651_i()));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.func_71410_x();
        if(sessionWorld!=mc.field_71441_e || sessionPlayer!=mc.field_71439_g) {
            sessionWorld=mc.field_71441_e; sessionPlayer=mc.field_71439_g;
            retiredHook=null; policy.reset(); biteSignals.clear();
        }
        long now=System.nanoTime()/1000000L;
        EntityPlayer player=mc.field_71439_g;
        World world=mc.field_71441_e;
        if(!enabled || player==null || world==null || mc.field_71442_b==null) {
            policy.tick(now,false,Long.MIN_VALUE,false,false); biteSignals.clear(); return;
        }
        // A reel packet may have removed the server hook without cleaning up a client ghost.
        // Retire only the hook we already reeled, after giving the server time to reply.
        if(retiredHook!=null) {
            boolean removed=retiredHook.field_70128_L || world.func_73045_a(retiredHook.func_145782_y())!=retiredHook;
            if(removed || now>=retireAt) {
                if(!removed) retiredHook.field_70128_L=true;
                if(player.field_71104_cf==retiredHook) player.field_71104_cf=null;
                retiredHook=null;
            }
        }
        EnumHand hand=fishingHand(player);
        ItemStack stack=hand==null?ItemStack.field_190927_a:player.func_184586_b(hand);
        boolean ready=hand!=null && !mc.func_147113_T()
                && !player.field_70128_L && player.func_70089_S() && !player.func_175149_v()
                && (!protectRod || !stack.func_77984_f() || stack.func_77958_k()-stack.func_77952_i()>2);
        if(!ready) { policy.tick(now,false,Long.MIN_VALUE,false,false); biteSignals.clear(); return; }
        EntityFishHook hook=player.field_71104_cf;
        if(hook!=null && (hook.field_70128_L || hook.field_70170_p!=world
                || world.func_73045_a(hook.func_145782_y())!=hook)) {
            player.field_71104_cf=null; hook=null;
        }
        // Repair the item model/reference when a live own hook still exists in the world.
        if(hook==null) {
            for(Entity entity:world.field_72996_f) {
                if(entity instanceof EntityFishHook && !entity.field_70128_L && entity!=retiredHook) {
                    EntityFishHook candidate=(EntityFishHook)entity;
                    if(candidate.func_190619_l()==player && (hook==null || candidate.func_145782_y()>hook.func_145782_y())) hook=candidate;
                }
            }
            if(hook!=null) player.field_71104_cf=hook;
        }
        // A short recast delay must not become permission to use a hook awaiting retraction.
        if(retiredHook!=null && (hook==null || hook==retiredHook)) { biteSignals.clear(); return; }
        boolean inWater=hook!=null && hook.func_70090_H();
        boolean bite=hook!=null && inWater && hook.field_70181_x < -motionThreshold
                && Math.abs(hook.field_70159_w)<0.01 && Math.abs(hook.field_70179_y)<0.01;
        BiteSignal signal;
        while((signal=biteSignals.poll())!=null) {
            if(!soundDetection || hook==null || signal.world!=world || now-signal.time<0 || now-signal.time>1000) continue;
            double distance=distanceSquared(hook,signal);
            if(distance>soundRadius*soundRadius) continue;
            boolean closerOtherHook=false;
            for(Entity other:world.field_72996_f) {
                if(other instanceof EntityFishHook && other!=hook && !other.field_70128_L
                        && ((EntityFishHook)other).func_190619_l()!=player
                        && distanceSquared(other,signal)<distance) { closerOtherHook=true; break; }
            }
            if(!closerOtherHook) bite=true;
        }
        FishingPolicy.Action action=policy.tick(now,true,
                hook==null?Long.MIN_VALUE:hook.func_145782_y(),bite,inWater);
        if(action==FishingPolicy.Action.NONE) return;
        // Rotation must reach the server before use-item: fishing hooks are launched server-side.
        // Only adjust a real cast, leaving waiting, reeling, GUIs and the OS mouse alone.
        if(action==FishingPolicy.Action.CAST && (fixCastPitch || fixCastYaw)) {
            if(fixCastPitch) player.field_70125_A=player.field_70127_C=castPitch;
            if(fixCastYaw) player.field_70177_z=player.field_70126_B=castYaw;
            mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayer.Rotation(
                    player.field_70177_z,player.field_70125_A,player.field_70122_E));
        }
        // Normal controller path synchronizes the selected slot, sends the packet and runs
        // local item logic; it also respects Forge's normal use-item hooks.
        EnumActionResult result=mc.field_71442_b.func_187101_a(player,world,hand);
        if(result==EnumActionResult.SUCCESS && action==FishingPolicy.Action.REEL && hook!=null) {
            retiredHook=hook; retireAt=now+ghostTimeout;
        }
        if(result!=EnumActionResult.SUCCESS) policy.actionFailed(now,action);
    }

    private static double distanceSquared(Entity entity,BiteSignal signal) {
        double dx=entity.field_70165_t-signal.x,dy=entity.field_70163_u-signal.y,dz=entity.field_70161_v-signal.z;
        return dx*dx+dy*dy+dz*dz;
    }

    private static EnumHand fishingHand(EntityPlayer player) {
        if(player.func_184614_ca().func_77973_b() instanceof ItemFishingRod) return EnumHand.MAIN_HAND;
        if(player.func_184592_cb().func_77973_b() instanceof ItemFishingRod) return EnumHand.OFF_HAND;
        return null;
    }
}
