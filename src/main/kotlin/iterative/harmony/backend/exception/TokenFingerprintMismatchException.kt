package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class TokenFingerprintMismatchException(
    val tokenFingerprint: String?,
    val requestFingerprint: String,
) : JwtException("Request fingerprint and token fingerprint do not match")
