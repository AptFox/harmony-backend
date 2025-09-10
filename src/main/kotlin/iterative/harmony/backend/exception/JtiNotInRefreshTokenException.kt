package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class JtiNotInRefreshTokenException(ex: NullPointerException) :
    JwtException("JTI is missing from Refresh token", ex)
