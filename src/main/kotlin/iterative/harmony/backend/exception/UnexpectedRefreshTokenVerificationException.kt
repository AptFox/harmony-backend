package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class UnexpectedRefreshTokenVerificationException(ex: Exception) :
    JwtException("Unexpected refresh token verification error: ${ex.message}", ex)
