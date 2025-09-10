package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class UnparseableTokenException(ex: IllegalArgumentException) :
    JwtException("The supplied token string is empty or unreadable", ex)
