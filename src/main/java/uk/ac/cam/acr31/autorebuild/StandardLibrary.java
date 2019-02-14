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

import com.google.common.collect.ImmutableSet;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;

public class StandardLibrary {

  private static final ImmutableSet<String> LIBRARY_PREFIXES =
      ImmutableSet.of("java", "com/sun", "jdk", "sun");

  private final Map<String, Optional<ClassFile>> cache;

  public StandardLibrary() {
    cache = new HashMap<>();
  }

  public Optional<ClassFile> load(String descriptor) {
    if (LIBRARY_PREFIXES.stream().noneMatch(p -> descriptor.startsWith(p))) {
      return Optional.empty();
    }
    String classFile = descriptor + ".class";
    if (cache.containsKey(classFile)) {
      return cache.get(classFile);
    }
    Optional<ClassFile> result = loadClassFile(classFile);
    cache.put(classFile, result);
    return result;
  }

  private Optional<ClassFile> loadClassFile(String classFile) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(classFile)) {
      if (is != null) {
        return Optional.of(ClassFile.create(classFile, is));
      }
    } catch (IOException e) {
      throw new IOError(e);
    }
    return Optional.empty();
  }
}
