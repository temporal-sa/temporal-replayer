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

package io.temporal.samples.replay;

import static org.hamcrest.MatcherAssert.assertThat;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.internal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit test for replay {@link Hello.HelloWorkflowImpl}. Doesn't use an external Temporal
 * service.
 */
public class HelloTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setDoNotStart(true).build();

  @Test
  public void replayWorkflowExecution() throws Exception {

    final String eventHistory = executeWorkflow(Hello.HelloWorkflowImpl.class);

    WorkflowReplayer.replayWorkflowExecution(
        eventHistory, Hello.HelloWorkflowImpl.class);
  }

  @Test
  public void replayWorkflowExecutionNonDeterministic() {

    // We are executing the workflow with one implementation (GreetingWorkflowImplTest) and trying
    // to replay the even history with a different implementation (GreetingWorkflowImpl),
    // which causes an exception during the replay

    try {

      final String eventHistory = executeWorkflow(HelloWorkflowImplTest.class);

      WorkflowReplayer.replayWorkflowExecution(
          eventHistory, Hello.HelloWorkflowImpl.class);

      Assert.fail("Should have thrown an Exception");
    } catch (Exception e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.containsString("error=io.temporal.worker.NonDeterministicException"));
    }
  }

  private String executeWorkflow(
      Class<? extends Hello.HelloWorkflow> workflowImplementationType) {

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new Hello.HelloActivitiesImpl());

    testWorkflowRule.getWorker().registerWorkflowImplementationTypes(workflowImplementationType);

    testWorkflowRule.getTestEnvironment().start();

    Hello.HelloWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                Hello.HelloWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    WorkflowExecution execution = WorkflowStub.fromTyped(workflow).start("Hello");
    // wait until workflow completes
    WorkflowStub.fromTyped(workflow).getResult(String.class);

    return new WorkflowExecutionHistory(testWorkflowRule.getHistory(execution)).toJson(true);
  }

  public static class HelloWorkflowImplTest implements Hello.HelloWorkflow {

    private final Hello.HelloActivities activities =
        Workflow.newActivityStub(
            Hello.HelloActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String getGreeting(String name) {
      Workflow.sleep(100);
      return activities.composeGreeting("Hello", name);
    }
  }
}
