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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;

@AutoValue
public abstract class OriginalProject {

  abstract ImmutableSet<SourceFile> sourceFiles();

  abstract ImmutableSet<ClassFile> classFiles();

  static OriginalProject load(Path projectRoot) throws IOException {
    Builder builder = builder();
    ProjectFileVisitor visitor = new ProjectFileVisitor(builder);
    Files.walkFileTree(projectRoot, visitor);
    return builder.build();
  }

  public static Builder builder() {
    return new AutoValue_OriginalProject.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    abstract ImmutableSet.Builder<SourceFile> sourceFilesBuilder();

    Builder addSourceFile(SourceFile sourceFile) {
      sourceFilesBuilder().add(sourceFile);
      return this;
    }

    Builder addSourceFiles(Iterable<SourceFile> sourceFiles) {
      sourceFiles.forEach(this::addSourceFile);
      return this;
    }

    abstract ImmutableSet.Builder<ClassFile> classFilesBuilder();

    Builder addClassFiles(Iterable<ClassFile> classFiles) {
      classFilesBuilder().addAll(classFiles);
      return this;
    }

    Builder addClassFile(ClassFile classFile) {
      classFilesBuilder().add(classFile);
      return this;
    }

    abstract OriginalProject build();
  }

  ImmutableSet<ClassFile> generatedClasses() {
    ImmutableMap<String, SourceFile> nameToSourceFile =
        Maps.uniqueIndex(
            sourceFiles(), s -> Objects.requireNonNull(s).packageName() + "." + s.fileName());

    ImmutableSet.Builder<ClassFile> generatedClasses = ImmutableSet.builder();
    for (ClassFile classFile : classFiles()) {
      if (classFile.sourceFileName().isPresent()) {
        SourceFile sourceFile =
            nameToSourceFile.get(classFile.packageName() + "." + classFile.sourceFileName().get());
        if (sourceFile != null) {
          generatedClasses.add(classFile);
        }
      }
    }
    return generatedClasses.build();
  }

  private static class ProjectFileVisitor extends SimpleFileVisitor<Path> {

    private static final PathMatcher SOURCE_FILE_MATCHER =
        FileSystems.getDefault().getPathMatcher("glob:**/*.java");
    private static final PathMatcher CLASS_FILE_MATCHER =
        FileSystems.getDefault().getPathMatcher("glob:**/*.class");
    private static final PathMatcher JAR_FILE_MATCHER =
        FileSystems.getDefault().getPathMatcher("glob:**/*.jar");
    private static final PathMatcher PACKAGE_INFO_MATCHER =
        FileSystems.getDefault().getPathMatcher("glob:**/package-info.*");

    private final Set<String> usedDigests = new HashSet<>();
    private final Builder builder;

    private ProjectFileVisitor(Builder builder) {
      this.builder = builder;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (PACKAGE_INFO_MATCHER.matches(file)) {
        return FileVisitResult.CONTINUE;
      }

      if (SOURCE_FILE_MATCHER.matches(file)) {
        builder.addSourceFile(SourceFile.create(file));
      } else if (CLASS_FILE_MATCHER.matches(file)) {
        recordClassFile(ClassFile.create(file));
      } else if (JAR_FILE_MATCHER.matches(file)) {
        JarFile jarFile = new JarFile(file.toFile());
        for (Enumeration<JarEntry> em = jarFile.entries(); em.hasMoreElements(); ) {
          JarEntry entry = em.nextElement();
          String name = entry.getName();
          if (name.endsWith(".class") && !name.endsWith("package-info.class")) {
            try (InputStream is = jarFile.getInputStream(entry)) {
              recordClassFile(ClassFile.create(file.toString(), name, is));
            }
          }
        }
      }
      return FileVisitResult.CONTINUE;
    }

    private void recordClassFile(ClassFile classFile) {
      if (usedDigests.contains(classFile.digest())) {
        return;
      }
      usedDigests.add(classFile.digest());
      builder.addClassFile(classFile);
    }
  }
}
