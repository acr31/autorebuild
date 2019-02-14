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

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.JavaFileObjects;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javax.tools.JavaFileObject;
import uk.ac.cam.acr31.autorebuild.clazzinfo.Identifier;

@AutoValue
public abstract class AutoSource {

  abstract String className();

  abstract String packageName();

  abstract Optional<String> source();

  abstract Optional<String> superClass();

  abstract ImmutableList<String> declared();

  abstract ImmutableList<Identifier> referenced();

  public JavaFileObject toJavaFileObject() {
    return JavaFileObjects.forSourceString(
        String.format("%s.%s", packageName(), className()), this.toString());
  }

  @Override
  public final String toString() {
    if (source().isPresent()) {
      return source().get();
    }
    StringWriter w = new StringWriter();
    try (PrintWriter p = new PrintWriter(w)) {
      p.printf("package %s;%n", packageName());
      if (superClass().isPresent()) {
        p.printf("public class %s extends %s {%n", className(), superClass().get());
      } else {
        p.printf("public class %s {%n", className());
      }
      for (String declared : declared()) {
        p.printf("  public void %s() {}%n", declared);
      }
      p.printf("  private static void referencing() {%n");
      for (Identifier referenced : referenced()) {
        p.printf("   new %s().%s();%n", referenced.owner(), referenced.name());
      }
      p.printf("  }%n");
      p.printf("}%n");
    }
    return w.toString();
  }

  public static Builder builder() {
    return new AutoValue_AutoSource.Builder().setPackageName("foo.bar");
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setClassName(String className);

    public abstract Builder setPackageName(String packageName);

    public abstract Builder setSource(String source);

    public Builder setSourceLines(String... lines) {
      return setSource(Joiner.on("\n").join(lines));
    }

    public abstract Builder setSuperClass(String superClass);

    abstract ImmutableList.Builder<String> declaredBuilder();

    public Builder addDeclared(String name) {
      declaredBuilder().add(name);
      return this;
    }

    abstract ImmutableList.Builder<Identifier> referencedBuilder();

    public Builder addReferenced(String owner, String name) {
      referencedBuilder().add(Identifier.create(owner, name));
      return this;
    }

    public abstract AutoSource build();
  }
}
