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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class AnnotationInfoVisitor extends AnnotationVisitor {

  private final Summary.Builder summary;

  AnnotationInfoVisitor(Summary.Builder summary) {
    super(Opcodes.ASM7);
    this.summary = summary;
  }

  @Override
  public void visitEnum(String name, String descriptor, String value) {
    summary.addReferenced(Identifiers.fromInternalName(descriptor));
  }

  @Override
  public AnnotationVisitor visitAnnotation(String name, String descriptor) {
    return new AnnotationInfoVisitor(summary);
  }

  @Override
  public AnnotationVisitor visitArray(String name) {
    return new AnnotationInfoVisitor(summary);
  }
}
