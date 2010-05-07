/**
 * Copyright 2009 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.client;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.hbase.io.RowResult;

/**
 * Interface for client-side scanning.
 * Go to {@link HTable} to obtain instances.
 * @deprecated See {@link ResultScanner}
 */
public interface Scanner extends Closeable, Iterable<RowResult> {
  /**
   * Grab the next row's worth of values.
   * @return RowResult object if there is another row, null if the scanner is
   * exhausted.
   * @throws IOException
   */
  public RowResult next() throws IOException;

  /**
   * @param nbRows number of rows to return
   * @return Between zero and <param>nbRows</param> Results
   * @throws IOException
   */
  public RowResult [] next(int nbRows) throws IOException;

  /**
   * Closes the scanner and releases any resources it has allocated
   */
  public void close();
}