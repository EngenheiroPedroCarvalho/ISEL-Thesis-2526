// AWS checks for aws-region-ref-test.sh, run with Java's single-file source launcher on
// OmniFlow's classpath (the AWS CLI is not needed). Credentials come from the environment, as
// in OmniFlow itself.
//
//   absent  <region> <function>       exit 0 if the Lambda does not exist, 3 if it does
//   execute <region> <state machine>  start an execution with input {} and wait for it;
//                                     exit 0 if it SUCCEEDED

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionResponse;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StateMachineListItem;

public class AwsRegionRefCheck {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("usage: absent <region> <function> | execute <region> <state machine>");
            System.exit(2);
        }
        Region region = Region.of(args[1]);
        switch (args[0]) {
            case "absent" -> System.exit(absent(region, args[2]));
            case "execute" -> System.exit(execute(region, args[2]));
            default -> {
                System.err.println("unknown command: " + args[0]);
                System.exit(2);
            }
        }
    }

    private static int absent(Region region, String function) {
        try (LambdaClient lambda = LambdaClient.builder().region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build()) {
            String arn = lambda.getFunction(GetFunctionRequest.builder().functionName(function).build())
                    .configuration().functionArn();
            System.out.println("exists: " + arn);
            return 3;
        } catch (ResourceNotFoundException e) {
            System.out.println("absent: " + function + " in " + region);
            return 0;
        }
    }

    private static int execute(Region region, String stateMachineName) throws InterruptedException {
        try (SfnClient sfn = SfnClient.builder().region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build()) {
            String arn = sfn.listStateMachinesPaginator().stateMachines().stream()
                    .filter(m -> m.name().equals(stateMachineName))
                    .map(StateMachineListItem::stateMachineArn)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No state machine named '" + stateMachineName + "' in " + region));
            String executionArn = sfn.startExecution(StartExecutionRequest.builder()
                    .stateMachineArn(arn).input("{}").build()).executionArn();
            System.out.println("started: " + executionArn);

            long deadline = System.currentTimeMillis() + 120_000;
            DescribeExecutionResponse execution;
            do {
                Thread.sleep(2_000);
                execution = sfn.describeExecution(
                        DescribeExecutionRequest.builder().executionArn(executionArn).build());
            } while (execution.status() == ExecutionStatus.RUNNING && System.currentTimeMillis() < deadline);

            System.out.println("status: " + execution.status());
            if (execution.output() != null) System.out.println("output: " + execution.output());
            if (execution.error() != null) System.out.println("error: " + execution.error() + " " + execution.cause());
            return execution.status() == ExecutionStatus.SUCCEEDED ? 0 : 1;
        }
    }
}
