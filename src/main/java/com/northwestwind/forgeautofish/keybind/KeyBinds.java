// Based on AutoFish for Forge 1.12.2 by NorthWestWind.
// Modified by allenk416, 2026-10-09: settings binding, translation keys and binding migration.
// Licensed under GPL-3.0; see LICENSE and NOTICE.md.
package com.northwestwind.forgeautofish.keybind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class KeyBinds {
    public static KeyBinding autofish;
    public static KeyBinding settings;
    public static void register() {
        // Translation keys are stable across language changes and saved options.
        autofish=new KeyBinding("key.forgeautofish.autofish",65,"key.categories.forgeautofish");
        settings=new KeyBinding("key.forgeautofish.settings",68,"key.categories.forgeautofish");
        ClientRegistry.registerKeyBinding(autofish);
        ClientRegistry.registerKeyBinding(settings);
        migrateBindings();
    }

    private static void migrateBindings() {
        Path options=Minecraft.func_71410_x().field_71412_D.toPath().resolve("options.txt");
        if(!Files.isRegularFile(options)) return;
        Map<String,Integer> saved=new HashMap<String,Integer>();
        try {
            for(String line:Files.readAllLines(options,StandardCharsets.UTF_8)) {
                if(!line.startsWith("key_")) continue;
                int separator=line.lastIndexOf(':');
                if(separator<0) continue;
                try { saved.put(line.substring(4,separator),Integer.parseInt(line.substring(separator+1))); }
                catch(NumberFormatException ignored) { }
            }
            restore(autofish,saved,"key.forgeautofish.autofish","Toggle AutoFish");
            restore(settings,saved,"key.forgeautofish.settings","\u81ea\u52a8\u9493\u9c7c\u53c2\u6570\u8bbe\u7f6e");
            KeyBinding.func_74508_b();
        } catch(IOException error) {
            System.err.println("[Better AutoFish] Cannot read saved key bindings: "+error);
        }
    }

    private static void restore(KeyBinding binding,Map<String,Integer> saved,String current,String previous) {
        Integer code=saved.get(current);
        if(code==null) code=saved.get(previous);
        if(code!=null) binding.func_151462_b(code);
    }
}
