package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class RefreshTokenFieldMismatchException(mismatchedFields: MutableList<String>) :
    JwtException(
        "The supplied refresh token does not match the DB. Mismatched fields: $mismatchedFields"
    )
