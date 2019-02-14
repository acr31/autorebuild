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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.objectweb.asm.ClassReader;

@AutoValue
public abstract class ClassFile {

  public abstract String classFileName();

  public abstract Optional<String> classFileJar();

  public abstract Optional<String> sourceFileName();

  public abstract String descriptor();

  public abstract ImmutableSet<String> ancestors();

  public abstract String packageName();

  public abstract ImmutableSet<Identifier> declared();

  public abstract ImmutableSet<Identifier> referenced();

  public abstract String digest();

  public boolean declares(String name) {
    return declared().stream().map(Identifier::name).anyMatch(n -> n.equals(name));
  }

  public static ClassFile create(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return create(path.toString(), is);
    }
  }

  private static ClassFile create(ClassFile.Builder builder, InputStream is) throws IOException {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
    ClassReader r = new ClassReader(new DigestInputStream(is, md));
    r.accept(new ClassInfoVisitor(builder), 0);
    return builder.setDigest(BaseEncoding.base16().encode(md.digest())).build();
  }

  public static ClassFile create(String classFile, InputStream is) throws IOException {
    return create(ClassFile.builder().setClassFileName(classFile), is);
  }

  public static ClassFile create(String jarFile, String classFile, InputStream is)
      throws IOException {
    return create(ClassFile.builder().setClassFileName(classFile).setClassFileJar(jarFile), is);
  }

  public static Builder builder() {
    return new AutoValue_ClassFile.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setClassFileName(String fileName);

    abstract Builder setClassFileJar(String jarName);

    abstract Builder setDescriptor(String descriptor);

    abstract Builder setSourceFileName(String sourceFileName);

    abstract Builder setPackageName(String packageName);

    abstract ImmutableSet.Builder<Identifier> declaredBuilder();

    Builder addDeclared(Identifier declared) {
      declaredBuilder().add(declared);
      return this;
    }

    abstract ImmutableSet.Builder<Identifier> referencedBuilder();

    Builder addReferenced(Identifier referenced) {
      if (referenced.owner().equals(descriptor())) {
        return this;
      }

      referencedBuilder().add(referenced);
      return this;
    }

    abstract ImmutableSet.Builder<String> ancestorsBuilder();

    Builder addAncestor(String ancestor) {
      ancestorsBuilder().add(ancestor);
      return this;
    }

    abstract Builder setDigest(String digest);

    abstract ClassFile build();

    public abstract String descriptor();
  }
}
