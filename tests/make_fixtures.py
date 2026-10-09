"""Minimal runtime fixtures: execute the REAL compiled handler, without connecting to a server."""
from pathlib import Path
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--output',type=Path,default=Path(__file__).resolve().parent.parent/'build/fixtures-src')
root=parser.parse_args().output
files={
'net/minecraft/entity/Entity.java': '''package net.minecraft.entity;
public class Entity { public boolean field_70128_L; public net.minecraft.world.World field_70170_p;
public double field_70159_w,field_70179_y,field_70181_x,field_70165_t,field_70163_u,field_70161_v; public int id; public int func_145782_y(){return id;} }''',
'net/minecraft/entity/player/EntityPlayer.java': '''package net.minecraft.entity.player;
public class EntityPlayer extends net.minecraft.entity.Entity {
public net.minecraft.entity.projectile.EntityFishHook field_71104_cf;
public net.minecraft.item.ItemStack main=new net.minecraft.item.ItemStack(),off=new net.minecraft.item.ItemStack();
public net.minecraft.item.ItemStack func_184614_ca(){return main;} public net.minecraft.item.ItemStack func_184592_cb(){return off;}
public net.minecraft.item.ItemStack func_184586_b(net.minecraft.util.EnumHand hand){return hand==net.minecraft.util.EnumHand.MAIN_HAND?main:off;}
public boolean func_70089_S(){return !field_70128_L;} public boolean func_175149_v(){return false;}
public String lastMessage;
public void func_146105_b(net.minecraft.util.text.ITextComponent text,boolean actionbar){lastMessage=text.func_150254_d();} }''',
'net/minecraft/client/entity/EntityPlayerSP.java': 'package net.minecraft.client.entity; public class EntityPlayerSP extends net.minecraft.entity.player.EntityPlayer {}',
'net/minecraft/entity/projectile/EntityFishHook.java': '''package net.minecraft.entity.projectile;
public class EntityFishHook extends net.minecraft.entity.Entity {
public net.minecraft.entity.player.EntityPlayer owner; public boolean wet=true;
public net.minecraft.entity.player.EntityPlayer func_190619_l(){return owner;} public boolean func_70090_H(){return wet;} }''',
'net/minecraft/world/World.java': '''package net.minecraft.world;
public class World { public java.util.List<net.minecraft.entity.Entity> field_72996_f=new java.util.ArrayList<>();
public net.minecraft.entity.Entity func_73045_a(int id){for(net.minecraft.entity.Entity e:field_72996_f)if(e.id==id)return e;return null;} }''',
'net/minecraft/client/multiplayer/WorldClient.java': 'package net.minecraft.client.multiplayer; public class WorldClient extends net.minecraft.world.World {}',
'net/minecraft/client/multiplayer/PlayerControllerMP.java': '''package net.minecraft.client.multiplayer;
public class PlayerControllerMP { public int calls; public net.minecraft.util.EnumHand lastHand;
public net.minecraft.util.EnumActionResult func_187101_a(net.minecraft.entity.player.EntityPlayer p,net.minecraft.world.World w,net.minecraft.util.EnumHand hand){calls++;lastHand=hand;return net.minecraft.util.EnumActionResult.SUCCESS;} }''',
'net/minecraft/client/gui/GuiScreen.java': '''package net.minecraft.client.gui;
public class GuiScreen { protected net.minecraft.client.Minecraft field_146297_k=net.minecraft.client.Minecraft.instance;
public int field_146294_l=320,field_146295_m=240; public java.util.List<GuiButton> field_146292_n=new java.util.ArrayList<>();
protected FontRenderer field_146289_q=new FontRenderer();
public void func_73866_w_(){} public void func_146276_q_(){} protected void func_146284_a(GuiButton b){}
protected void func_73864_a(int x,int y,int b){} protected void func_73869_a(char c,int key){}
public void func_73863_a(int x,int y,float t){} public void func_73876_c(){} public boolean func_73868_f(){return true;}
public java.util.List<String> drawn=new java.util.ArrayList<>();
public void func_73732_a(FontRenderer f,String s,int x,int y,int color){drawn.add(s);} public void func_73731_b(FontRenderer f,String s,int x,int y,int color){drawn.add(s);} }''',
'net/minecraft/client/gui/FontRenderer.java': 'package net.minecraft.client.gui; public class FontRenderer {}',
'net/minecraft/client/gui/GuiButton.java': '''package net.minecraft.client.gui;
public class GuiButton { public int field_146127_k,x,y,width,height;public String field_146126_j;
public GuiButton(int id,int x,int y,int w,int h,String text){field_146127_k=id;this.x=x;this.y=y;this.width=w;this.height=h;field_146126_j=text;} }''',
'net/minecraft/client/gui/GuiTextField.java': '''package net.minecraft.client.gui;
public class GuiTextField { public int x,y,width,height;private String text="";private boolean focused;
public GuiTextField(int id,FontRenderer f,int x,int y,int w,int h){this.x=x;this.y=y;this.width=w;this.height=h;}
public void func_146203_f(int max){} public void func_146180_a(String s){text=s;}public String func_146179_b(){return text;}
public void func_146194_f(){} public boolean func_146192_a(int x,int y,int b){return false;}
public boolean func_146201_a(char c,int key){return false;}public boolean func_146206_l(){return focused;}
public void func_146195_b(boolean b){focused=b;}public void func_146178_a(){} }''',
'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
public class Minecraft { public static final Minecraft instance=new Minecraft(); public java.io.File field_71412_D;
public net.minecraft.client.entity.EntityPlayerSP field_71439_g; public net.minecraft.client.multiplayer.WorldClient field_71441_e;
public net.minecraft.client.multiplayer.PlayerControllerMP field_71442_b=new net.minecraft.client.multiplayer.PlayerControllerMP();
public net.minecraft.client.gui.GuiScreen field_71462_r; public boolean paused;
public static Minecraft func_71410_x(){return instance;} public boolean func_147113_T(){return paused;}
public void func_147108_a(net.minecraft.client.gui.GuiScreen screen){field_71462_r=screen;if(screen!=null)screen.func_73866_w_();} }''',
'net/minecraft/item/Item.java': 'package net.minecraft.item; public class Item {}',
'net/minecraft/item/ItemFishingRod.java': 'package net.minecraft.item; public class ItemFishingRod extends Item {}',
'net/minecraft/item/ItemStack.java': '''package net.minecraft.item;
public class ItemStack { public static final ItemStack field_190927_a=new ItemStack(); public Item item=new Item();public int damage;
public Item func_77973_b(){return item;} public boolean func_77984_f(){return item instanceof ItemFishingRod;}
public int func_77958_k(){return 64;} public int func_77952_i(){return damage;} }''',
'net/minecraft/util/EnumHand.java': 'package net.minecraft.util; public enum EnumHand { MAIN_HAND, OFF_HAND }',
'net/minecraft/util/EnumActionResult.java': 'package net.minecraft.util; public enum EnumActionResult { SUCCESS, PASS, FAIL }',
'net/minecraft/util/text/ITextComponent.java': 'package net.minecraft.util.text; public interface ITextComponent {String func_150254_d();}',
'net/minecraft/util/text/TextComponentString.java': 'package net.minecraft.util.text; public class TextComponentString implements ITextComponent {final String text;public TextComponentString(String text){this.text=text;}public String func_150254_d(){return text;}}',
'net/minecraft/util/text/TextComponentTranslation.java': '''package net.minecraft.util.text;
public class TextComponentTranslation implements ITextComponent {final String key;final Object[] args;
public TextComponentTranslation(String key,Object... args){this.key=key;this.args=args;}
public String func_150254_d(){return net.minecraft.client.resources.I18n.func_135052_a(key,args);} }''',
'net/minecraft/client/settings/KeyBinding.java': '''package net.minecraft.client.settings;
public class KeyBinding {public boolean pending;public String description;public int code;
public KeyBinding(String description,int code,String category){this.description=description;this.code=code;}
public boolean func_151468_f(){boolean p=pending;pending=false;return p;}
public void func_151462_b(int value){code=value;}public static void func_74508_b(){} }''',
'net/minecraftforge/fml/client/registry/ClientRegistry.java': '''package net.minecraftforge.fml.client.registry;
public class ClientRegistry {public static void registerKeyBinding(net.minecraft.client.settings.KeyBinding binding){} }''',
'net/minecraft/client/resources/I18n.java': '''package net.minecraft.client.resources;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class I18n {static Map<String,String> translations=new HashMap<>();static {locale("en_us");}
public static void locale(String lang){translations.clear();load("en_us");if(!"en_us".equals(lang))load(lang);}
static void load(String lang){InputStream in=I18n.class.getClassLoader().getResourceAsStream("assets/forgeautofish/lang/"+lang+".lang");
if(in==null)return;try(BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;
while((line=reader.readLine())!=null){if(line.startsWith("#"))continue;int sep=line.indexOf('=');if(sep>0)translations.put(line.substring(0,sep),line.substring(sep+1));}}
catch(IOException e){throw new RuntimeException(e);}}
public static String func_135052_a(String key,Object... args){return String.format(Locale.ROOT,translations.containsKey(key)?translations.get(key):key,args);} }''',
'net/minecraft/util/ResourceLocation.java': 'package net.minecraft.util;public class ResourceLocation {final String name;public ResourceLocation(String name){this.name=name;}public String toString(){return name;}}',
'net/minecraft/client/audio/ISound.java': 'package net.minecraft.client.audio;public interface ISound {net.minecraft.util.ResourceLocation func_147650_b();float func_147649_g();float func_147654_h();float func_147651_i();}',
'net/minecraftforge/client/event/sound/SoundEvent.java': 'package net.minecraftforge.client.event.sound;public class SoundEvent {}',
'net/minecraftforge/client/event/sound/PlaySoundEvent.java': 'package net.minecraftforge.client.event.sound;public class PlaySoundEvent extends SoundEvent {final net.minecraft.client.audio.ISound sound;public PlaySoundEvent(net.minecraft.client.audio.ISound sound){this.sound=sound;}public net.minecraft.client.audio.ISound getSound(){return sound;}}',
'net/minecraftforge/fml/common/gameevent/InputEvent.java': 'package net.minecraftforge.fml.common.gameevent; public class InputEvent {public static class KeyInputEvent extends InputEvent {}}',
'net/minecraftforge/fml/common/gameevent/TickEvent.java': 'package net.minecraftforge.fml.common.gameevent; public class TickEvent {public enum Phase {START,END} public Phase phase; public static class ClientTickEvent extends TickEvent {public ClientTickEvent(Phase p){phase=p;}}}',
'net/minecraftforge/fml/relauncher/Side.java': 'package net.minecraftforge.fml.relauncher; public enum Side { CLIENT, SERVER }',
'net/minecraftforge/fml/relauncher/SideOnly.java': 'package net.minecraftforge.fml.relauncher; @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS) @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE,java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.CONSTRUCTOR,java.lang.annotation.ElementType.FIELD}) public @interface SideOnly { Side value(); }',
}
for name,source in files.items():
    path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source,encoding='utf-8')
print(f'Wrote {len(files)} fixture classes; they will NOT be packaged in the mod.')
