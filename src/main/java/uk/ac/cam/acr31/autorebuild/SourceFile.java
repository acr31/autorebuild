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

import com.google.auto.value.AutoValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class SourceFile {

  private static Pattern PACKAGE_NAME =
      Pattern.compile(
          "package "
              + "((?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+"
              + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*);");

  public abstract String directory();

  public abstract String fileName();

  abstract String packageName();

  private static SourceFile create(String directory, String fileName, String packageName) {
    return new AutoValue_SourceFile(directory, fileName, packageName);
  }

  public static SourceFile create(String directory, String fileName, BufferedReader r)
      throws IOException {
    return create(directory, fileName, findPackageDeclaration(r).orElseThrow());
  }

  static SourceFile create(Path path) throws IOException {
    try (BufferedReader r = Files.newBufferedReader(path)) {
      return create(
          path.getParent().toString(),
          path.getFileName().toString(),
          findPackageDeclaration(r).orElseThrow());
    }
  }

  private static Optional<String> findPackageDeclaration(BufferedReader br) throws IOException {
    String line;
    while ((line = br.readLine()) != null) {
      Matcher matcher = PACKAGE_NAME.matcher(line);
      if (matcher.matches()) {
        return Optional.of(matcher.group(1));
      }
    }
    return Optional.empty();
  }
}
