package aki.tr.api.model

/**
 * Exception wrapping a typed [ApiError] for propagation through `Result` chains.
 *
 * @param apiError The categorised error that occurred.
 * @param message Human-readable error description.
 */
class ApiException(val apiError: ApiError, message: String) : Exception(message)
