package costaber.com.github.omniflow.cloud.provider.amazon.service

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest

/**
 * Discovers which AWS regions are enabled for the calling account.
 *
 * AWS counterpart of the Cloud Run Locations API used on the GCP path
 * ([costaber.com.github.omniflow.cloud.provider.google.service.CloudRunLocationsV1RestClient]).
 * Declared as an interface so region-scanning logic can be exercised without reaching AWS.
 */
interface AwsRegionsLister {
    /**
     * @param bootstrapRegion the region whose EC2 endpoint is called to list regions. AWS has no
     * region-less/global endpoint for this, so some starting region is always required.
     */
    fun listRegions(bootstrapRegion: String = "us-east-1"): List<String>
}

/**
 * EC2-backed implementation.
 *
 * Uses `DescribeRegions` without `allRegions(true)`, which returns only the regions actually
 * enabled for the account - the closest AWS analog to Cloud Run's project-scoped locations list.
 */
class Ec2AwsRegionsLister(
    private val credentialsProvider: AwsCredentialsProvider = EnvironmentVariableCredentialsProvider.create()
) : AwsRegionsLister {

    override fun listRegions(bootstrapRegion: String): List<String> =
        Ec2Client.builder()
            .region(Region.of(bootstrapRegion))
            .credentialsProvider(credentialsProvider)
            .build()
            .use { client ->
                client.describeRegions(DescribeRegionsRequest.builder().build())
                    .regions()
                    .mapNotNull { it.regionName() }
                    .distinct()
            }
}
