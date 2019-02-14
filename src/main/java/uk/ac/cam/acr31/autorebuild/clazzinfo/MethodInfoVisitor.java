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
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

class MethodInfoVisitor extends MethodVisitor {

  private final ClassFile.Builder classFile;

  MethodInfoVisitor(ClassFile.Builder classFile) {
    super(Opcodes.ASM7);
    this.classFile = classFile;
  }

  @Override
  public void visitInvokeDynamicInsn(
      String name,
      String descriptor,
      Handle bootstrapMethodHandle,
      Object... bootstrapMethodArguments) {
    Identifier.fromMethodDescriptor(descriptor).forEach(classFile::addReferenced);
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    if (Identifier.isClassType(descriptor)) {
      classFile.addReferenced(Identifier.create(descriptor));
    }
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
  }

  @Override
  public void visitLocalVariable(
      String name, String descriptor, String signature, Label start, Label end, int index) {
    if (Identifier.isClassType(descriptor)) {
      classFile.addReferenced(Identifier.create(descriptor));
    }
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(
      int parameter, String descriptor, boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    // this isn't ideal because we can't tell the difference between I (the internal name for a
    // class I with no package) and I (the type descriptor for an integer)
    if (Identifier.isClassType(type)) {
      classFile.addReferenced(Identifier.create(type));
    }
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    if (Identifier.isPrimitiveArrayType(owner)) {
      return;
    }
    classFile.addReferenced(Identifier.create(owner, name));
    if (Identifier.isClassType(descriptor)) {
      classFile.addReferenced(Identifier.create(descriptor));
    }
  }

  @Override
  public void visitMethodInsn(
      int opcode, String owner, String name, String descriptor, boolean isInterface) {
    if (Identifier.isPrimitiveArrayType(owner)) {
      return;
    }
    classFile.addReferenced(Identifier.create(owner, name + descriptor));
    Identifier.fromMethodDescriptor(descriptor).forEach(classFile::addReferenced);
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    if (type != null) {
      classFile.addReferenced(Identifier.create(type));
    }
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
      int typeRef,
      TypePath typePath,
      Label[] start,
      Label[] end,
      int[] index,
      String descriptor,
      boolean visible) {
    classFile.addReferenced(Identifier.create(descriptor));
    return new AnnotationInfoVisitor(classFile);
  }
}
