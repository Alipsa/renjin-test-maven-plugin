package se.alipsa.renjintestplugin;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

public class StringReplacementFileCopier {

  /**
   * Copy all files in a directory to the target directory
   * If there is a filter match for a file then matching string will be replaced otherwise the file will be copied as is
   */
  public static void copyDirectory(File sourceDirectory, File targetDirectory, FileFilter filter, Properties replaceStringsWhenCopy) throws IOException {
    if (sourceDirectory == null) {
      throw new IllegalArgumentException("source directory is null");
    }
    File[] sourceFiles = sourceDirectory.listFiles();
    if (sourceFiles == null) {
      // No files here, nothing to do
      return;
    }
    for (File file : sourceFiles) {
      if (filter.accept(file)) {
        StringBuilder sb = new StringBuilder();
        Files.lines(file.toPath(), StandardCharsets.UTF_8).forEach(line -> {
          for (String key : replaceStringsWhenCopy.stringPropertyNames()) {
            String modLine = line.replaceAll(key, replaceStringsWhenCopy.getProperty(key));
            sb.append(modLine).append(System.lineSeparator());
          }
        });
        FileUtils.write(new File(targetDirectory, file.getName()), sb.toString(), StandardCharsets.UTF_8);
      } else {
        FileUtils.copyFileToDirectory(file, targetDirectory);
      }
    }
  }
}
