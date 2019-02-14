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
import java.util.TreeSet;
import uk.ac.cam.acr31.autorebuild.clazzinfo.ClassFile;
import uk.ac.cam.acr31.autorebuild.clazzinfo.Identifier;

public class Searcher {

  public static Repository search(OriginalProject originalProject) {
    StandardLibrary standardLibrary = new StandardLibrary();
    Repository repository = new Repository(standardLibrary);
    repository.addClassFiles(originalProject.generatedClasses());

    ClassFileStore originalClasses = new ClassFileStore(standardLibrary);
    originalProject.classFiles().forEach(originalClasses::add);

    TreeSet<Identifier> unresolved = new TreeSet<>();
    while (!repository.isSatisfied()) {
      Identifier next = repository.nextUnresolved();
      System.out.printf(
          "\rUnresolved: %07d, Used: %07d", repository.unresolvedCount(), repository.usedCount());
      ImmutableSet<ClassFile> p = originalClasses.provides(next);
      if (p.isEmpty()) {
        repository.unresolveable(next);
        unresolved.add(next);
      } else {
        repository.addClassFiles(p);
      }
    }

    unresolved.forEach(next -> System.out.printf("Unresolved %s#%s%n", next.owner(), next.name()));
    return repository;
  }
}
