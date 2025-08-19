sealed class Result<out OkT, out ErrT> {
    data class Ok<out OkT>(val value: OkT) : Result<OkT, Nothing>()
    data class Err<out ErrT>(val error: ErrT) : Result<Nothing, ErrT>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    // Get value if Ok, null otherwise
    fun getOrNull(): OkT? = when (this) {
        is Ok -> value
        is Err -> null
    }

    // Get error if Err, null otherwise
    fun getErrorOrNull(): ErrT? = when (this) {
        is Ok -> null
        is Err -> error
    }

    // Get value if Ok, or return default
    fun getOrDefault(default: OkT): OkT = when (this) {
        is Ok -> value
        is Err -> default
    }

    // Get value if Ok, or compute default from error
    inline fun getOrElse(onError: (ErrT) -> OkT): OkT = when (this) {
        is Ok -> value
        is Err -> onError(error)
    }

    // Transform the Ok value
    inline fun <R> map(transform: (OkT) -> R): Result<R, ErrT> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(error)
    }

    // Transform the Err value
    inline fun <R> mapError(transform: (ErrT) -> R): Result<OkT, R> = when (this) {
        is Ok -> Ok(value)
        is Err -> Err(transform(error))
    }

    // Flat map for chaining operations that return Results
    inline fun <R> flatMap(transform: (OkT) -> Result<R, ErrT>): Result<R, ErrT> = when (this) {
        is Ok -> transform(value)
        is Err -> Err(error)
    }

    // Execute side effects based on result type
    inline fun onSuccess(action: (OkT) -> Unit): Result<OkT, ErrT> = apply {
        if (this is Ok) action(value)
    }

    inline fun onError(action: (ErrT) -> Unit): Result<OkT, ErrT> = apply {
        if (this is Err) action(error)
    }

    // Fold both cases into a single result
    inline fun <R> fold(
        onSuccess: (OkT) -> R,
        onError: (ErrT) -> R
    ): R = when (this) {
        is Ok -> onSuccess(value)
        is Err -> onError(error)
    }

    companion object {
        // Convenience factory methods
        fun <T> success(value: T): Result<T, Nothing> = Ok(value)
        fun <E> error(error: E): Result<Nothing, E> = Err(error)

        // Catch exceptions and convert to Result
        inline fun <T> catching(action: () -> T): Result<T, Exception> = try {
            Ok(action())
        } catch (e: Exception) {
            Err(e)
        }
    }
}

fun f(result: Result<String, Int>): Void {
    when (result) {
        is Result.Ok -> {
            // result is smart cast to Result.Success<String>
            println("Got data: ${result.value}")
        }
        is Result.Err -> {
            // result is smart cast to Result.Error
            println("Error: ${result.error}")
        }
    }
}
