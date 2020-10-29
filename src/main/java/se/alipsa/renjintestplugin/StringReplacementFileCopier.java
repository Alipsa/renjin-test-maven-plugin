package se.alipsa.renjintestplugin;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class StringReplacementFileCopier {

  private static final Logger logger = LoggerFactory.getLogger(StringReplacementFileCopier.class);

  /**
   * Copy all files in a directory to the target directory
   * If there is a filter match for a file then matching string will be replaced otherwise the file will be copied as is
   *
   * @param sourceDirectory the source directory
   * @param targetDirectory the target directory
   * @param filter a FileFilter for files where content should be replcaed
   * @param replaceStringsWhenCopy a Properties map where key=target to be replaced and value=the replacement value
   * @throws IOException if some file reading or writing operation failed
   */
  public static void copyDirectory(File sourceDirectory, File targetDirectory, FileFilter filter, Properties replaceStringsWhenCopy) throws IOException {
    if (sourceDirectory == null) {
      throw new IllegalArgumentException("source directory is null");
    }
    File[] sourceFiles = sourceDirectory.listFiles();
    if (sourceFiles == null) {
      logger.info("No files in {}, nothing to do", sourceDirectory);
      return;
    }
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs();
    }
    for (File file : sourceFiles) {
      if (file.isDirectory()) {
        File targetDir = new File(targetDirectory, file.getName());
        copyDirectory(file, targetDir, filter, replaceStringsWhenCopy);
      } else {
        //logger.info("copying {} to {}", file, targetDirectory);
        if (filter.accept(file)) {
          StringBuilder sb = new StringBuilder();
          Files.lines(file.toPath(), StandardCharsets.UTF_8).forEach(line -> {
            for (String key : replaceStringsWhenCopy.stringPropertyNames()) {
              if (line.contains(key)) {
                logger.debug("Found {} in file {}, changing it to {}", key, file, replaceStringsWhenCopy.getProperty(key));
              }
              String modLine = line.replace(key, replaceStringsWhenCopy.getProperty(key));
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
}
