# Temporal Replayer
The temporal-replayer is a sample that shows you how to safely attempt a rollout of new workflow code and ensure it doesn't cause non-determinism errors for existing in-flight workflows. Success with temporal-replayer means you can proceed with replacing your old workers with your new workers (running your new workflow code) with peace of mind that your in-progress and future workflows will complete.

## How it works?
Perform a Temporal Workflow Replay as part of k8s deployment.

An init container is used to run Workflow Replay. The event history is pulled from prior execution with previous release, against new release and code updates. It ensures that any new code changes to Workflow, don't break Workflow determinism. 

If the Workflow Replay fails, the init container will error and the deployment of the new worker won't occur.

If Workflow Replay succeeds, the deployment of the new worker code will occur.

## Environment Settings
```bash
TEMPORAL_WORKFLOW_ID=HelloWorkflow
TEMPORAL_ADDRESS=<Namespace>.<AccountId>.tmprl.cloud:7233
TEMPORAL_TASK_QUEUE=hello
TEMPORAL_NAMESPACE=<Namespace>.<AccountId>
TEMPORAL_CERT_PATH=/etc/certs/tls.crt
TEMPORAL_KEY_PATH=/etc/certs/tls.key
```

## Step 1: Run a HelloWorkflow (using k8s job)
This will create a HelloWorkflow with v1 code. This is just so you have existing Workflow event history. Notice the event history activity order (GreetingOne, GreetingTwo).

```bash
$ kubectl create -f yaml/job.yaml -n <K8s Namespace>
```

![Workflow](static/workflow.png)

![Event History](static/event_history.png)

## Step 2: Deploy v1
Deploying [v1](https://github.com/temporal-sa/temporal-replayer/blob/v1/src/main/java/io/temporal/samples/replay/Hello.java#L113) will deploy v1 code which has workflow activity order (GreetingOne, GreetingTwo). Replay will be performed using event history generated from step 1.

```bash
$ kubectl create -f yaml/deployment-v1.yaml -n <K8s Namespace>
```

### Workflow Replay Succeeds
Replay will succeed since the workflow code path v1 follows the recorded event history in step 1.

```bash
kubectl get pod -n temporal-workflow-replayer
NAME                                    READY   STATUS        RESTARTS   AGE
temporal-hello-worker-6dbb76577-j8lq8   0/1     Init:0/1      0          2s
```

```bash
NAME                                    READY   STATUS            RESTARTS   AGE
temporal-hello-worker-6dbb76577-j8lq8   0/1     PodInitializing   0          73s
```

```bash
NAME                                    READY   STATUS    RESTARTS   AGE
temporal-hello-worker-6dbb76577-j8lq8   1/1     Running   0          74s
```

## Step 3: Deploy v2
Deploying [v2](https://github.com/temporal-sa/temporal-replayer/blob/v2/src/main/java/io/temporal/samples/replay/Hello.java#L113) will deploy v2 code which has workflow activity order (GreetingTwo, GreetingOne). Replay will be performed using event history generated from step 1.

```bash
$ kubectl create -f yaml/deployment-v1.yaml -n <K8s Namespace>
```

### Workflow Replay Fails
Replay will fail since the event history from step 1 no longer follows the workflow code path. Inspecting the init container logs will show the replay failure.

```bash
kubectl get pod -n temporal-workflow-replayer
NAME                                    READY   STATUS        RESTARTS   AGE
temporal-hello-worker-59d6b8c8f-f7vp8   0/1     Init:0/1      0          3s
```

```bash
NAME                                    READY   STATUS       RESTARTS   AGE
temporal-hello-worker-59d6b8c8f-f7vp8   0/1     Init:Error   0          74s
```

```bash
kubectl logs -f temporal-hello-worker-59d6b8c8f-f7vp8 -c temporal-replayer -n temporal-workflow-replayer

...
17:23:38.070 { } [main] ERROR i.t.s.r.replayer.WorkflowReplayer - Failed to replay workflow HelloWorkflow

FAILURE: Build failed with an exception.
...
```


