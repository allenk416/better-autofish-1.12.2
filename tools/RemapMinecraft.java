import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Builds a compile-only SRG jar from the user's vanilla jar and loader mapping. */
public final class RemapMinecraft {
    public static void main(String[] args) throws Exception {
        Map<String,String> classes = new HashMap<>(), fields = new HashMap<>(), methods = new HashMap<>();
        String owner = null;
        for (String line : Files.readAllLines(Paths.get(args[1]))) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 2 || p[0].startsWith("#")) continue;
            if (!Character.isWhitespace(line.charAt(0))) { owner=p[0]; classes.put(owner,p[1]); }
            else if (p.length == 2) fields.put(owner+"/"+p[0],p[1]);
            else methods.put(owner+"/"+p[0]+p[1],p[2]);
        }
        Map<String,List<String>> parents = new HashMap<>();
        try (JarFile jar = new JarFile(args[0])) {
            Enumeration<JarEntry> en=jar.entries();
            while(en.hasMoreElements()) {
                JarEntry e=en.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                ClassReader r=new ClassReader(jar.getInputStream(e));
                List<String> ps=new ArrayList<>();
                if(r.getSuperName()!=null) ps.add(r.getSuperName());
                ps.addAll(Arrays.asList(r.getInterfaces())); parents.put(r.getClassName(),ps);
            }
        }
        Remapper remapper=new Remapper() {
            public String map(String name) {return classes.getOrDefault(name,name);}
            String resolve(Map<String,String> table,String owner,String key,Set<String> seen) {
                if(!seen.add(owner)) return null;
                String value=table.get(owner+"/"+key);
                if(value!=null) return value;
                for(String parent:parents.getOrDefault(owner,Collections.emptyList())) {
                    value=resolve(table,parent,key,seen); if(value!=null) return value;
                }
                return null;
            }
            public String mapFieldName(String owner,String name,String desc) {
                String value=resolve(fields,owner,name,new HashSet<>()); return value==null?name:value;
            }
            public String mapMethodName(String owner,String name,String desc) {
                if(name.startsWith("<")) return name;
                String value=resolve(methods,owner,name+desc,new HashSet<>()); return value==null?name:value;
            }
        };
        int count=0;
        try(JarFile jar=new JarFile(args[0]); JarOutputStream out=new JarOutputStream(Files.newOutputStream(Paths.get(args[2])))) {
            Enumeration<JarEntry> en=jar.entries();
            while(en.hasMoreElements()) {
                JarEntry e=en.nextElement(); if(!e.getName().endsWith(".class")) continue;
                ClassReader r=new ClassReader(jar.getInputStream(e)); ClassWriter w=new ClassWriter(0);
                r.accept(new ClassRemapper(w,remapper),0);
                out.putNextEntry(new JarEntry(remapper.map(r.getClassName())+".class")); out.write(w.toByteArray()); out.closeEntry(); count++;
            }
        }
        System.out.println("Remapped "+count+" Minecraft classes for compilation only.");
    }
}
