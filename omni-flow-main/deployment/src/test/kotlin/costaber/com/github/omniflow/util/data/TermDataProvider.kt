package costaber.com.github.omniflow.util.data

import costaber.com.github.omniflow.util.*
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

internal object TermDataProvider {

    @JvmStatic
    fun termResolveDataProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            VARIABLE_1,
            VARIABLE_NAME_1,
        ),
        Arguments.of(
            VALUE_1,
            "\'$VALUE_OF_1\'",
        ),
        Arguments.of(
            VALUE_2,
            VALUE_OF_2.toString(),
        )
    )
}