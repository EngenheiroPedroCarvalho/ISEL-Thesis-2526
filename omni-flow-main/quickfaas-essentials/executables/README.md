# JAR executables

In order to run QuickFaaS JAR files, you will first need to install JDK 11 (Java Development Kit) or later, which already includes a JRE (Java Runtime Environment) version.

## QuickFaaS-Auth-1.0-fat.jar
Run the JAR file on the command line or terminal window:
```sh
$ java -jar QuickFaaS-Auth-1.0-fat.jar
```
You will be prompt with several options of cloud providers to choose from. The application will then try to redirect you to the provider's authentication web page using your default browser. The authentication process relies on the OAuth 2.0 protocol. The application will never have access to your credentials. You will, however, be requested to grant access to a certain number of scopes that are required for the application to work as expected.

After a successful authentication, an access token will be provided. You will be asked for this token to be inserted in the `func-deployment.json` file to enable FaaS deployments.

In order to receive tokens sent by providers, the application launches an HTTP server that starts listening for requests to a callback API locally, on the `8080` port.

## QuickFaaS-Deployment-1.0-fat.jar
Before running the JAR file, make sure you have your cloud-agnostic function file ready, together with the `func-deployment.json` file configured. You can find templates for both of these files in the _[templates](https://github.com/Pexers/quickfaas-essentials/tree/main/templates)_ root directory.

The `./function-deployment` directory as well as the `func-deployment.json` file should be on the same directory as the JAR file.  
Run the JAR file on the command line or terminal window:
```sh
$ java -jar QuickFaaS-Deployment-1.0-fat.jar
```
> **Note**
> Google Cloud Platform deployments require the [_Cloud Functions API_](https://cloud.google.com/functions/docs/reference/rest) and the [_Cloud Resource Manager API_](https://cloud.google.com/resource-manager/reference/rest) enabled.

The `func-deployment.json` file can be configured with the following values:
| Property name | Type | Values & Description |
| --- | :---: | --- |
| **cloudProvider** | _string_ | `gcp`, `msazure`<br/>The chosen cloud provider for the FaaS deployment. |
| **accessToken** | _string_ | The OAuth 2.0 access token provided by `QuickFaaS-Auth-1.0-fat.jar` after a successful authentication. |
| **subscriptionId**<br/>[MsAzure exclusive] | _string_ | MsAzure active subscription ID. |
| **project** | _string_ | The project responsible for holding deployed resources.<br/>GCP -> _Project_ name<br/>MsAzure -> _Resource Group_ name |
| **function** | _object_ | The function's resource configuration. |
| **function.name** | _string_ | The FaaS resource name. |
| **function.location** | _string_ | The location where the resource resides, preferably as close as possible to the end user. |
| **function.bucket** | _string_ | GCP -> _Bucket_ name<br/>MsAzure -> _Storage Account_ name |
| **function.runtime** | _string_ | `java11`<br/>The function's runtime. |
| **function.trigger** | _object_ | Function's event trigger configuration. |
| **function.trigger.type** | _string_ | `http`, `storage`<br/>Function's event trigger type. |
| **function.trigger.bucket**<br/>[storage trigger exclusive] | _string_ | The storage bucket to detect changes.<br/>GCP -> _Bucket_ name<br/>MsAzure -> _Storage Account_ name |
| **function.trigger.eventType**<br/>[storage trigger exclusive] | _string_ | `Create`, `Delete`, `Update`<br/>The storage event type to trigger execution. Only `Create` is supported in MsAzure for now. |
| **functionFile** | _string_ | Path to the cloud-agnostic function definition file. |
| **dependenciesFile**<br/>[optional] | _string_ | Path to the function's extra dependencies file to be installed before deployment. |
| **configurationsFile**<br/>[optional] | _string_ | Path to the configurations JSON file. The configurations file allows users to specify JSON properties that can be accessed during function's execution time. |

### Java functions
QuickFaaS uses Apache Maven to build Java projects before deployment. For now, the `./function-deployment` directory already comes with a Maven version, so that you don't need to install it separately. However, this may change in future releases.
