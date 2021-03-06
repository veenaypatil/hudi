/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.utils;

import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.common.util.FileIOUtils;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.util.StreamerUtil;

import org.apache.flink.configuration.Configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestStreamerUtil {

  @TempDir
  File tempFile;

  @Test
  public void testInitTableIfNotExists() throws IOException {
    Configuration conf = TestConfigurations.getDefaultConf(tempFile.getAbsolutePath());

    // Test for partitioned table.
    conf.setString(FlinkOptions.PRECOMBINE_FIELD, "ts");
    conf.setString(FlinkOptions.PARTITION_PATH_FIELD, "p0,p1");
    StreamerUtil.initTableIfNotExists(conf);

    // Validate the partition fields & preCombineField in hoodie.properties.
    HoodieTableMetaClient metaClient1 = HoodieTableMetaClient.builder()
            .setBasePath(tempFile.getAbsolutePath())
            .setConf(new org.apache.hadoop.conf.Configuration())
            .build();
    assertTrue(metaClient1.getTableConfig().getPartitionColumns().isPresent(),
            "Missing partition columns in the hoodie.properties.");
    assertArrayEquals(metaClient1.getTableConfig().getPartitionColumns().get(), new String[] { "p0", "p1" });
    assertEquals(metaClient1.getTableConfig().getPreCombineField(), "ts");

    // Test for non-partitioned table.
    conf.removeConfig(FlinkOptions.PARTITION_PATH_FIELD);
    FileIOUtils.deleteDirectory(tempFile);
    StreamerUtil.initTableIfNotExists(conf);
    HoodieTableMetaClient metaClient2 = HoodieTableMetaClient.builder()
            .setBasePath(tempFile.getAbsolutePath())
            .setConf(new org.apache.hadoop.conf.Configuration())
            .build();
    assertFalse(metaClient2.getTableConfig().getPartitionColumns().isPresent());
  }
}

