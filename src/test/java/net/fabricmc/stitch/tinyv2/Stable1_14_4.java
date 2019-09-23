package net.fabricmc.stitch.tinyv2;

import net.fabricmc.mappings.ClassEntry;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MethodEntry;
import net.fabricmc.stitch.commands.tinyv2.Mapping;
import net.fabricmc.tinyv2.V2MappingsProvider;
import net.fabricmc.tinyv2.model.MethodParameter;
import net.fabricmc.tinyv2.model.MethodParameterEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import cuchaz.enigma.command.ConvertMappingsCommand;

public class Stable1_14_4 {
    private static final String DIR = new File(Stable1_14_4.class.getClassLoader().getResource("stable-1.14.4").getPath()).getAbsolutePath() + "/";

    @Test
    public void testReorder2() throws Exception {
        Commands.reorder(DIR + "intermediary-mappings.tinyv2",
                DIR + "intermediary-mappings-inverted.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testMerge() throws Exception {
        String target = DIR + "merged-unordered.tinyv2";
        Commands.merge(DIR + "intermediary-mappings-inverted.tinyv2",
                DIR + "yarn-mappings.tinyv2",
                target
        );
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(target))){
            Mappings mappings  = V2MappingsProvider.readTinyMappings(reader);
            ClassEntry class_2843$class_2845$1 = findClassMapping("intermediary",
                    "net/minecraft/class_2843$class_2845$1",mappings);

            Assertions.assertEquals("net/minecraft/world/chunk/UpgradeData$class_2845$1",class_2843$class_2845$1.get("named"));

            ClassEntry class_481$class_482 = findClassMapping("intermediary","net/minecraft/class_481$class_482",mappings);
            Assertions.assertEquals("net/minecraft/client/gui/screen/ingame/CreativeInventoryScreen$class_482",
                    class_481$class_482.get("named"));

            EntryTriple blockInit = new EntryTriple("net/minecraft/class_2248","<init>","(Lnet/minecraft/class_2248$class_2251;)V");
            MethodParameterEntry blockInitParam = findMethodParameterMapping("intermediary",
                    new MethodParameter(blockInit,"",1),mappings);

            Assertions.assertEquals("settings",blockInitParam.get("named").getName());

        }

    }

    private ClassEntry findClassMapping(String column, String key, Mappings mappings){
        return find(mappings.getClassEntries(),c -> c.get(column).equals(key))
                .orElseThrow(() -> new AssertionError("Could not find key " + key + " in namespace " + column));
    }

    private MethodEntry findMethodMapping(String column, EntryTriple key, Mappings mappings){
        return find(mappings.getMethodEntries(), c-> c.get(column).equals(key))
                .orElseThrow(() -> new AssertionError("Could not find key " + key + " in namespace " + column));
    }


    private MethodParameterEntry findMethodParameterMapping(String column, MethodParameter key, Mappings mappings){
        return find(mappings.getMethodParameterEntries(), c-> c.get(column).equals(key))
                .orElseThrow(() -> new AssertionError("Could not find key " + key + " in namespace " + column));
    }

    private <T>Optional<T> find(Collection<T> list, Predicate<T> predicate){
        return list.stream().filter(predicate).findFirst();
    }


    @Test
    public void testReorder3() throws Exception {
        Commands.reorder(DIR + "merged-unordered.tinyv2",
                DIR + "merged.tinyv2",
                "official", "intermediary", "named"
        );
    }

    @Test
    public void testFieldNameProposal() throws Exception {
        Commands.proposeFieldNames("local/1.14.4-merged.jar",
                DIR + "merged.tinyv2", DIR + "merged-proposed.tinyv2");
    }

    // Requirements:
    // - Official -> Intermediary mappings "intermediary-mappings.tinyv2" from matcher
    // - Intermediary -> Named mappings "yarn-mappings.tinyv2" from yarn
    @Test
    public void testFullProcess() throws Exception{
        testReorder2();
        testMerge();
        testReorder3();
        testFieldNameProposal();
    }

    @Test
    public void testConversion() throws Exception{
        new ConvertMappingsCommand().run("tiny", DIR + "unmerged-yarn.tinyv1","tinyv2:intermediary:named", DIR + "unmerged-yarn.tinyv2");
    }
}
