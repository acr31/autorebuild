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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.Identifier;

public class ClassFileStore {

  private final SetMultimap<String, ClassFile> classFiles;
  private final StandardLibrary standardLibrary;

  public ClassFileStore(StandardLibrary standardLibrary) {
    this.standardLibrary = standardLibrary;
    classFiles = MultimapBuilder.hashKeys().hashSetValues().build();
  }

  public ImmutableSet<ClassFile> entries() {
    return classFiles.asMap().entrySet().stream()
        .map(e -> Iterables.getFirst(e.getValue(), null))
        .collect(toImmutableSet());
  }

  public ImmutableSet<ClassFile> entries(String descriptor) {
    return ImmutableSet.copyOf(classFiles.get(descriptor));
  }

  public void add(ClassFile classFile) {
    classFiles.put(classFile.descriptor(), classFile);
  }

  public Map<String, Identifier> definedNames(ClassFile classFile) {
    Deque<ClassFile> queue = new LinkedList<>();
    queue.add(classFile);
    Map<String, Identifier> result = new HashMap<>();
    while (!queue.isEmpty()) {
      ClassFile next = queue.pollFirst();
      for (Identifier declared : next.declared()) {
        result.putIfAbsent(declared.name(), declared);
      }
      for (String parent : next.ancestors()) {
        Set<ClassFile> c = classFiles.get(parent);
        if (c.isEmpty()) {
          standardLibrary.load(parent).ifPresent(queue::add);
        } else {
          queue.addAll(c);
        }
      }
    }
    return result;
  }

  public void remove(ClassFile classFile) {
    classFiles.remove(classFile.descriptor(), classFile);
  }

  public ImmutableSet<ClassFile> provides(Identifier unresolved) {

    String owner = unresolved.owner();
    String name = unresolved.name();

    return classFiles.get(owner).stream()
        .filter(classFile -> provides(classFile, name, classFiles))
        .collect(toImmutableSet());
  }

  private boolean provides(
      ClassFile classFile, String name, SetMultimap<String, ClassFile> classFiles) {
    if (classFile.declares(name)) {
      return true;
    }
    for (String parent : classFile.ancestors()) {
      Optional<ClassFile> lib = standardLibrary.load(parent);
      if (lib.isPresent() && provides(lib.get(), name, classFiles)) {
        return true;
      }
      for (ClassFile parentClassFile : classFiles.get(parent)) {
        if (provides(parentClassFile, name, classFiles)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean providedByClassPath(Identifier i) {
    Deque<String> queue = new LinkedList<>();
    queue.add(i.owner());
    while (!queue.isEmpty()) {
      Optional<ClassFile> classFile = standardLibrary.load(queue.pollFirst());
      if (classFile.isPresent()) {
        if (classFile.get().declares(i.name())) {
          return true;
        }
        queue.addAll(classFile.get().ancestors());
      }
    }
    return false;
  }
}
