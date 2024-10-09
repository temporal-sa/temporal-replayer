package io.temporal.samples.replay.worker;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.samples.replay.Hello;
import io.temporal.samples.replay.replayer.WorkflowReplayer;
import io.temporal.samples.replay.util.Environment;
import io.temporal.samples.replay.util.TemporalClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class TemporalWorkerSelfReplay {

    @SuppressWarnings("CatchAndPrintStackTrace")
    public static void main(String[] args) throws Exception {
        final String TASK_QUEUE = Environment.getTaskqueue();

        String workflowId = Environment.getWorkflowId();
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(TemporalClient.getWorkflowServiceStubs().getOptions());
        WorkflowClientOptions.Builder builder = WorkflowClientOptions.newBuilder();
        WorkflowClientOptions clientOptions = builder.setNamespace(Environment.getNamespace()).build();
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

        WorkflowReplayer workflowReplayer = new WorkflowReplayer();

        WorkflowExecutionHistory history = workflowReplayer.getWorkflowHistory(workflowId, client);

        WorkerFactory factory = WorkerFactory.newInstance(TemporalClient.get());

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(Hello.HelloWorkflowImpl.class);
        worker.registerActivitiesImplementations(new Hello.HelloActivitiesImpl());

        worker.replayWorkflowExecution(history);
        System.out.println("Workflow replay for workflowId: " + workflowId + " completed successfully");

        factory.start();
        System.out.println("Worker started for task queue: " + TASK_QUEUE);
    }
}
