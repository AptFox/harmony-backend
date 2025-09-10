package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class RefreshTokenNotInDBException(ex: RuntimeException) :
    JwtException("Refresh token not in DB.", ex)
