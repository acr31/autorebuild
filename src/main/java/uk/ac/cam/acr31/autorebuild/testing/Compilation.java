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

package uk.ac.cam.acr31.autorebuild.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.testing.compile.Compiler.javac;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import uk.ac.cam.acr31.autorebuild.SourceFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;

@AutoValue
public abstract class Compilation {

  public abstract ImmutableList<SourceFile> sourceFiles();

  public abstract ImmutableList<ClassFile> classFiles();

  public ImmutableSet<ClassFile> classFiles(AutoSource autoSource) {
    return classFiles().stream()
        .filter(f -> f.sourceFileName().orElseThrow().equals(autoSource.className() + ".java"))
        .collect(toImmutableSet());
  }

  public SourceFile sourceFile(AutoSource autoSource) {
    return sourceFiles().stream()
        .filter(i -> i.fileName().equals(autoSource.className() + ".java"))
        .findAny()
        .orElseThrow();
  }

  private static Compilation create(
      ImmutableList<SourceFile> sourceFiles, ImmutableList<ClassFile> classFiles) {
    return new AutoValue_Compilation(sourceFiles, classFiles);
  }

  public static Compilation create(AutoSource... inputFiles) {
    ImmutableList<JavaFileObject> inputObjects =
        Arrays.stream(inputFiles).map(AutoSource::toJavaFileObject).collect(toImmutableList());
    return create(
        inputObjects.stream().map(Compilation::createSourceFile).collect(toImmutableList()),
        javac().compile(inputObjects).generatedFiles().stream()
            .map(Compilation::createClassFile)
            .collect(toImmutableList()));
  }

  private static SourceFile createSourceFile(JavaFileObject i) {
    Path p = Paths.get(i.getName());
    try (BufferedReader br = new BufferedReader(i.openReader(true))) {
      return SourceFile.create(p.getParent().toString(), p.getFileName().toString(), br);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  private static ClassFile createClassFile(JavaFileObject fileObject) {
    try {
      return ClassFile.create(fileObject.getName(), fileObject.openInputStream());
    } catch (IOException e) {
      throw new IOError(e);
    }
  }
}
