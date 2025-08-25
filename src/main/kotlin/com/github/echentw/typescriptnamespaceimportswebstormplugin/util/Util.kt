import com.google.gson.GsonBuilder
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

sealed class Result<out OkT, out ErrT> {
    data class Ok<out OkT>(val value: OkT) : Result<OkT, Nothing>()
    data class Err<out ErrT>(val err: ErrT) : Result<Nothing, ErrT>()

    companion object {
        // Convenience factory methods
        fun <T> ok(value: T): Result<T, Nothing> = Ok(value)
        fun <E> err(error: E): Result<Nothing, E> = Err(error)
    }
}

object Util {
    fun stringify(obj: Any): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(obj)
    }

    fun pathWithoutExtension(path: String): String {
        val path = Path(path)
        return path.parent?.resolve(path.nameWithoutExtension)?.normalize()?.toString()
            ?: path.nameWithoutExtension
    }

    fun playground() {
//        println("--begin playground--")
//        println(Path("/a/b/c.ts").relativize(Path("/a/d/e.ts")).toString())
//        println(Path("/a/b/c.ts").parent.toString())
//        println(Path("/a").parent.toString())
//        println("--end playground--")
    }
}
