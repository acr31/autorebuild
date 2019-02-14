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
import java.util.Map;
import java.util.Set;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.Identifier;

public class IdentifierStore {

  private SetMultimap<String, Identifier> ownerToIdentifier;
  private SetMultimap<Identifier, ClassFile> identifierToReferent;
  private SetMultimap<ClassFile, Identifier> referentToIdentifier;

  IdentifierStore() {
    ownerToIdentifier = MultimapBuilder.linkedHashKeys().hashSetValues().build();
    identifierToReferent = MultimapBuilder.hashKeys().hashSetValues().build();
    referentToIdentifier = MultimapBuilder.hashKeys().hashSetValues().build();
  }

  int size() {
    return ownerToIdentifier.size();
  }

  ImmutableSet<Identifier> identifiers(String descriptor) {
    return ImmutableSet.copyOf(ownerToIdentifier.get(descriptor));
  }

  ImmutableSet<String> names(String descriptor) {
    return ownerToIdentifier.get(descriptor).stream()
        .map(Identifier::name)
        .collect(toImmutableSet());
  }

  void add(Identifier identifier, ClassFile referent) {
    ownerToIdentifier.put(identifier.owner(), identifier);
    identifierToReferent.put(identifier, referent);
    referentToIdentifier.put(referent, identifier);
  }

  void add(Identifier identifier, Iterable<ClassFile> referring) {
    ownerToIdentifier.put(identifier.owner(), identifier);
    identifierToReferent.putAll(identifier, referring);
    referring.forEach(r -> referentToIdentifier.put(r, identifier));
  }

  Set<ClassFile> referents(Identifier identifier) {
    return identifierToReferent.get(identifier);
  }

  void remove(Identifier identifier) {
    ownerToIdentifier.remove(identifier.owner(), identifier);
    Set<ClassFile> referents = identifierToReferent.removeAll(identifier);
    referents.forEach(r -> referentToIdentifier.remove(r, identifier));
  }

  void removeReferredFrom(ClassFile classFile) {
    for (Identifier identifier : referentToIdentifier.get(classFile)) {
      identifierToReferent.remove(identifier, classFile);
    }
    referentToIdentifier.removeAll(classFile);
  }

  boolean isEmpty() {
    return ownerToIdentifier.isEmpty();
  }

  Identifier next() {
    Map.Entry<String, Identifier> entry = Iterables.getFirst(ownerToIdentifier.entries(), null);
    Set<Identifier> identifiers = ownerToIdentifier.removeAll(entry.getKey());
    ownerToIdentifier.putAll(entry.getKey(), identifiers);
    return entry.getValue();
  }
}
