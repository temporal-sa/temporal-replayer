/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.replay.util;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import javax.net.ssl.SSLException;

public class TemporalClient {
  public static WorkflowServiceStubs getWorkflowServiceStubs()
          throws FileNotFoundException, SSLException {
    WorkflowServiceStubsOptions.Builder workflowServiceStubsOptionsBuilder =
            WorkflowServiceStubsOptions.newBuilder();

    if (!Environment.getCertPath().equals("") && !"".equals(Environment.getKeyPath())) {
      InputStream clientCert = new FileInputStream(Environment.getCertPath());

      InputStream clientKey = new FileInputStream(Environment.getKeyPath());

      workflowServiceStubsOptionsBuilder.setSslContext(
              SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build());
    }

    // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
    String targetEndpoint = Environment.getAddress();
    // Your registered namespace.

    workflowServiceStubsOptionsBuilder.setTarget(targetEndpoint);
    WorkflowServiceStubs service = null;

    if (!Environment.getAddress().equals("localhost:7233")) {
      // if not local server, then use the workflowServiceStubsOptionsBuilder
      service = WorkflowServiceStubs.newServiceStubs(workflowServiceStubsOptionsBuilder.build());
    } else {
      service = WorkflowServiceStubs.newLocalServiceStubs();
    }

    return service;
  }

  public static WorkflowClient get() throws FileNotFoundException, SSLException {
    // TODO support local server
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowServiceStubs service = getWorkflowServiceStubs();

    WorkflowClientOptions.Builder builder = WorkflowClientOptions.newBuilder();

    // if environment variable ENCRYPT_PAYLOADS is set to true, then use CryptCodec
    if (System.getenv("ENCRYPT_PAYLOADS") != null
            && System.getenv("ENCRYPT_PAYLOADS").equals("true")) {
      builder.setDataConverter(
              new CodecDataConverter(
                      DefaultDataConverter.newDefaultInstance(),
                      Collections.singletonList(new CryptCodec()),
                      true /* encode failure attributes */));
    }

    System.out.println("<<<<SERVER INFO>>>>:\n " + Environment.getServerInfo());
    WorkflowClientOptions clientOptions = builder.setNamespace(Environment.getNamespace()).build();

    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
    return client;
  }

  public static ScheduleClient getScheduleClient() throws FileNotFoundException, SSLException {
    // TODO support local server
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowServiceStubs service = getWorkflowServiceStubs();

    ScheduleClientOptions.Builder builder = ScheduleClientOptions.newBuilder();

    // if environment variable ENCRYPT_PAYLOADS is set to true, then use CryptCodec
    if (System.getenv("ENCRYPT_PAYLOADS") != null
            && System.getenv("ENCRYPT_PAYLOADS").equals("true")) {
      builder.setDataConverter(
              new CodecDataConverter(
                      DefaultDataConverter.newDefaultInstance(),
                      Collections.singletonList(new CryptCodec()),
                      true /* encode failure attributes */));
    }

    System.out.println("<<<<SERVER INFO>>>>:\n " + Environment.getServerInfo());
    ScheduleClientOptions clientOptions = builder.setNamespace(Environment.getNamespace()).build();

    // client that can be used to start and signal workflows
    ScheduleClient client = ScheduleClient.newInstance(service, clientOptions);
    return client;
  }
}
