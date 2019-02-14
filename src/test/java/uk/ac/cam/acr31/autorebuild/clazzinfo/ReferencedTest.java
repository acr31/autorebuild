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

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.autorebuild.testing.AutoSource;
import uk.ac.cam.acr31.autorebuild.testing.Compilation;

@RunWith(JUnit4.class)
public class ReferencedTest {

  @Test
  public void referencedMethod_isFound() {
    // ARRANGE
    AutoSource sourceFile =
        AutoSource.builder()
            .setPackageName("foo.bar")
            .setClassName("Test")
            .setSourceLines(
                "package foo.bar;", //
                "public class Test {",
                "  void f() {",
                "    System.out.println(3);",
                "  }",
                "}")
            .build();
    Compilation compilation = Compilation.create(sourceFile);

    // ACT
    ClassFile classFile = Iterables.getOnlyElement(compilation.classFiles());

    // ASSERT
    assertThat(classFile.referenced())
        .contains(Identifier.create("java/io/PrintStream", "println(I)V"));
  }

  @Test
  public void innerClass_isReferenced() {
    // ARRANGE
    AutoSource sourceFile =
        AutoSource.builder()
            .setPackageName("foo.bar")
            .setClassName("Test")
            .setSourceLines(
                "package foo.bar;", //
                "public class Test {",
                "  class Inner {}",
                "}")
            .build();
    Compilation compilation = Compilation.create(sourceFile);

    // ACT
    ClassFile classFile = compilation.classFiles().get(0);

    // ASSERT
    assertThat(classFile.referenced()).contains(Identifier.create("foo/bar/Test$Inner"));
  }

  @Test
  public void annotation_isReferenced() {
    // ARRANGE
    AutoSource annotation =
        AutoSource.builder()
            .setPackageName("foo.bar")
            .setClassName("TestAnn")
            .setSourceLines(
                "package foo.bar;", //
                "import java.lang.annotation.Retention;",
                "import java.lang.annotation.RetentionPolicy;",
                "import java.lang.annotation.Target;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "public @interface TestAnn {}")
            .build();
    AutoSource sourceFile =
        AutoSource.builder()
            .setPackageName("foo.bar")
            .setClassName("Test")
            .setSourceLines(
                "package foo.bar;", //
                "@TestAnn",
                "public class Test {}")
            .build();

    Compilation compilation = Compilation.create(annotation, sourceFile);

    // ACT
    ClassFile classFile = compilation.classFiles().get(1);

    // ASSERT
    assertThat(classFile.referenced()).contains(Identifier.create("foo/bar/TestAnn"));
  }
}
