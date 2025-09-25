package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class UnparseableTokenException(ex: Exception) :
    JwtException("Unable to parse token: ${ex.message}", ex)
