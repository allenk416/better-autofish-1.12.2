// New component for the derivative of NorthWestWind AutoFish for Forge 1.12.2.
// Modified by allenk416, 2026-10-09; see NOTICE.md for the modification notice.
// See NOTICE.md and LICENSE for provenance and license information.
package com.northwestwind.forgeautofish.handler;

import java.util.Properties;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

/** Two-column settings screen that itself leaves fishing and the mouse free. */
public class FishingSettingsScreen extends GuiScreen {
    private final AutoFishHandler owner;
    private final GuiScreen parent;
    private Properties values;
    private GuiTextField[] fields;
    private boolean enabled,protectRod,soundDetection,fixCastPitch,fixCastYaw;
    private int page;
    private String statusKey="gui.forgeautofish.status.ready";
    private Object[] statusArgs=new Object[0];
    private final String[] keys={"recastDelayMs","castRetryMs","startupDelayMs","hookTimeoutMs",
            "dryHookTimeoutMs","ghostTimeoutMs","motionThreshold","soundRadius","castPitch","castYaw"};
    private final String[] defaults={"500","5000","5000","10000","20000","3000","0.05","1.0","60","0"};
    private final double[] min={100,2000,1000,6000,10000,1000,0.01,0.1,-90,-180};
    private final double[] max={10000,60000,60000,600000,60000,10000,1.0,5.0,90,180};

    public FishingSettingsScreen(AutoFishHandler owner,GuiScreen parent) {
        this.owner=owner; this.parent=parent; values=owner.settingsSnapshot();
        enabled=Boolean.parseBoolean(values.getProperty("enabled","false"));
        protectRod=Boolean.parseBoolean(values.getProperty("protectRod","true"));
        soundDetection=Boolean.parseBoolean(values.getProperty("soundDetection","true"));
        fixCastPitch=Boolean.parseBoolean(values.getProperty("fixCastPitch","false"));
        fixCastYaw=Boolean.parseBoolean(values.getProperty("fixCastYaw","false"));
    }

    private int fieldOffset() { return page==0?0:8; }
    private void rememberFields() {
        if(fields!=null) for(int i=0;i<fields.length;i++)
            values.setProperty(keys[fieldOffset()+i],fields[i].func_146179_b());
    }

    @Override public void func_73866_w_() {
        // Preserve typed-but-unsaved values when the window is resized.
        rememberFields();
        fields=new GuiTextField[page==0?8:2];
        for(int i=0;i<fields.length;i++) {
            int x=field_146294_l/2-145+(i%2)*150,y=42+(i/2)*31;
            fields[i]=new GuiTextField(i,field_146289_q,x,y+10,135,16);
            int index=fieldOffset()+i;
            fields[i].func_146203_f(12); fields[i].func_146180_a(values.getProperty(keys[index],defaults[index]));
        }
        field_146292_n.clear();
        field_146292_n.add(new GuiButton(10,field_146294_l/2-145,172,70,20,""));
        field_146292_n.add(new GuiButton(11,field_146294_l/2-70,172,70,20,""));
        field_146292_n.add(new GuiButton(12,field_146294_l/2+5,172,70,20,""));
        field_146292_n.add(new GuiButton(3,field_146294_l/2+80,172,70,20,
                tr(page==0?"gui.forgeautofish.anglesPage":"gui.forgeautofish.timingsPage")));
        if(page==1) {
            field_146292_n.add(new GuiButton(13,field_146294_l/2-145,88,145,20,""));
            field_146292_n.add(new GuiButton(14,field_146294_l/2+5,88,145,20,""));
            field_146292_n.add(new GuiButton(15,field_146294_l/2-145,120,295,20,tr("gui.forgeautofish.captureView")));
        }
        field_146292_n.add(new GuiButton(0,field_146294_l/2-145,field_146295_m-26,95,20,tr("gui.forgeautofish.save")));
        field_146292_n.add(new GuiButton(2,field_146294_l/2-45,field_146295_m-26,95,20,tr("gui.forgeautofish.reset")));
        field_146292_n.add(new GuiButton(1,field_146294_l/2+55,field_146295_m-26,95,20,tr("gui.forgeautofish.back")));
        updateButtons();
    }

    private void updateButtons() {
        for(GuiButton b:field_146292_n) {
            if(b.field_146127_k==10) b.field_146126_j=tr("gui.forgeautofish.fishing",state(enabled));
            if(b.field_146127_k==11) b.field_146126_j=tr("gui.forgeautofish.rodGuard",state(protectRod));
            if(b.field_146127_k==12) b.field_146126_j=tr("gui.forgeautofish.sound",state(soundDetection));
            if(b.field_146127_k==13) b.field_146126_j=tr("gui.forgeautofish.fixPitch",state(fixCastPitch));
            if(b.field_146127_k==14) b.field_146126_j=tr("gui.forgeautofish.fixYaw",state(fixCastYaw));
        }
    }

    private static String tr(String key,Object... args) { return I18n.func_135052_a(key,args); }
    private static String state(boolean value) { return tr(value?"gui.forgeautofish.on":"gui.forgeautofish.off"); }
    private String label(int index) { return tr("gui.forgeautofish."+keys[index]); }
    private void status(String key,Object... args) { statusKey=key; statusArgs=args; }

    @Override protected void func_146284_a(GuiButton button) {
        if(button.field_146127_k==1) { field_146297_k.func_147108_a(parent); return; }
        if(button.field_146127_k==3) {
            rememberFields(); page=1-page; fields=null; func_73866_w_(); return;
        }
        if(button.field_146127_k==2) {
            for(int i=0;i<keys.length;i++) values.setProperty(keys[i],defaults[i]);
            for(int i=0;i<fields.length;i++) fields[i].func_146180_a(defaults[fieldOffset()+i]);
            protectRod=true; soundDetection=true; fixCastPitch=false; fixCastYaw=false;
            updateButtons(); status("gui.forgeautofish.status.defaults"); return;
        }
        if(button.field_146127_k==15 && page==1) {
            if(field_146297_k.field_71439_g==null) { status("gui.forgeautofish.status.noPlayer"); return; }
            float yaw=field_146297_k.field_71439_g.field_70177_z%360;
            if(yaw>180) yaw-=360; if(yaw< -180) yaw+=360;
            fields[0].func_146180_a(Float.toString(field_146297_k.field_71439_g.field_70125_A));
            fields[1].func_146180_a(Float.toString(yaw));
            fixCastPitch=true; fixCastYaw=true; updateButtons(); status("gui.forgeautofish.status.captured"); return;
        }
        if(button.field_146127_k==10) enabled=!enabled;
        if(button.field_146127_k==11) protectRod=!protectRod;
        if(button.field_146127_k==12) soundDetection=!soundDetection;
        if(button.field_146127_k==13) fixCastPitch=!fixCastPitch;
        if(button.field_146127_k==14) fixCastYaw=!fixCastYaw;
        updateButtons();
        if(button.field_146127_k!=0) return;
        rememberFields();
        Properties edited=new Properties();
        for(int i=0;i<keys.length;i++) {
            String raw=values.getProperty(keys[i],defaults[i]).trim();
            try {
                double value=Double.parseDouble(raw);
                if(Double.isNaN(value) || Double.isInfinite(value) || value<min[i] || value>max[i]) throw new NumberFormatException();
                if(i<6) { Long.parseLong(raw); }
                edited.setProperty(keys[i],raw);
            } catch(NumberFormatException error) {
                String low=i<6?Long.toString((long)min[i]):Double.toString(min[i]);
                String high=i<6?Long.toString((long)max[i]):Double.toString(max[i]);
                status("gui.forgeautofish.status.invalid",label(i),low,high); return;
            }
        }
        edited.setProperty("enabled",Boolean.toString(enabled)); edited.setProperty("protectRod",Boolean.toString(protectRod));
        edited.setProperty("soundDetection",Boolean.toString(soundDetection));
        edited.setProperty("fixCastPitch",Boolean.toString(fixCastPitch));
        edited.setProperty("fixCastYaw",Boolean.toString(fixCastYaw));
        boolean saved=owner.applySettings(edited);
        values=owner.settingsSnapshot();
        status(saved?"gui.forgeautofish.status.saved":"gui.forgeautofish.status.saveFailed");
    }

    @Override public void func_73863_a(int mouseX,int mouseY,float partialTicks) {
        func_146276_q_();
        func_73732_a(field_146289_q,tr("gui.forgeautofish.title"),field_146294_l/2,12,0xffffff);
        func_73732_a(field_146289_q,tr(page==0?"gui.forgeautofish.hint":"gui.forgeautofish.angleHint"),field_146294_l/2,26,0xaaaaaa);
        for(int i=0;i<fields.length;i++) {
            func_73731_b(field_146289_q,label(fieldOffset()+i),field_146294_l/2-145+(i%2)*150,42+(i/2)*31,0xdddddd);
            fields[i].func_146194_f();
        }
        func_73732_a(field_146289_q,tr(statusKey,statusArgs),field_146294_l/2,field_146295_m-39,0xffcc66);
        super.func_73863_a(mouseX,mouseY,partialTicks);
    }

    @Override protected void func_73864_a(int mouseX,int mouseY,int button) {
        super.func_73864_a(mouseX,mouseY,button);
        for(GuiTextField field:fields) field.func_146192_a(mouseX,mouseY,button);
    }
    @Override protected void func_73869_a(char character,int key) {
        if(key==15) {
            int selected=-1; for(int i=0;i<fields.length;i++) if(fields[i].func_146206_l()) selected=i;
            for(GuiTextField field:fields) field.func_146195_b(false);
            fields[(selected+1)%fields.length].func_146195_b(true); return;
        }
        for(GuiTextField field:fields) if(field.func_146201_a(character,key)) return;
        super.func_73869_a(character,key);
    }
    @Override public void func_73876_c() { for(GuiTextField field:fields) field.func_146178_a(); }
    @Override public boolean func_73868_f() { return false; }
}
