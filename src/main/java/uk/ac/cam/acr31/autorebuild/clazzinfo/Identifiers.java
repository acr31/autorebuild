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

public class Identifiers {

  static String fromInternalName(String name) {
    return String.format("L%s;", name);
  }

  public static String fromMethod(String className, String name, String descriptor) {
    return String.format("%s#%s#%s", className, name, descriptor);
  }

  public static String fromField(String className, String name, String descriptor) {
    return String.format("%s#%s#%s", className, name, descriptor);
  }
}
