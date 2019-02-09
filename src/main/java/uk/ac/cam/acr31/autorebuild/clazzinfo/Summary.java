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
public abstract class Summary {

  public abstract String classFile();

  public abstract Optional<String> sourceFile();

  public abstract String className();

  public abstract ImmutableSet<String> declared();

  public abstract ImmutableSet<String> referenced();

  public abstract String digest();

  public static Summary create(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return create(path.toString(), is);
    }
  }

  public static Summary create(String classFile, InputStream is) throws IOException {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
    Builder builder = builder().setClassFile(classFile);
    ClassReader r = new ClassReader(new DigestInputStream(is, md));
    r.accept(new ClassInfoVisitor(builder), 0);
    return builder.setDigest(BaseEncoding.base16().encode(md.digest())).build();
  }

  static Builder builder() {
    return new AutoValue_Summary.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setClassFile(String classFile);

    abstract Builder setSourceFile(String sourceFile);

    abstract Builder setClassName(String className);

    abstract String className();

    abstract ImmutableSet.Builder<String> declaredBuilder();

    Builder addDeclared(String declared) {
      declaredBuilder().add(declared);
      return this;
    }

    abstract ImmutableSet.Builder<String> referencedBuilder();

    Builder addReferenced(String referenced) {
      referencedBuilder().add(referenced);
      return this;
    }

    abstract Builder setDigest(String digest);

    abstract Summary build();
  }
}
