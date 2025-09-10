package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class RefreshTokenExpiredException() : JwtException("The supplied refresh token is expired")
