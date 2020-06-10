/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.models.configurations;

import lombok.Data;

import java.io.Serializable;
import java.net.URI;

/**
 * Repo configuration model
 *
 * @author moses AT doraventures DOT com
 */
@Data
public class Repo implements Serializable {

  private static final long serialVersionUID = 5460376495600337528L;

  URI url;
  String name;
  String handler;
}
