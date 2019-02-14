/*
 * Copyright © 2019 Andrew Rice (acr31@cam.ac.uk)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.acr31.autorebuild;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;

public class Main {

  public static void main(String[] args) throws IOException {

    // todo(acr31) exclude source files that are not compiled

    Path projectRoot = Paths.get(args[0]);
    Path targetRoot = Paths.get(args[1]);
    OriginalProject originalProject = OriginalProject.load(projectRoot);
    System.out.println("Loaded project");
    final Repository repository = Searcher.search(originalProject);

    Files.walk(targetRoot)
        .map(Path::toFile)
        .sorted((o1, o2) -> -o1.compareTo(o2))
        .forEach(File::delete);

    Files.createDirectories(targetRoot.resolve("src"));
    Files.createDirectories(targetRoot.resolve("lib"));
    for (SourceFile sourceFile : originalProject.sourceFiles()) {
      Path source = Paths.get(sourceFile.directory(), sourceFile.fileName());
      Path target = targetRoot.resolve("src");
      for (String packagePart : sourceFile.packageName().split("\\.")) {
        target = target.resolve(packagePart);
      }
      target = target.resolve(sourceFile.fileName());
      Files.createDirectories(target.getParent());

      try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(source))) {
        CharsetDetector cd = new CharsetDetector();
        cd.setText(bis);
        CharsetMatch cm = cd.detect();
        if (cm != null && !cm.getName().equals("UTF-8")) {
          try (Reader reader = cm.getReader();
              Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            reader.transferTo(writer);
          }
        } else {
          Files.copy(source, target);
        }
      }
    }

    SetMultimap<String, ClassFile> jarEntries = MultimapBuilder.hashKeys().hashSetValues().build();
    for (ClassFile classFile : repository.classes()) {
      if (classFile.classFileJar().isPresent()) {
        jarEntries.put(classFile.classFileJar().get(), classFile);
      } else {
        Path source = Paths.get(classFile.classFileName());
        Path target = targetRoot.resolve("lib");
        for (String packagePart : classFile.packageName().split("\\.")) {
          target = target.resolve(packagePart);
        }
        target = target.resolve(source.getFileName());
        Files.createDirectories(target.getParent());
        Files.copy(source, target);
      }
    }

    for (Map.Entry<String, Set<ClassFile>> entry : Multimaps.asMap(jarEntries).entrySet()) {
      JarFile jarFile = new JarFile(entry.getKey());
      ImmutableMap<String, ClassFile> classes =
          Maps.uniqueIndex(entry.getValue(), ClassFile::classFileName);
      for (Enumeration<JarEntry> em = jarFile.entries(); em.hasMoreElements(); ) {
        JarEntry jarEntry = em.nextElement();
        ClassFile classFile = classes.get(jarEntry.getName());
        if (classFile != null) {
          Path target = targetRoot.resolve("lib");
          for (String packagePart : classFile.packageName().split("\\.")) {
            target = target.resolve(packagePart);
          }
          target = target.resolve(Paths.get(classFile.classFileName()).getFileName());
          try (InputStream is = jarFile.getInputStream(jarEntry)) {
            Files.createDirectories(target.getParent());
            Files.copy(is, target);
          }
        }
      }
    }
  }
}
