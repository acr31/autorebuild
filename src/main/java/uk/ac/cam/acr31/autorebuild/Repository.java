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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.Identifier;

public class Repository {

  private final ClassFileStore classFiles;
  private final IdentifierStore unresolved;
  private final IdentifierStore used;

  Repository() {
    this(new StandardLibrary());
  }

  Repository(StandardLibrary standardLibrary) {
    classFiles = new ClassFileStore(standardLibrary);
    unresolved = new IdentifierStore();
    used = new IdentifierStore();
  }

  public ImmutableSet<ClassFile> classes() {
    return classFiles.entries();
  }

  public int unresolvedCount() {
    return unresolved.size();
  }

  public void addClassFiles(Iterable<ClassFile> classFiles) {
    classFiles.forEach(this::addClassFile);
  }

  public boolean addClassFile(ClassFile toAdd) {
    // todo(acr13) you might want to add a class that doesn't provide all the required symbols
    // because these are now in the superclass.

    // Check that this class standardLibrary all the identifiers that we need
    Map<String, Identifier> namesInToAdd = classFiles.definedNames(toAdd);
    if (!namesInToAdd.keySet().containsAll(used.names(toAdd.descriptor()))) {
      return false;
    }

    // Resolve any unresolved identifiers now provided by this class
    for (Identifier unresolvedIdentifier : unresolved.identifiers(toAdd.descriptor())) {
      Identifier resolving = namesInToAdd.get(unresolvedIdentifier.name());
      if (resolving != null) {
        used.add(unresolvedIdentifier, unresolved.referents(unresolvedIdentifier));
        // If this identifier was resolved by a superclass then also record that we are using that
        // identifier in the superclass.
        if (!resolving.owner().equals(toAdd.descriptor())) {
          used.add(resolving, toAdd);
        }
        unresolved.remove(unresolvedIdentifier);
      }
    }

    Set<String> descriptorsToCheckForMissingIdentifiers = new HashSet<>();
    descriptorsToCheckForMissingIdentifiers.add(toAdd.descriptor());

    classFiles.add(toAdd);

    // Add any identifiers this class requires to unresolved
    for (Identifier identifier : toAdd.referenced()) {

      boolean found = false;
      for (ClassFile potential : classFiles.entries(identifier.owner())) {
        Map<String, Identifier> potentialNames = classFiles.definedNames(potential);
        Identifier foundIdentifier = potentialNames.get(identifier.name());
        if (foundIdentifier != null) {
          found = true;
          used.add(identifier, toAdd);
          if (!identifier.owner().equals(foundIdentifier.owner())) {
            used.add(foundIdentifier, potential);
          }
          descriptorsToCheckForMissingIdentifiers.add(identifier.owner());
        }
      }
      if (found) {
        continue;
      }

      if (classFiles.providedByClassPath(identifier)) {
        continue;
      }

      unresolved.add(identifier, toAdd);
    }

    for (String d : descriptorsToCheckForMissingIdentifiers) {
      // Remove any versions of this class that don't provide all the used identifiers
      removeClassesWithMissingIdentifiers(d);
    }

    return true;
  }

  private void removeClassesWithMissingIdentifiers(String descriptor) {
    Set<String> usedNames = used.names(descriptor);
    for (ClassFile classFile : classFiles.entries(descriptor)) {
      Map<String, Identifier> namesToOwners = classFiles.definedNames(classFile);
      if (!namesToOwners.keySet().containsAll(usedNames)) {
        classFiles.remove(classFile);
        used.removeReferredFrom(classFile);
        unresolved.removeReferredFrom(classFile);
      }
    }
  }

  public boolean isSatisfied() {
    return unresolved.isEmpty();
  }

  public Identifier nextUnresolved() {
    return unresolved.next();
  }

  public int usedCount() {
    return used.size();
  }

  public void unresolveable(Identifier next) {
    unresolved.remove(next);
  }
}
