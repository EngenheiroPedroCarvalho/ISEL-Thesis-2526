/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

import io.ktor.util.date.*
import kotlin.time.Duration

data class DeploymentTimeData(
    var zipUploadTime: Duration = Duration.ZERO,
    var deploymentStartDate: GMTDate = GMTDate.START,
    var deploymentEndDate: GMTDate = GMTDate.START
)
