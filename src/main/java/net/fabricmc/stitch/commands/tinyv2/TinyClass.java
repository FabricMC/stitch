package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TinyClass implements Comparable<TinyClass> {

    private final List<String> classNames;
    private final Set<TinyMethod> methods;
    private final Set<TinyField> fields;
    private final Collection<String> comments;

    public TinyClass(List<String> classNames, Set<TinyMethod> methods, Set<TinyField> fields, Collection<String> comments) {
        this.classNames = classNames;
        this.methods = methods;
        this.fields = fields;
        this.comments = comments;
    }


    public List<String> getClassNames() {
        return classNames;
    }

    public Set<TinyMethod> getMethods() {
        return methods;
    }

    public Set<TinyField> getFields() {
        return fields;
    }

    public Collection<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TinyClass o) {
        return classNames.get(0).compareTo(o.classNames.get(0));
////        if(classNames.get(0).equals("aaa")){
////            int x = 2;
////        }
//
//        // Inner classes come before (this is not important, it just makes testing easier)
//        String[] classAndInnerClasses = classNames.get(0).split("\\$");
//        String[] otherClassAndInnerClasses = o.classNames.get(0).split("\\$");
//
//        int result = classAndInnerClasses[0].compareTo(otherClassAndInnerClasses[0]);
//        if(result == 0){
//            if(classAndInnerClasses.length != otherClassAndInnerClasses.length) {
//                return otherClassAndInnerClasses.length - classAndInnerClasses.length;
//            }else if( classAndInnerClasses.length >= 2){
//                return classAndInnerClasses[1].compareTo(otherClassAndInnerClasses[1]);
//            }else{
//                return 0;
//            }
//        }
//
//        return result;

    }
}
