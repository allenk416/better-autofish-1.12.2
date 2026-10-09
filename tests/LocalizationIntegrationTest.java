import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import com.northwestwind.forgeautofish.handler.*;
import com.northwestwind.forgeautofish.keybind.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class LocalizationIntegrationTest {
    private static int checks;
    private static void ok(boolean pass,String what){checks++;if(!pass)throw new AssertionError(what);}
    private static Object get(Object owner,String name)throws Exception{
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }
    private static GuiButton button(FishingSettingsScreen screen,int id){
        for(GuiButton b:screen.field_146292_n)if(b.field_146127_k==id)return b;
        throw new AssertionError("Missing button "+id);
    }
    public static void main(String[] args)throws Exception {
        Minecraft mc=Minecraft.instance;
        Path base=Paths.get("build/test-data");Files.createDirectories(base);
        mc.field_71412_D=Files.createTempDirectory(base,"localization-").toFile();
        mc.field_71439_g=new EntityPlayerSP();
        KeyBinds.register();
        ok(KeyBinds.autofish.code==65 && KeyBinds.settings.code==68,"F7/F10 defaults");
        ok("key.forgeautofish.autofish".equals(KeyBinds.autofish.description),"Toggle description is a translation key");
        ok("key.forgeautofish.settings".equals(KeyBinds.settings.description),"Settings description is a translation key");
        Files.write(mc.field_71412_D.toPath().resolve("options.txt"),
            "key_Toggle AutoFish:66\nkey_自动钓鱼参数设置:67\n".getBytes(StandardCharsets.UTF_8));
        KeyBinds.register();
        ok(KeyBinds.autofish.code==66 && KeyBinds.settings.code==67,"Legacy custom bindings migrate");
        Files.write(mc.field_71412_D.toPath().resolve("options.txt"),
            "key_Toggle AutoFish:66\nkey_自动钓鱼参数设置:67\nkey_key.forgeautofish.autofish:60\nkey_key.forgeautofish.settings:61\n".getBytes(StandardCharsets.UTF_8));
        KeyBinds.register();
        ok(KeyBinds.autofish.code==60 && KeyBinds.settings.code==61,"Current bindings take precedence over legacy bindings");
        AutoFishHandler handler=new AutoFishHandler();
        I18n.locale("en_us");
        FishingSettingsScreen screen=new FishingSettingsScreen(handler,null);screen.func_73866_w_();screen.func_73863_a(0,0,0);
        ok("Save & apply".equals(button(screen,0).field_146126_j),"English save button");
        ok(screen.drawn.contains("Better AutoFish - Settings"),"English settings title");
        ok(screen.drawn.contains("No-bite timeout (ms)"),"English field label");
        KeyBinds.autofish.pending=true;handler.onKeyInput(new InputEvent.KeyInputEvent());
        ok(mc.field_71439_g.lastMessage.contains("AutoFish enabled"),"English toggle message");
        I18n.locale("zh_cn");
        screen=new FishingSettingsScreen(handler,null);screen.func_73866_w_();screen.func_73863_a(0,0,0);
        ok("保存并生效".equals(button(screen,0).field_146126_j),"Chinese save button");
        ok(screen.drawn.contains("Better AutoFish · 参数设置"),"Chinese settings title");
        ok(screen.drawn.contains("无咬钩超时 (毫秒)"),"Chinese field label");
        KeyBinds.autofish.pending=true;handler.onKeyInput(new InputEvent.KeyInputEvent());
        ok(mc.field_71439_g.lastMessage.contains("已关闭"),"Chinese toggle message");
        GuiTextField[] fields=(GuiTextField[])get(screen,"fields");fields[0].func_146180_a("NaN");
        Method click=FishingSettingsScreen.class.getDeclaredMethod("func_146284_a",GuiButton.class);click.setAccessible(true);
        click.invoke(screen,button(screen,0));screen.drawn.clear();screen.func_73863_a(0,0,0);
        ok(screen.drawn.contains("重抛等待 (毫秒)：请输入 100 至 10000。"),"Chinese validation uses translated label and bounds");
        I18n.locale("fr_fr");
        screen=new FishingSettingsScreen(handler,null);screen.func_73866_w_();
        ok("Save & apply".equals(button(screen,0).field_146126_j),"Unsupported locale falls back to English");
        Path blocked=Files.createDirectory(mc.field_71412_D.toPath().resolve("blocked-config"));
        Field config=AutoFishHandler.class.getDeclaredField("configFile");config.setAccessible(true);config.set(handler,blocked.toFile());
        click.invoke(screen,button(screen,0));
        ok("gui.forgeautofish.status.saveFailed".equals(get(screen,"statusKey")),"A failed file save is reported instead of false success");
        screen.drawn.clear();screen.func_73863_a(0,0,0);
        ok(screen.drawn.contains("Applied, but not saved. Check the game log."),"English save failure explanation");
        System.out.println("PASS: "+checks+" localization, binding migration and save-failure checks");
    }
}
