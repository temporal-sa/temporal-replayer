package io.temporal.samples.replay.worker;

import io.temporal.samples.replay.Hello;
import io.temporal.samples.replay.util.Environment;
import io.temporal.samples.replay.util.TemporalClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class TemporalWorker {

    @SuppressWarnings("CatchAndPrintStackTrace")
    public static void main(String[] args) throws Exception {
        final String TASK_QUEUE = Environment.getTaskqueue();

        WorkerFactory factory = WorkerFactory.newInstance(TemporalClient.get());

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(Hello.HelloWorkflowImpl.class);
        worker.registerActivitiesImplementations(new Hello.HelloActivitiesImpl());

        factory.start();
        System.out.println("Worker started for task queue: " + TASK_QUEUE);
    }
}
