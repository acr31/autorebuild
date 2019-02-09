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

import java.util.Arrays;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class ClassInfoVisitor extends ClassVisitor {

  private Summary.Builder summary;

  public ClassInfoVisitor(Summary.Builder summary) {
    super(Opcodes.ASM7);
    this.summary = summary;
  }

  @Override
  public void visitSource(String source, String debug) {
    summary.setSourceFile(source);
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    summary.setClassName(Identifiers.fromInternalName(name));
    summary.addDeclared(Identifiers.fromInternalName(name));
    summary.addReferenced(Identifiers.fromInternalName(superName));
    Arrays.stream(interfaces).map(Identifiers::fromInternalName).forEach(summary::addReferenced);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    return new AnnotationInfoVisitor(summary);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    return new AnnotationInfoVisitor(summary);
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    summary.addDeclared(Identifiers.fromInternalName(name));
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    summary.addDeclared(Identifiers.fromField(summary.className(), name, descriptor));
    return new FieldInfoVisitor(summary);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    summary.addDeclared(Identifiers.fromMethod(summary.className(), name, descriptor));
    return new MethodInfoVisitor(summary);
  }
}
