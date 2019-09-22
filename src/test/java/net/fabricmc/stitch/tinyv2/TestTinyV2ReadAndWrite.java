package net.fabricmc.stitch.tinyv2;

import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestTinyV2ReadAndWrite {
    private static final String DIR = new File(TestTinyV2ReadAndWrite.class.getClassLoader().getResource("sorted").getPath()).getAbsolutePath() + "/";
    private void tryToReadAndWrite(String path) throws IOException {
        Path intMappings = Paths.get(path);
        TinyFile tinyFile = TinyV2Reader.read(intMappings);

        Path tempLocation = Paths.get(path + "temp");
        tempLocation.toFile().deleteOnExit();
        TinyV2Writer.write(tinyFile, tempLocation);
        String originalIntMappings = new String(Files.readAllBytes(intMappings));
        String writtenIntMappings = new String(Files.readAllBytes(tempLocation));

        // Ensure the file has not changed
        Assertions.assertEquals(originalIntMappings, writtenIntMappings);

    }

    @Test
    public void ReadingAndWritingAV2FileLeavesItUnchanged() throws IOException {
        tryToReadAndWrite(DIR +"intermediary-mappings.tinyv2");
        tryToReadAndWrite(DIR +"yarn-mappings.tinyv2");
        tryToReadAndWrite(DIR +"merged-proposed.tinyv2");
        tryToReadAndWrite(DIR + "test-skip");
    }
}
