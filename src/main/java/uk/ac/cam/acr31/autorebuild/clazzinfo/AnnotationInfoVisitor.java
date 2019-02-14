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

class AnnotationInfoVisitor extends AnnotationVisitor {

  private final ClassFile.Builder classFile;

  AnnotationInfoVisitor(ClassFile.Builder classFile) {
    super(Opcodes.ASM7);
    this.classFile = classFile;
  }

  @Override
  public void visitEnum(String name, String descriptor, String value) {
    classFile.addReferenced(Identifier.create(descriptor));
  }

  @Override
  public AnnotationVisitor visitAnnotation(String name, String descriptor) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public AnnotationVisitor visitArray(String name) {
    return new AnnotationInfoVisitor(classFile);
  }
}
