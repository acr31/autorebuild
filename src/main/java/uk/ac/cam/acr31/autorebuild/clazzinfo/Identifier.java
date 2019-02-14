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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

@AutoValue
public abstract class Identifier implements Comparable<Identifier> {

  private static final Comparator<Identifier> COMPARATOR =
      Comparator.comparing(Identifier::owner).thenComparing(Identifier::name);
  private static final Pattern INTERNAL_NAME = Pattern.compile("[/\\p{javaJavaIdentifierPart}]*");
  private static final Pattern PRIMITIVE_TYPE_DESCRIPTOR = Pattern.compile("^\\[*[ZCBSIFJDV].*");
  private static final Pattern PRIMITIVE_ARRAY_TYPE_DESCRIPTOR =
      Pattern.compile("^\\[+[ZCBSIFJDLV].*");

  private static final Pattern CLASS_TYPE_DESCRIPTOR =
      Pattern.compile("^\\[*L([/\\p{javaJavaIdentifierPart}]+);");

  private static final Pattern METHOD_TYPE_DESCRIPTOR = Pattern.compile("^\\((.*)\\)(.*)");

  public abstract String owner();

  public abstract String name();

  @Override
  public int compareTo(@Nonnull Identifier that) {
    return COMPARATOR.compare(this, that);
  }

  public static Identifier create(String owner, String name) {
    return new AutoValue_Identifier(convertName(owner), name);
  }

  static Identifier create(String owner) {
    return create(owner, "");
  }

  static ImmutableSet<Identifier> fromMethodDescriptor(String methodDescriptor) {
    ImmutableSet.Builder<Identifier> builder = ImmutableSet.builder();
    Matcher methodTypeMatcher = METHOD_TYPE_DESCRIPTOR.matcher(methodDescriptor);
    if (!methodTypeMatcher.matches()) {
      throw new IllegalArgumentException("Invalid method descriptor: " + methodDescriptor);
    }
    String argumentTypes = methodTypeMatcher.group(1);
    String returnType = methodTypeMatcher.group(2);

    if (isClassType(returnType)) {
      builder.add(create(returnType));
    }
    StringBuilder objectName = null;
    for (char c : argumentTypes.toCharArray()) {
      if (objectName == null) {
        switch (c) {
          case '[':
          case 'Z':
          case 'C':
          case 'B':
          case 'S':
          case 'I':
          case 'F':
          case 'J':
          case 'D':
            // ignore
            break;
          case 'L':
            objectName = new StringBuilder();
            break;
          default:
            throw new IllegalArgumentException("Failed to parse: " + argumentTypes);
        }
      } else {
        if (c == ';') {
          builder.add(create(objectName.toString(), ""));
          objectName = null;
        } else {
          objectName.append(c);
        }
      }
    }
    if (objectName != null) {
      throw new IllegalArgumentException("Failed to parse: " + argumentTypes);
    }
    return builder.build();
  }

  static boolean isClassType(String descriptor) {
    return !PRIMITIVE_TYPE_DESCRIPTOR.matcher(descriptor).matches();
  }

  static boolean isPrimitiveArrayType(String descriptor) {
    return PRIMITIVE_ARRAY_TYPE_DESCRIPTOR.matcher(descriptor).matches();
  }

  private static String convertName(String name) {
    Matcher classTypeMatcher = CLASS_TYPE_DESCRIPTOR.matcher(name);
    if (classTypeMatcher.matches()) {
      return classTypeMatcher.group(1);
    }
    if (INTERNAL_NAME.matcher(name).matches()) {
      return name;
    }
    throw new IllegalArgumentException("Unable to infer kind for: " + name);
  }
}
