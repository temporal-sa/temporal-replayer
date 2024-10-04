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

package io.temporal.samples.replay.replayer;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.samples.replay.Hello;
import io.temporal.samples.replay.util.Environment;
import io.temporal.samples.replay.util.TemporalClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowReplayer {
  private static final Logger log = LoggerFactory.getLogger(WorkflowReplayer.class);

  public void replayWorkflowHistory(String workflowId, WorkflowClient workflowClient) throws Exception {
    WorkflowExecutionHistory history = getWorkflowHistory(workflowId, workflowClient);
    log.info("Replaying workflow id " + workflowId);
    log.info(Environment.getServerInfo().toString());

    try {
      io.temporal.testing.WorkflowReplayer.replayWorkflowExecution(history, Hello.HelloWorkflowImpl.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to replay workflow " + workflowId, e);
    }
  }

  private WorkflowExecutionHistory getWorkflowHistory(String workflowId, WorkflowClient workflowClient) {
    return workflowClient.fetchHistory(workflowId);
  }

  public static void main(String[] args) throws Exception{
    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(TemporalClient.getWorkflowServiceStubs().getOptions());

    WorkflowClientOptions.Builder builder = WorkflowClientOptions.newBuilder();
    WorkflowClientOptions clientOptions = builder.setNamespace(Environment.getNamespace()).build();
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);


    WorkflowReplayer workflowReplayer = new WorkflowReplayer();
    String workflowId = Environment.getWorkflowId();

    try {
      workflowReplayer.replayWorkflowHistory(workflowId, client);
      log.info("Replay test successful");
      System.exit(0);
    } catch (Exception e) {
      throw new RuntimeException("Failed to replay workflowId " + workflowId + " " + e);
    } finally {
      System.exit(1);
    }
  }
}
