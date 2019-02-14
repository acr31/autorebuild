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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

class ClassInfoVisitor extends ClassVisitor {

  private ClassFile.Builder classFile;

  ClassInfoVisitor(ClassFile.Builder classFile) {
    super(Opcodes.ASM7);
    this.classFile = classFile;
  }

  @Override
  public void visitSource(String source, String debug) {
    classFile.setSourceFileName(source);
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    Identifier descriptor = Identifier.create(name);
    classFile.setDescriptor(descriptor.owner());
    classFile.addDeclared(descriptor);

    // if its an innerclass add a ref to the outerclass so we include it
    if (name.contains("$")) {
      Identifier outerDescriptor = Identifier.create(name.replaceAll("\\$.*", ""));
      classFile.addReferenced(outerDescriptor);
    }

    List<String> elements = Splitter.on("/").splitToList(name);
    classFile.setPackageName(Joiner.on(".").join(elements.subList(0, elements.size() - 1)));

    if (superName != null) { // objects have no super-class
      Identifier superIdentifier = Identifier.create(superName);
      classFile.addAncestor(superIdentifier.owner());
      classFile.addReferenced(superIdentifier);
    }

    Arrays.stream(interfaces)
        .map(Identifier::create)
        .forEach(
            i -> {
              classFile.addAncestor(i.owner());
              classFile.addReferenced(i);
            });
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
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    classFile.addReferenced(Identifier.create(name));
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    classFile.addDeclared(Identifier.create(classFile.descriptor(), name));
    return new FieldInfoVisitor(classFile);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    classFile.addDeclared(Identifier.create(classFile.descriptor(), name + descriptor));
    Identifier.fromMethodDescriptor(descriptor).forEach(classFile::addReferenced);
    return new MethodInfoVisitor(classFile);
  }
}
