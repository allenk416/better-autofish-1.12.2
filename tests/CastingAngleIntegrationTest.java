import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;
import com.northwestwind.forgeautofish.handler.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Runs the compiled handler against fixtures, including the packet/use-item ordering. */
public class CastingAngleIntegrationTest {
    static int checks;
    static Minecraft mc=Minecraft.instance;
    static AutoFishHandler handler;
    static void ok(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    static Object get(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    static void tick(){handler.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));}
    static void cast()throws Exception {
        Object policy=get(handler,"policy");set(policy,"readyLastTick",true);set(policy,"castAllowedAt",0L);tick();
    }
    static void apply(String... pairs){
        Properties p=new Properties();for(int i=0;i<pairs.length;i+=2)p.setProperty(pairs[i],pairs[i+1]);
        ok(handler.applySettings(p),"Settings save");
    }
    static GuiButton button(FishingSettingsScreen s,int id){
        for(GuiButton b:s.field_146292_n)if(b.field_146127_k==id)return b;
        throw new AssertionError("No button "+id);
    }
    static void click(FishingSettingsScreen s,int id)throws Exception {
        Method m=FishingSettingsScreen.class.getDeclaredMethod("func_146284_a",GuiButton.class);
        m.setAccessible(true);m.invoke(s,button(s,id));
    }
    static GuiTextField[] fields(FishingSettingsScreen s)throws Exception {return (GuiTextField[])get(s,"fields");}
    public static void main(String[] args)throws Exception {
        Path root=Paths.get("build/test-data");Files.createDirectories(root);
        mc.field_71412_D=Files.createTempDirectory(root,"angles-").toFile();
        mc.field_71439_g=new EntityPlayerSP();mc.field_71441_e=new WorldClient();
        mc.field_71439_g.main.item=new ItemFishingRod();
        handler=new AutoFishHandler();apply("enabled","true");tick();
        ok("false".equals(handler.settingsSnapshot().getProperty("fixCastPitch")),"Old configs keep pitch control off");
        ok("false".equals(handler.settingsSnapshot().getProperty("fixCastYaw")),"Old configs keep yaw control off");
        mc.field_71439_g.field_70125_A=7;mc.field_71439_g.field_70177_z=23;cast();
        ok(mc.field_71439_g.field_70125_A==7 && mc.field_71439_g.field_70177_z==23,"Disabled angles preserve current view");
        ok(mc.field_71439_g.field_71174_a.packets.isEmpty(),"No rotation packet when controls are off");
        apply("fixCastPitch","true","castPitch","60");cast();
        ok(mc.field_71439_g.field_70125_A==60 && mc.field_71439_g.field_70177_z==23,"Pitch-only cast preserves heading");
        CPacketPlayer packet=(CPacketPlayer)mc.field_71439_g.field_71174_a.packets.get(0);
        ok(packet.pitch==60 && packet.yaw==23,"Rotation packet uses desired pitch and actual heading");
        ok(mc.field_71442_b.packetsAtUse==1 && mc.field_71442_b.pitchAtUse==60,"Server rotation is sent before use-item");
        apply("fixCastPitch","false","fixCastYaw","true","castYaw","-135");
        mc.field_71439_g.field_70125_A=11;cast();
        ok(mc.field_71439_g.field_70125_A==11 && mc.field_71439_g.field_70177_z== -135,"Heading-only cast preserves pitch");
        apply("fixCastPitch","true","castPitch","52.5");
        mc.field_71439_g.field_70125_A=0;mc.field_71439_g.field_70177_z=0;
        mc.field_71462_r=new GuiScreen();GuiScreen original=mc.field_71462_r;cast();
        ok(mc.field_71439_g.field_70125_A==52.5f && mc.field_71439_g.field_70177_z== -135,"Restores angles after teleport resets the view");
        ok(mc.field_71462_r==original,"Casting preserves the open GUI");
        EntityFishHook hook=new EntityFishHook();hook.id=17;hook.owner=mc.field_71439_g;hook.field_70170_p=mc.field_71441_e;
        mc.field_71441_e.field_72996_f.add(hook);mc.field_71439_g.field_71104_cf=hook;
        mc.field_71439_g.field_70125_A=4;mc.field_71439_g.field_70177_z=9;
        int before=mc.field_71439_g.field_71174_a.packets.size();tick();
        ok(mc.field_71439_g.field_70125_A==4 && mc.field_71439_g.field_70177_z==9,"Waiting does not lock the camera");
        hook.field_70181_x= -0.1;tick();
        ok(mc.field_71439_g.field_71174_a.packets.size()==before,"Reeling does not send angle overrides");
        ok(mc.field_71439_g.field_70125_A==4,"Reeling preserves the player's view");
        // A reconnect creates a new player/connection, but persisted angle preferences remain.
        mc.field_71439_g=null;mc.field_71441_e=null;tick();
        mc.field_71439_g=new EntityPlayerSP();mc.field_71441_e=new WorldClient();
        mc.field_71439_g.off.item=new ItemFishingRod();tick();cast();
        ok(mc.field_71439_g.field_70125_A==52.5f && mc.field_71439_g.field_70177_z== -135,"Reconnect restores saved angles on first cast");
        ok(mc.field_71442_b.lastHand==net.minecraft.util.EnumHand.OFF_HAND,"Angles also work with an off-hand rod");
        AutoFishHandler reloaded=new AutoFishHandler();
        ok("52.5".equals(reloaded.settingsSnapshot().getProperty("castPitch")),"Pitch persists across client restarts");
        ok("true".equals(reloaded.settingsSnapshot().getProperty("fixCastYaw")),"Heading switch persists");
        apply("castPitch","NaN","castYaw","Infinity");
        ok("60.0".equals(handler.settingsSnapshot().getProperty("castPitch")),"Non-finite pitch recovers to default");
        ok("0.0".equals(handler.settingsSnapshot().getProperty("castYaw")),"Non-finite yaw recovers to default");
        apply("castPitch","999","castYaw","-999");
        ok("90.0".equals(handler.settingsSnapshot().getProperty("castPitch")),"Pitch file setting is clamped");
        ok("-180.0".equals(handler.settingsSnapshot().getProperty("castYaw")),"Heading file setting is clamped");
        I18n.locale("en_us");FishingSettingsScreen screen=new FishingSettingsScreen(handler,null);screen.func_73866_w_();
        fields(screen)[0].func_146180_a("800");click(screen,3);screen.func_73863_a(0,0,0);
        ok(screen.drawn.contains("Pitch (-90 to 90)"),"Angle page has English labels");
        fields(screen)[0].func_146180_a("48.25");fields(screen)[1].func_146180_a("-75");
        screen.func_73866_w_();ok("48.25".equals(fields(screen)[0].func_146179_b()),"Resizing preserves unsaved angles");
        click(screen,3);ok("800".equals(fields(screen)[0].func_146179_b()),"Page changes preserve unsaved timings");
        click(screen,0);ok("48.25".equals(handler.settingsSnapshot().getProperty("castPitch")),"Saving timing page also saves angle page");
        click(screen,3);fields(screen)[0].func_146180_a("91");click(screen,0);
        ok("48.25".equals(handler.settingsSnapshot().getProperty("castPitch")),"GUI rejects out-of-range pitch");
        fields(screen)[0].func_146180_a("45");fields(screen)[1].func_146180_a("NaN");click(screen,0);
        ok("-75.0".equals(handler.settingsSnapshot().getProperty("castYaw")),"GUI rejects non-finite heading");
        mc.field_71439_g.field_70125_A=37.5f;mc.field_71439_g.field_70177_z=725;
        before=mc.field_71439_g.field_71174_a.packets.size();click(screen,15);
        ok("37.5".equals(fields(screen)[0].func_146179_b()) && "5.0".equals(fields(screen)[1].func_146179_b()),"Capture reads pitch and normalizes heading");
        ok(mc.field_71439_g.field_71174_a.packets.size()==before,"Reading view does not move the camera or send packets");
        ok("48.25".equals(handler.settingsSnapshot().getProperty("castPitch")),"Capture waits for Save before applying");
        click(screen,0);ok("true".equals(handler.settingsSnapshot().getProperty("fixCastPitch")),"Capture enables pitch on Save");
        I18n.locale("zh_cn");screen=new FishingSettingsScreen(handler,null);screen.func_73866_w_();click(screen,3);screen.func_73863_a(0,0,0);
        ok(screen.drawn.contains("俯仰角 (-90 至 90)"),"Angle page has Chinese labels");
        ok("读取当前视角（启用两个角度）".equals(button(screen,15).field_146126_j),"Capture button is translated");
        for(GuiButton b:screen.field_146292_n)ok(b.x>=0 && b.x+b.width<=320 && b.y+b.height<=240,"Buttons fit minimum GUI size");
        mc.field_71439_g=null;click(screen,15);
        ok("gui.forgeautofish.status.noPlayer".equals(get(screen,"statusKey")),"Capture safely handles a disconnect");
        click(screen,2);click(screen,0);
        ok("false".equals(handler.settingsSnapshot().getProperty("fixCastPitch")) && "false".equals(handler.settingsSnapshot().getProperty("fixCastYaw")),"Reset turns angle overrides off");
        System.out.println("PASS: "+checks+" casting-angle, packet-order, persistence and GUI checks");
    }
}
