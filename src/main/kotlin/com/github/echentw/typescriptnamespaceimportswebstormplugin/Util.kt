import com.google.gson.GsonBuilder

sealed class Result<out OkT, out ErrT> {
    data class Ok<out OkT>(val value: OkT) : Result<OkT, Nothing>()
    data class Err<out ErrT>(val err: ErrT) : Result<Nothing, ErrT>()

    companion object {
        // Convenience factory methods
        fun <T> ok(value: T): Result<T, Nothing> = Ok(value)
        fun <E> err(error: E): Result<Nothing, E> = Err(error)
    }
}

fun resultExample(result: Result<String, Int>): Unit {
    when (result) {
        is Result.Err -> {
            println("Error: ${result.err}")
            return
        }
        is Result.Ok -> {}
    }

    // result is smart cast to Result.Ok<String> here
    val data = result.value
}

object Util {
    fun stringify(obj: Any): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(obj)
    }
}
