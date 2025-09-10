package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class UnexpectedRefreshTokenVerificationException(ex: Exception) :
    JwtException("An unexpected error occurred while verifying refresh token", ex)
