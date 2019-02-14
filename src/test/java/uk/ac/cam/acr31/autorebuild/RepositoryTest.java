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
public class RepositoryTest {

  @Test
  public void resolvesStandardLibraryMethod() {
    // ARRANGE
    AutoSource sourceFile =
        AutoSource.builder()
            .setClassName("Test")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "public class Test {",
                "  Object o = new Object();",
                "}")
            .build();

    Compilation compilation = Compilation.create(sourceFile);
    Repository repository = new Repository();

    // ACT
    repository.addClassFiles(compilation.classFiles());

    // ASSERT
    assertThat(repository.isSatisfied()).isTrue();
  }

  @Test
  public void resolvesInheritedSymbol() {
    // ARRANGE
    AutoSource a = AutoSource.builder().setClassName("A").addDeclared("f").build();
    AutoSource bExtendsA = AutoSource.builder().setClassName("B").setSuperClass("A").build();
    AutoSource c = AutoSource.builder().setClassName("C").addReferenced("B", "f").build();

    Compilation compilation = Compilation.create(a, bExtendsA, c);
    Repository repository = new Repository();

    // ACT
    repository.addClassFiles(compilation.classFiles());

    // ASSERT
    assertThat(repository.isSatisfied()).isTrue();
  }

  @Test
  public void resolvesCyclicDependency() {
    // ARRANGE
    AutoSource a =
        AutoSource.builder().setClassName("A").addDeclared("f").addReferenced("B", "g").build();
    AutoSource b =
        AutoSource.builder().setClassName("B").addDeclared("g").addReferenced("A", "f").build();

    Compilation compilation = Compilation.create(a, b);
    Repository repository = new Repository();

    // ACT
    repository.addClassFiles(compilation.classFiles());

    // ASSERT
    assertThat(repository.isSatisfied()).isTrue();
  }

  @Test
  public void noticesMissingSymbol() {
    // ARRANGE
    AutoSource aWithF = AutoSource.builder().setClassName("A").addDeclared("f").build();
    AutoSource aWithoutF = AutoSource.builder().setClassName("A").build();
    AutoSource bDependsOnF = AutoSource.builder().setClassName("B").addReferenced("A", "f").build();

    Compilation compilation1 = Compilation.create(aWithF, bDependsOnF);
    Compilation compilation2 = Compilation.create(aWithoutF);

    Repository repository = new Repository();

    // ACT
    repository.addClassFiles(compilation1.classFiles(bDependsOnF));
    repository.addClassFiles(compilation2.classFiles(aWithoutF));

    // ASSERT
    assertThat(repository.isSatisfied()).isFalse();
  }

  @Test
  public void noticesMissingSymbolAddedBack() {
    AutoSource aWithF = AutoSource.builder().setClassName("A").addDeclared("f").build();
    AutoSource aWithoutF = AutoSource.builder().setClassName("A").build();
    AutoSource bDependsOnF = AutoSource.builder().setClassName("B").addReferenced("A", "f").build();

    Compilation compilation1 = Compilation.create(aWithF, bDependsOnF);
    Compilation compilation2 = Compilation.create(aWithoutF);

    Repository repository = new Repository();

    // ACT
    repository.addClassFiles(compilation1.classFiles(bDependsOnF));
    repository.addClassFiles(compilation2.classFiles(aWithoutF));
    repository.addClassFiles(compilation1.classFiles(aWithF));

    // ASSERT
    assertThat(repository.classes()).containsExactlyElementsIn(compilation1.classFiles());
  }

  @Test
  public void resolvesUnimplementedInterfaceMethod() {
    AutoSource iface =
        AutoSource.builder()
            .setClassName("Iface")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "interface Iface {",
                "  void f();",
                "}")
            .build();
    AutoSource abstrct =
        AutoSource.builder()
            .setClassName("Abstrct")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "abstract class Abstrct implements Iface {}")
            .build();
    AutoSource concrete =
        AutoSource.builder()
            .setClassName("Concrete")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "class Concrete extends Abstrct {",
                "  @Override",
                "  public void f() {}",
                "}")
            .build();
    AutoSource uses =
        AutoSource.builder()
            .setClassName("Uses")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "class Uses {",
                "  void g(Abstrct a) {",
                "    a.f();",
                "  }",
                "}")
            .build();

    Compilation compilation = Compilation.create(iface, abstrct, concrete, uses);
    Repository repository = new Repository();

    repository.addClassFiles(compilation.classFiles());

    assertThat(repository.isSatisfied()).isTrue();
  }

  @Test
  public void resolvesMethodForSuperclass() {
    AutoSource a =
        AutoSource.builder()
            .setClassName("A")
            .setPackageName("foo.bar")
            .setSourceLines(
                "package foo.bar;", //
                "interface A {",
                "  static final Object o = new Object() {};",
                "}")
            .build();

    Compilation compilation = Compilation.create(a);
    Repository repository = new Repository();

    repository.addClassFiles(compilation.classFiles());

    assertThat(repository.isSatisfied()).isTrue();
  }
}
