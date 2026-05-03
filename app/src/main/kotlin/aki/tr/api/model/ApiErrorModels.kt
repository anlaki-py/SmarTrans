package aki.tr.api.model

/**
 * Typed API errors returned by OpenAI-compatible providers.
 * Each variant maps to a distinct user-facing scenario.
 */
sealed interface ApiError {

    /**
     * Provider rate-limited the request.
     *
     * @param message User-facing description.
     * @param retryAfterSeconds Optional `Retry-After` header value.
     */
    data class RateLimit(val message: String, val retryAfterSeconds: Int? = null) : ApiError

    /**
     * Authentication failure (401/403).
     *
     * @param message User-facing description.
     */
    data class InvalidKey(val message: String) : ApiError

    /**
     * Network-level failure (timeout, DNS, connection refused).
     *
     * @param message User-facing description.
     */
    data class Network(val message: String) : ApiError

    /**
     * Server-side error (5xx).
     *
     * @param message User-facing description.
     */
    data class ServerError(val message: String) : ApiError

    /**
     * Uncategorised error.
     *
     * @param message User-facing description.
     */
    data class Other(val message: String) : ApiError
}
