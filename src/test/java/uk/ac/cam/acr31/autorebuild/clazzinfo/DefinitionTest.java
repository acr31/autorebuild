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

package uk.ac.cam.acr31.autorebuild.clazzinfo;

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.common.collect.Iterables;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefinitionTest {

  @Test
  public void definedMethod_isFound() throws IOException {
    // ARRANGE
    JavaFileObject sourceFile =
        JavaFileObjects.forSourceLines(
            "foo.bar.Test", //
            "package foo.bar;",
            "public class Test {",
            "  void methodDef(String a, int b, Integer c) {}",
            "}");
    Compilation compilation = javac().compile(sourceFile);
    JavaFileObject output = Iterables.getOnlyElement(compilation.generatedFiles());

    // ACT
    Summary summary = Summary.create(output.getName(), output.openInputStream());

    // ASSERT
    assertThat(summary.declared())
        .contains(
            Identifiers.fromMethod(
                "Lfoo/bar/Test;", "methodDef", "(Ljava/lang/String;ILjava/lang/Integer;)V"));
  }

  @Test
  public void definedClass_isFound() throws IOException {
    // ARRANGE
    JavaFileObject sourceFile =
        JavaFileObjects.forSourceLines(
            "foo.bar.Test", //
            "package foo.bar;",
            "public class Test {}");
    Compilation compilation = javac().compile(sourceFile);
    JavaFileObject output = Iterables.getOnlyElement(compilation.generatedFiles());

    // ACT
    Summary summary = Summary.create(output.getName(), output.openInputStream());

    // ASSERT
    assertThat(summary.declared()).contains(Identifiers.fromInternalName("foo/bar/Test"));
  }

  @Test
  public void definedField_isFound() throws IOException {
    // ARRANGE
    JavaFileObject sourceFile =
        JavaFileObjects.forSourceLines(
            "foo.bar.Test", //
            "package foo.bar;",
            "public class Test {",
            "  java.util.List<String> myField = null;",
            "}");
    Compilation compilation = javac().compile(sourceFile);
    JavaFileObject output = Iterables.getOnlyElement(compilation.generatedFiles());

    // ACT
    Summary summary = Summary.create(output.getName(), output.openInputStream());

    // ASSERT
    assertThat(summary.declared())
        .contains(Identifiers.fromField("Lfoo/bar/Test;", "myField", "Ljava/util/List;"));
  }

  @Test
  public void definedInnerClass_isFound() throws IOException {
    // ARRANGE
    JavaFileObject sourceFile =
        JavaFileObjects.forSourceLines(
            "foo.bar.Test", //
            "package foo.bar;",
            "public class Test {",
            "  class Inner {}",
            "}");
    Compilation compilation = javac().compile(sourceFile);
    JavaFileObject output = compilation.generatedFiles().get(0);

    // ACT
    Summary summary = Summary.create(output.getName(), output.openInputStream());

    // ASSERT
    assertThat(summary.declared()).contains(Identifiers.fromInternalName("foo/bar/Test$Inner"));
  }
}
