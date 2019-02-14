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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.autorebuild.testing.AutoSource;
import uk.ac.cam.acr31.autorebuild.testing.Compilation;

@RunWith(JUnit4.class)
public class SearcherTest {

  @Test
  public void findsMissingDirectSymbol() {
    // ARRANGE
    AutoSource aWithF = AutoSource.builder().setClassName("A").addDeclared("f").build();
    AutoSource aWithoutF = AutoSource.builder().setClassName("A").build();
    AutoSource b = AutoSource.builder().setClassName("B").addReferenced("A", "f").build();

    Compilation compilation1 = Compilation.create(aWithF, b);
    Compilation compilation2 = Compilation.create(aWithoutF);

    OriginalProject originalProject =
        OriginalProject.builder()
            .addSourceFile(compilation1.sourceFile(b))
            .addClassFiles(compilation1.classFiles())
            .addClassFiles(compilation2.classFiles())
            .build();

    // ACT
    Repository repository = Searcher.search(originalProject);

    // ASSERT
    assertThat(repository.classes()).containsExactlyElementsIn(compilation1.classFiles());
  }

  @Test
  public void findsMissingInheritedSymbol() {
    // ARRANGE
    AutoSource aWithF = AutoSource.builder().setClassName("A").addDeclared("f").build();
    AutoSource bExtendsA = AutoSource.builder().setClassName("B").setSuperClass("A").build();
    AutoSource b = AutoSource.builder().setClassName("B").build();
    AutoSource cRequiresFInB =
        AutoSource.builder().setClassName("C").addReferenced("B", "f").build();

    Compilation compilation1 = Compilation.create(aWithF, bExtendsA, cRequiresFInB);
    Compilation compilation2 = Compilation.create(b);

    OriginalProject originalProject =
        OriginalProject.builder()
            .addSourceFile(compilation1.sourceFile(cRequiresFInB))
            .addClassFiles(compilation1.classFiles())
            .addClassFiles(compilation2.classFiles())
            .build();

    // ACT
    Repository repository = Searcher.search(originalProject);

    // ASSERT
    assertThat(repository.classes()).containsExactlyElementsIn(compilation1.classFiles());
  }

  @Test
  public void findsInheritedSymbolFromJavaLang() {
    // ARRANGE
    AutoSource a =
        AutoSource.builder().setClassName("A").setSuperClass("java.util.LinkedList").build();
    AutoSource b = AutoSource.builder().setClassName("B").addReferenced("A", "size").build();

    Compilation compilation = Compilation.create(a, b);

    OriginalProject originalProject =
        OriginalProject.builder()
            .addSourceFile(compilation.sourceFile(b))
            .addClassFiles(compilation.classFiles())
            .build();

    // ACT
    Repository repository = Searcher.search(originalProject);

    // ASSERT
    assertThat(repository.classes()).containsExactlyElementsIn(compilation.classFiles());
    assertThat(repository.isSatisfied()).isTrue();
  }

  @Test
  public void resolvesOuterOfInnerClass() {
    AutoSource a =
        AutoSource.builder()
            .setClassName("A")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "class A {",
                "   class AInner {}",
                "}")
            .build();
    AutoSource b =
        AutoSource.builder()
            .setClassName("B")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "class B {",
                "  void f(A.AInner a) {}",
                "}")
            .build();

    Compilation compilation = Compilation.create(a, b);

    OriginalProject originalProject =
        OriginalProject.builder()
            .addSourceFile(compilation.sourceFile(b))
            .addClassFiles(compilation.classFiles())
            .build();

    // ACT
    Repository repository = Searcher.search(originalProject);

    // ASSERT
    assertThat(repository.classes()).containsAllIn(compilation.classFiles());
  }
}
