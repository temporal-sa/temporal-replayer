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

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.replay.util.Environment;
import io.temporal.samples.replay.util.TemporalClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.Duration;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample Temporal Workflow Definition that executes a single Activity. */
public class Hello {
  private static final Logger log = LoggerFactory.getLogger(HelloActivitiesImpl.class);

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface HelloWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreetings(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface HelloActivities {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod(name = "GreetingOne")
    String composeGreetingOne(String greeting, String name);

    @ActivityMethod(name = "GreetingTwo")
    String composeGreetingTwo(String greeting, String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class HelloWorkflowImpl implements HelloWorkflow {

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * overall timeout that our workflow is willing to wait for activity to complete. For this
     * example it is set to 2 seconds.
     */
    private final HelloActivities activities =
        Workflow.newActivityStub(
            HelloActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String getGreetings(String name) {
      // This is a blocking call that returns only after the activity has completed.
      String greetingOne = activities.composeGreetingOne("Hello", name);
      String greetingTwo = activities.composeGreetingTwo("Guten Tag", name);
      return greetingOne + " " + greetingTwo;
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  public static class HelloActivitiesImpl implements HelloActivities {

    @Override
    public String composeGreetingOne(String greeting, String name) {
      log.info("Composing greeting...");
      return greeting + " " + name + "!";
    }

    @Override
    public String composeGreetingTwo(String greeting, String name) {
      log.info("Composing greeting...");
      return greeting + " " + name + "!";
    }    
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */

  public static void main(String[] args) throws Exception{

    // Get a Workflow service stub.

    WorkflowServiceStubs service = TemporalClient.getWorkflowServiceStubs();

    WorkflowClientOptions.Builder builder = WorkflowClientOptions.newBuilder();
    WorkflowClientOptions clientOptions = builder.setNamespace(Environment.getNamespace()).build();
    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */

    WorkerFactory factory = WorkerFactory.newInstance(TemporalClient.get());

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(Environment.getTaskqueue());

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(HelloWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new HelloActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();
    log.info("Started worker...");
    // Create the workflow client stub. It is used to start our workflow execution.
    HelloWorkflow workflow =
        client.newWorkflowStub(
            HelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(Environment.getTaskqueue())
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */

    String greeting = workflow.getGreetings("World");

    // Display workflow execution results
    System.out.println(greeting);
    System.exit(0);
  }
}
